module guiced.vertx.tests.sockets {
    requires com.guicedee.vertx.sockets;
    requires com.google.guice;
    requires org.junit.jupiter.api;
    requires com.guicedee.client;
    requires transitive io.vertx.core;
    requires java.net.http;
    requires com.guicedee.jsonrepresentation;

    opens com.guicedee.vertx.tests.websockets to org.junit.platform.commons;
}