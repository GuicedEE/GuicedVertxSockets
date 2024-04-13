package com.guicedee.vertx.websockets.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.vertx.websockets.GuicedWebSocket;

public class VertxWebSocketsModule extends AbstractModule implements IGuiceModule<VertxWebSocketsModule>
{
    private final CallScoper callScope = new CallScoper();

    @Override
    protected void configure()
    {
        super.configure();
        bindScope(CallScope.class, callScope);
        bind(CallScopeProperties.class).in(CallScope.class);
        //bind(ServerWebSocket.class).in(CallScope.class);
        bind(IGuicedWebSocket.class).to(GuicedWebSocket.class);
    }
}
