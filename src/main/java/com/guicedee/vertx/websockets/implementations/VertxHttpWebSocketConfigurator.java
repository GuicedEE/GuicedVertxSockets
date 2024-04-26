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

    @Override
    public void postLoad()
    {
    }

    @Override
    public HttpServer builder(HttpServer builder)
    {
        builder.webSocketHandler((ctx) -> {
            try
            {
                callScoper.enter();
                callScoper.scope(Key.get(ServerWebSocket.class), () -> ctx);
                CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
                String id = ctx.textHandlerID();
                properties.setSource(WebSocket);

                if (!groupConsumers.containsKey(EveryoneGroup))
                {
                    groupConsumers.put(EveryoneGroup, new ArrayList<>());
                }

                MessageConsumer<Object> personalSocketSender = vertx.eventBus()
                                                                    .consumer(id, message -> {
                                                                        ctx.writeTextMessage((String) message.body());
                                                                    });
                properties.getProperties()
                          .put("Groups", new ArrayList<>());
                List<String> groups = (List<String>) properties.getProperties()
                                                               .get("Groups");
                groups.add(EveryoneGroup);
                groups.add(id);

                //what happens on a message received
                ctx.textMessageHandler((msg) -> {
                       try
                       {
                           callScoper.enter();
                           //   CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
                           properties.getProperties()
                                     .put("ServerWebSocket", ctx);
                           GuicedWebSocket guicedWebSocket = (GuicedWebSocket) IGuiceContext.get(IGuicedWebSocket.class);
                           guicedWebSocket.receiveMessage(msg);

                       }
                       finally
                       {
                           callScoper.exit();
                       }
                   })
                   .exceptionHandler((e) -> {
                       groupConsumers.get(EveryoneGroup).remove(personalSocketSender);
                       System.out.println("Closed, restarting in 10 seconds");
                   })
                   .closeHandler((__) -> {
                       groupConsumers.get(EveryoneGroup).remove(personalSocketSender);
                       System.out.println("Closed, restarting in 10 seconds");
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

    @Override
    public Router builder(Router builder)
    {
        return builder;
    }

    @Override
    public HttpServerOptions builder(HttpServerOptions builder)
    {
        builder.setRegisterWebSocketWriteHandlers(true);
        return builder;
    }
}
