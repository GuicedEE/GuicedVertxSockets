import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.vertx.spi.VertxHttpServerOptionsConfigurator;
import com.guicedee.vertx.websockets.VertxHttpWebSocketConfigurator;
import com.guicedee.vertx.websockets.implementations.VertxWebSocketsModule;

module guiced.vertx.sockets {

    exports com.guicedee.vertx.websockets;

    uses com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
    uses com.guicedee.guicedservlets.servlets.services.IOnCallScopeEnter;
    uses com.guicedee.guicedservlets.servlets.services.IOnCallScopeExit;
    requires transitive guiced.vertx;
    requires transitive com.guicedee.client;
    requires io.vertx;

    requires static lombok;

    provides IGuicePostStartup with VertxHttpWebSocketConfigurator;
    provides com.guicedee.vertx.spi.VertxHttpServerConfigurator with VertxHttpWebSocketConfigurator;
    provides IGuiceModule with VertxWebSocketsModule;
    provides VertxHttpServerOptionsConfigurator with VertxHttpWebSocketConfigurator;

    opens com.guicedee.vertx.websockets.implementations to com.google.guice;
    opens com.guicedee.vertx.websockets to com.google.guice;



}