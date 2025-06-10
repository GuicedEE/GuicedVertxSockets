package com.guicedee.vertx.websockets.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.client.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.vertx.websockets.GuicedWebSocket;
import io.vertx.core.http.HttpServerOptions;
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
