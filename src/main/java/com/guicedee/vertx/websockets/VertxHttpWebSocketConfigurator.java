package com.guicedee.vertx.websockets;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.vertx.spi.VertxHttpServerConfigurator;
import com.guicedee.vertx.spi.VertxHttpServerOptionsConfigurator;
import com.guicedee.vertx.spi.VertxRouterConfigurator;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.guicedee.guicedservlets.websockets.options.CallScopeSource.WebSocket;
import static com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket.EveryoneGroup;

@Log
@Singleton
public class VertxHttpWebSocketConfigurator implements IGuicePostStartup<VertxHttpWebSocketConfigurator>,
                                                       VertxHttpServerConfigurator, VertxRouterConfigurator,
                                                       VertxHttpServerOptionsConfigurator

{
    @Inject
    Vertx vertx;

    @Inject
    CallScoper callScoper;

    public Integer sortOrder()
    {
        return 55;
    }

    public static final Map<String, List<MessageConsumer<String>>> groupConsumers = new HashMap<>();
    public static final Map<String, List<ServerWebSocket>> groupSockets = new HashMap<>();
    public static final Map<String, CallScopeProperties> groupCallScopeProperties = new HashMap<>();

    @Override
    public List<CompletableFuture<Boolean>> postLoad()
    {
        return List.of();
    }

    public static void addToGroup(String group, ServerWebSocket webSocket)
    {
            configureGroupListener(IGuiceContext.get(Vertx.class), group,webSocket);
    }

    public static void removeFromGroup(String group, ServerWebSocket webSocket)
    {
        if (groupSockets.containsKey(group))
        {
            groupSockets.get(group).remove(webSocket);
            if (groupSockets.get(group).isEmpty() && !EveryoneGroup.equalsIgnoreCase(group))
            {
                groupSockets.remove(group);
            }
        }
    }

    @Override
    public HttpServer builder(HttpServer builder)
    {
        builder.webSocketHandler((ctx) -> {
            try
            {
                if (vertx == null)
                {
                    IGuiceContext.instance()
                                 .inject()
                                 .injectMembers(this);
                }
                callScoper.enter();
                callScoper.scope(Key.get(ServerWebSocket.class), () -> ctx);
                CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
                String id = ctx.textHandlerID();
                properties.setSource(WebSocket);
                properties.getProperties()
                          .put("RequestContextId", id);

                configureGroupListener(vertx, EveryoneGroup,ctx);

                //create my group id on connect
                groupConsumers.put(id, new ArrayList<>());
                groupCallScopeProperties.put(id, properties);

                configureGroupListener(vertx,id,ctx);

                //what happens on a message received
                ctx.textMessageHandler((msg) -> {
                       processMessageInContext(ctx, msg, properties);
                   })
                   .exceptionHandler((e) -> {
                       log.log(Level.SEVERE,"Exception on web handler",e);
                       groupSockets.forEach((key,value)->{
                           value.removeIf(a->a.textHandlerID().equals(id));
                       });
                       groupConsumers.forEach((key,value)->{
                           value.removeIf(a->a.address().equals(id));
                       });
                   })
                   .closeHandler((__) -> {
                       groupSockets.forEach((key,value)->{
                           value.removeIf(a->a.textHandlerID().equals(id));
                       });
                       groupConsumers.forEach((key,value)->{
                           value.removeIf(a->a.address().equals(id));
                       });
                   });

                log.fine("Client connected: " + ctx.remoteAddress() + " / " + id);
                //add to default groups, everyone and me
            }
            finally
            {
                callScoper.exit();
            }
        });
        return builder;
    }

    public static void configureGroupListener(Vertx vertx, String group,ServerWebSocket webSocket) {
        if (!groupConsumers.containsKey(group) || groupConsumers.get(group).isEmpty())
        {
            groupConsumers.put(group, new ArrayList<>());
            groupSockets.put(group, new ArrayList<>());
            MessageConsumer<String> r =  vertx.eventBus()
                    .consumer(group, message -> {
                        List<ServerWebSocket> serverWebSockets = groupSockets.get(group);
                        for (ServerWebSocket serverWebSocket : serverWebSockets) {
                            serverWebSocket.writeTextMessage((String) message.body());
                        }
                    });
            groupConsumers.get(group)
                    .add(r);
        }
        groupSockets.get(group).add(webSocket);

    }

    private void processMessageInContext(ServerWebSocket ctx, String msg, CallScopeProperties properties)
    {
        CallScoper callScoper = IGuiceContext.get(CallScoper.class);
        try
        {
            callScoper.enter();
            callScoper.scope(Key.get(ServerWebSocket.class), () -> ctx);

            CallScopeProperties props = IGuiceContext.get(CallScopeProperties.class);
            props.setSource(properties.getSource());
            properties.getProperties()
                      .put("ServerWebSocket", ctx);
            props.getProperties()
                 .putAll(properties.getProperties());
            GuicedWebSocket guicedWebSocket = (GuicedWebSocket) IGuiceContext.get(IGuicedWebSocket.class);
            guicedWebSocket.receiveMessage(msg);
        }
        finally
        {
            callScoper.exit();
        }
    }

    @Override
    public Router builder(Router builder)
    {
        return builder;
    }

    @Override
    public HttpServerOptions builder(HttpServerOptions builder)
    {
        builder = builder.setRegisterWebSocketWriteHandlers(true);
        builder = builder.setWebSocketAllowServerNoContext(true);
        builder = builder.setPerMessageWebSocketCompressionSupported(true);
        return builder;
    }


}
