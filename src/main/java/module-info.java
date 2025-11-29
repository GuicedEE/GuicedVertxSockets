import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.client.services.lifecycle.IOnCallScopeEnter;
import com.guicedee.client.services.lifecycle.IOnCallScopeExit;
import com.guicedee.client.services.websocket.GuicedWebSocketOnAddToGroup;
import com.guicedee.client.services.websocket.GuicedWebSocketOnPublish;
import com.guicedee.client.services.websocket.GuicedWebSocketOnRemoveFromGroup;
import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;
import com.guicedee.vertx.websockets.implementations.VertxWebSocketsModule;
import com.guicedee.vertx.web.spi.*;
import com.guicedee.vertx.websockets.*;

module com.guicedee.vertx.sockets {

    exports com.guicedee.vertx.websockets;

    uses IWebSocketMessageReceiver;
    uses IOnCallScopeEnter;
    uses IOnCallScopeExit;
    requires transitive com.guicedee.vertx.web;


    requires static lombok;

    provides IGuicePostStartup with VertxSocketHttpWebSocketConfigurator;
    provides VertxHttpServerConfigurator with VertxSocketHttpWebSocketConfigurator;
    provides IGuiceModule with VertxWebSocketsModule;
    provides VertxHttpServerOptionsConfigurator with VertxSocketHttpWebSocketConfigurator;

    opens com.guicedee.vertx.websockets.implementations to com.google.guice;
    opens com.guicedee.vertx.websockets to com.google.guice;

    uses GuicedWebSocketOnAddToGroup;
    uses GuicedWebSocketOnRemoveFromGroup;
    uses GuicedWebSocketOnPublish;
}