package com.guicedee.vertx.websockets.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.scopes.CallScope;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.services.websocket.IGuicedWebSocket;
import com.guicedee.vertx.websockets.GuicedWebSocket;
import io.vertx.core.http.ServerWebSocket;

public class VertxWebSocketsModule extends AbstractModule implements IGuiceModule<VertxWebSocketsModule>
{
    @Override
    protected void configure()
    {
        bind(ServerWebSocket.class).toProvider(() -> (ServerWebSocket) IGuiceContext.get(CallScopeProperties.class)
                                                                                    .getProperties()
                                                                                    .get("ServerWebSocket"))
                                   .in(CallScope.class);
        bind(IGuicedWebSocket.class).to(GuicedWebSocket.class);
    }

}
