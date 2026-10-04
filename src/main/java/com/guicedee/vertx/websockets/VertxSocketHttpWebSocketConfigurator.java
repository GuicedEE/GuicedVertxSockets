package com.guicedee.vertx.websockets;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScoper;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.client.services.websocket.IGuicedWebSocket;
import com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
import com.guicedee.vertx.web.spi.VertxHttpServerOptionsConfigurator;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.guicedee.client.scopes.CallScopeSource.WebSocket;
import static com.guicedee.client.services.websocket.IGuicedWebSocket.EveryoneGroup;

/**
 * Configures Vert.x HTTP server WebSocket handling for GuicedEE.
 *
 * <p>Registers the WebSocket handler, initializes group event-bus consumers,
 * and applies {@link WebSocketServerOptions} to the server options.</p>
 *
 * @see WebSocketServerOptions
 * @see GuicedWebSocket
 */
@Log4j2
@Singleton
public class VertxSocketHttpWebSocketConfigurator implements IGuicePostStartup<VertxSocketHttpWebSocketConfigurator>,
        VertxHttpServerConfigurator, VertxRouterConfigurator<VertxSocketHttpWebSocketConfigurator>,
        VertxHttpServerOptionsConfigurator {
    @Inject
    Vertx vertx;

    @Inject
    CallScoper callScoper;

    @Inject
    WebSocketServerOptions webSocketServerOptions;

    /**
     * Provides the configurator ordering value used during startup.
     *
     * @return sort order for this configurator
     */
    public Integer sortOrder() {
        return 55;
    }

    /**
     * EventBus consumers keyed by group name.
     */
    public static final Map<String, List<MessageConsumer<String>>> groupConsumers = new ConcurrentHashMap<>();
    /**
     * Active WebSocket connections keyed by group name.
     */
    public static final Map<String, List<ServerWebSocket>> groupSockets = new ConcurrentHashMap<>();
    /**
     * Call-scope properties keyed by connection/group identifier.
     */
    public static final Map<String, CallScopeProperties> groupCallScopeProperties = new ConcurrentHashMap<>();

    /**
     * No-op post-load hook for IGuicePostStartup.
     *
     * @return an empty list (no async work required)
     */
    @Override
    public List<Uni<Boolean>> postLoad() {
        return List.of();
    }

    /**
     * Adds a WebSocket connection to a group and ensures the group listener exists.
     *
     * @param group     the group name
     * @param webSocket the WebSocket connection
     */
    public static void addToGroup(String group, ServerWebSocket webSocket) {
        configureGroupListener(IGuiceContext.get(Vertx.class), group, webSocket);
    }

    /**
     * Removes a WebSocket connection from a group and cleans up empty groups.
     *
     * @param group     the group name
     * @param webSocket the WebSocket connection
     */
    public static synchronized void removeFromGroup(String group, ServerWebSocket webSocket) {
        var sockets = groupSockets.get(group);
        if (sockets == null) return;
        sockets.remove(webSocket);
        if (sockets.isEmpty()) {
            groupSockets.remove(group);
            var consumers = groupConsumers.remove(group);
            if (consumers != null) consumers.forEach(MessageConsumer::unregister);
        }
    }

    private static final Map<ServerWebSocket, String> socketContextIds = new ConcurrentHashMap<>();

    public static synchronized void removeSocket(ServerWebSocket socket) {
        for (String group : List.copyOf(groupSockets.keySet())) removeFromGroup(group, socket);
        String handler = socketContextIds.remove(socket);
        if (handler == null) handler = socket.textHandlerID();
        if (handler != null) groupCallScopeProperties.remove(handler);
    }

    /**
     * Configures the HTTP server to accept WebSocket connections.
     *
     * <p>Establishes call-scoped context, initializes per-connection groups,
     * and routes inbound messages to {@link GuicedWebSocket}.</p>
     *
     * @param builder the HttpServer builder
     * @return the configured builder
     */
    @Override
    public HttpServer builder(HttpServer builder) {
        builder.webSocketHandler((ctx) -> {
            if (vertx == null) {
                IGuiceContext
                        .instance()
                        .inject()
                        .injectMembers(this)
                ;
            }
            callScoper.enter();
            try {
                callScoper.seed(Key.get(ServerWebSocket.class), ctx);
                CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
                String handlerId = ctx.textHandlerID();
                String id = handlerId == null ? java.util.UUID.randomUUID().toString() : handlerId;
                socketContextIds.put(ctx, id);
                ctx.setWriteQueueMaxSize(65536);
                properties.setSource(WebSocket);
                properties
                        .getProperties()
                        .put("RequestContextId", id);

                configureGroupListener(vertx, EveryoneGroup, ctx);

                //create my group id on connect
                groupConsumers.put(id, new ArrayList<>());
                groupCallScopeProperties.put(id, properties);

                configureGroupListener(vertx, id, ctx);

                //what happens on a message received
                ctx
                        .textMessageHandler((msg) -> {
                            processMessageInContext(ctx, msg, properties)
                                    .subscribe()
                                    .with(
                                            v -> {
                                            },
                                            e -> log.error("WebSocket message processing failed", e)
                                    )
                            ;
                        })
                        .exceptionHandler((e) -> {
                            log.error("Exception on web handler", e);
                            removeSocket(ctx);
                        })
                        .closeHandler((__) -> removeSocket(ctx))
                ;

                log.debug("Client connected: " + ctx.remoteAddress() + " / " + id);
                //add to default groups, everyone and me
            } finally {
                callScoper.exit();
            }
        });
        return builder;
    }

    /**
     * Ensures a group listener and registers the provided WebSocket.
     *
     * @param vertx     the Vertx instance used for EventBus consumers
     * @param group     the group name
     * @param webSocket the WebSocket connection
     */
    public static synchronized void configureGroupListener(Vertx vertx, String group, ServerWebSocket webSocket) {
        if (group == null || group.length() > 256) throw new IllegalArgumentException("Invalid WebSocket group");
        if (!groupSockets.containsKey(group) && groupSockets.size() >= 4096)
            throw new IllegalStateException("WebSocket group capacity reached");
        if (!groupConsumers.containsKey(group) || groupConsumers
                .get(group)
                .isEmpty()) {
            groupConsumers.put(group, new CopyOnWriteArrayList<>());
            groupSockets.put(group, new CopyOnWriteArrayList<>());
            MessageConsumer<String> r = vertx
                    .eventBus()
                    .consumer(group, message -> {
                        List<ServerWebSocket> serverWebSockets = groupSockets.get(group);
                        if (serverWebSockets == null) return;
                        for (ServerWebSocket serverWebSocket : List.copyOf(serverWebSockets)) {
                            GuicedWebSocket.writeMessageToSocket(message.body(), serverWebSocket);
                            //serverWebSocket.writeTextMessage((String) message.body());
                        }
                    });
            groupConsumers
                    .get(group)
                    .add(r);
        }
        if (!groupSockets.containsKey(group)) {
            groupSockets.put(group, new CopyOnWriteArrayList<>());
        }
        var sockets = groupSockets.get(group);
        if (sockets.size() >= 4096) throw new IllegalStateException("WebSocket recipient capacity reached");
        if (!sockets.contains(webSocket)) sockets.add(webSocket);
    }

    private io.smallrye.mutiny.Uni<Void> processMessageInContext(ServerWebSocket ctx, String msg, CallScopeProperties properties) {
        return io.smallrye.mutiny.Uni
                .createFrom()
                .deferred(() -> {
                    callScoper.enter();
                    try {
                        callScoper.seed(Key.get(ServerWebSocket.class), ctx);
                        CallScopeProperties props = IGuiceContext.get(CallScopeProperties.class);
                        props.setSource(properties.getSource());
                        properties
                                .getProperties()
                                .put("ServerWebSocket", ctx);
                        props
                                .getProperties()
                                .putAll(properties.getProperties());
                        GuicedWebSocket guicedWebSocket = (GuicedWebSocket) IGuiceContext.get(IGuicedWebSocket.class);
                        return guicedWebSocket
                                .receiveMessage(msg)
                                .eventually(() -> {
                                    callScoper.exit();
                                    return io.smallrye.mutiny.Uni.createFrom().voidItem();
                                });
                    } catch (Throwable t) {
                        callScoper.exit();
                        return io.smallrye.mutiny.Uni.createFrom().failure(t);
                    }
                });

    }

    /**
     * No-op router configurator for compatibility with Vert.x setup.
     *
     * @param builder the Router builder
     * @return the unmodified builder
     */
    @Override
    public Router builder(Router builder) {
        return builder;
    }

    /**
     * Applies configured WebSocket server options to the HTTP server.
     *
     * @param builder the HttpServerOptions builder
     * @return the configured builder
     * @throws IllegalArgumentException if option validation fails
     */
    @Override
    public HttpServerOptions builder(HttpServerOptions builder) {
        webSocketServerOptions.validate();

        builder = builder.setRegisterWebSocketWriteHandlers(
                webSocketServerOptions.isRegisterWebSocketWriteHandlers());
        builder = builder.setPerMessageWebSocketCompressionSupported(
                webSocketServerOptions.isPerMessageCompressionSupported());
        builder = builder.setCompressionLevel(
                webSocketServerOptions.getCompressionLevel());
        builder = builder.setMaxChunkSize(
                webSocketServerOptions.getMaxChunkSize());
        builder = builder.setMaxFormAttributeSize(
                webSocketServerOptions.getMaxFormAttributeSize());

        return builder;
    }


}
