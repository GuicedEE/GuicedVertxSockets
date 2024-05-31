package com.guicedee.vertx.websockets.implementations;

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
import com.guicedee.vertx.websockets.GuicedWebSocket;
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

    private static final Map<String, List<MessageConsumer<String>>> groupConsumers = new HashMap<>();
    private static final Map<String, List<ServerWebSocket>> groupSockets = new HashMap<>();

    @Override
    public List<CompletableFuture<Boolean>> postLoad()
    {
        return List.of();
    }

    public static void addToGroup(String group, ServerWebSocket webSocket)
    {
        if (!groupSockets.containsKey(group))
        {
            groupSockets.put(group, new ArrayList<>());
            IGuiceContext.get(Vertx.class)
                         .eventBus()
                         .consumer(group, message -> {
                             List<ServerWebSocket> serverWebSockets = groupSockets.get(group);
                             for (ServerWebSocket serverWebSocket : serverWebSockets)
                             {
                                 //send to everyone group
                                 serverWebSocket.writeTextMessage((String) message.body());
                             }
                         });
        }
        groupSockets.get(group).add(webSocket);
    }

    public static void removeFromGroup(String group, ServerWebSocket webSocket)
    {
        if (groupSockets.containsKey(group))
        {
            groupSockets.get(group).remove(webSocket);
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

                if (!groupConsumers.containsKey(EveryoneGroup))
                {
                    groupConsumers.put(EveryoneGroup, new ArrayList<>());
                    groupSockets.put(EveryoneGroup, new ArrayList<>());

                    vertx.eventBus()
                         .consumer(EveryoneGroup, message -> {
                             List<ServerWebSocket> serverWebSockets = groupSockets.get(EveryoneGroup);
                             for (ServerWebSocket serverWebSocket : serverWebSockets)
                             {
                                 //send to everyone group
                                 serverWebSocket.writeTextMessage((String) message.body());
                             }
                         });
                }
                groupSockets.get(EveryoneGroup)
                            .add(ctx);

                //create my group id on connect
                groupConsumers.put(id, new ArrayList<>());
                MessageConsumer<String> personalSocketSender = vertx.eventBus()
                                                                    .consumer(id, message -> {
                                                                        //send only to me
                                                                        ctx.writeTextMessage((String) message.body());
                                                                    });
                groupConsumers.get(id)
                              .add(personalSocketSender);

                //what happens on a message received
                ctx.textMessageHandler((msg) -> {
                       processMessageInContext(ctx, msg, properties);
                   })
                   .exceptionHandler((e) -> {
                       groupConsumers.get(EveryoneGroup)
                                     .remove(personalSocketSender);
                       groupSockets.get(EveryoneGroup)
                                   .remove(ctx);
                       groupConsumers.remove(id);
                       groupSockets.forEach((key,value)->{
                           value.removeIf(a->a.textHandlerID().equals(id));
                       });
                   })
                   .closeHandler((__) -> {
                       groupConsumers.get(EveryoneGroup)
                                     .remove(personalSocketSender);
                       groupSockets.get(EveryoneGroup)
                                   .remove(ctx);
                       groupConsumers.remove(id);
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

    private void processMessageInContext(ServerWebSocket ctx, String msg, CallScopeProperties properties)
    {
        CallScoper callScoper = IGuiceContext.get(CallScoper.class);
        try
        {
            callScoper.enter();
            callScoper.scope(Key.get(ServerWebSocket.class), () -> ctx);

            CallScopeProperties props = IGuiceContext.get(CallScopeProperties.class);
            props.setSource(properties.getSource());
            props.getProperties()
                 .putAll(properties.getProperties());
            properties.getProperties()
                      .put("ServerWebSocket", ctx);
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
        return builder;
    }
}
