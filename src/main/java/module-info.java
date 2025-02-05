import com.guicedee.vertx.websockets.implementations.VertxWebSocketsModule;
import com.guicedee.guicedinjection.interfaces.*;
import com.guicedee.vertx.spi.*;
import com.guicedee.vertx.websockets.*;

module com.guicedee.vertx.sockets {

    exports com.guicedee.vertx.websockets;

    uses com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
    uses com.guicedee.guicedservlets.servlets.services.IOnCallScopeEnter;
    uses com.guicedee.guicedservlets.servlets.services.IOnCallScopeExit;
    requires transitive com.guicedee.vertx;
    requires transitive com.guicedee.client;
    requires transitive io.vertx.core;

    requires static lombok;

    provides IGuicePostStartup with VertxHttpWebSocketConfigurator;
    provides com.guicedee.vertx.spi.VertxHttpServerConfigurator with VertxHttpWebSocketConfigurator;
    provides IGuiceModule with VertxWebSocketsModule;
    provides VertxHttpServerOptionsConfigurator with VertxHttpWebSocketConfigurator;

    opens com.guicedee.vertx.websockets.implementations to com.google.guice;
    opens com.guicedee.vertx.websockets to com.google.guice;

    uses GuicedWebSocketOnAddToGroup;
    uses GuicedWebSocketOnRemoveFromGroup;
    uses GuicedWebSocketOnPublish;
}