package com.guicedee.vertx.websockets.implementations;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.guicedee.client.Environment;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.vertx.spi.VertxHttpServerConfigurator;
import com.guicedee.vertx.websockets.GuicedWebSocket;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.java.Log;

import static com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket.EveryoneGroup;

@Log
public class VertxHttpWebSocketConfigurator implements IGuicePostStartup<VertxHttpWebSocketConfigurator>,
                                                       VertxHttpServerConfigurator
{
    @Inject
    Vertx vertx;

    @Inject
    CallScoper callScoper;

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
                //what happens on a message received
                ctx.textMessageHandler((msg) -> {
                    try
                    {
                        callScoper.enter();
                        callScoper.scope(Key.get(ServerWebSocket.class), () -> ctx);
                        GuicedWebSocket guicedWebSocket = (GuicedWebSocket) IGuiceContext.get(IGuicedWebSocket.class);
                        guicedWebSocket.receiveMessage(msg);
                    }finally
                    {
                        callScoper.exit();
                    }
                   })
                   .exceptionHandler((e) -> {
                       System.out.println("Closed, restarting in 10 seconds");
                   })
                   .closeHandler((__) -> {
                       System.out.println("Closed, restarting in 10 seconds");
                   });

                String id = ctx.textHandlerID();
                log.fine("Client connected: " + ctx.remoteAddress() + " / " + id);
                //add to default groups, everyone and me
                vertx.eventBus()
                     .consumer(EveryoneGroup, message -> {
                         ctx.writeTextMessage((String) message.body());
                     });
                /*vertx.eventBus()
                     .consumer(id, message -> {
                         ctx.writeTextMessage((String) message.body());
                     });*/
                System.out.println("Connected - " + id);
            }
            finally
            {
                callScoper.exit();
            }
        });
        return builder;
    }
}
