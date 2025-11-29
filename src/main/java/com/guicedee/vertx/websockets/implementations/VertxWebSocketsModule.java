package com.guicedee.vertx.websockets.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.scopes.CallScope;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.services.websocket.IGuicedWebSocket;
import com.guicedee.client.services.websocket.GuicedWebSocketOnAddToGroup;
import com.guicedee.client.services.websocket.GuicedWebSocketOnPublish;
import com.guicedee.client.services.websocket.GuicedWebSocketOnRemoveFromGroup;
import com.guicedee.vertx.websockets.GuicedWebSocket;
import io.vertx.core.http.ServerWebSocket;

/**
 * Dependency injection module for WebSocket services.
 * 
 * Configures DI bindings for WebSocket-related classes and provides
 * multibinder extension points for SPI handlers.
 * 
 * @since 2.0.0
 */
public class VertxWebSocketsModule extends AbstractModule implements IGuiceModule<VertxWebSocketsModule>
{
    @Override
    protected void configure()
    {
        // Bind ServerWebSocket instance from CallScope properties
        bind(ServerWebSocket.class).toProvider(() -> (ServerWebSocket) IGuiceContext.get(CallScopeProperties.class)
                                                                                    .getProperties()
                                                                                    .get("ServerWebSocket"))
                                   .in(CallScope.class);
        
        // Bind IGuicedWebSocket to GuicedWebSocket implementation
        bind(IGuicedWebSocket.class).to(GuicedWebSocket.class);
        
        // Provide extension points for WebSocket SPI handlers
        // Applications can add handlers via: Multibinder<GuicedWebSocketOnAddToGroup> addBinder = 
        //   Multibinder.newSetBinder(binder(), GuicedWebSocketOnAddToGroup.class);
        // and register implementations
        Multibinder.newSetBinder(binder(), GuicedWebSocketOnAddToGroup.class);
        Multibinder.newSetBinder(binder(), GuicedWebSocketOnRemoveFromGroup.class);
        Multibinder.newSetBinder(binder(), GuicedWebSocketOnPublish.class);
    }
}
