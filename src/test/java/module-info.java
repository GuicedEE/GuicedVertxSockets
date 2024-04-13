module guiced.vertx.tests.sockets {
    requires guiced.vertx.sockets;
    requires com.google.guice;
    requires org.junit.jupiter.api;
    requires com.guicedee.client;
    requires io.vertx;
    requires java.net.http;
    requires com.guicedee.jsonrepresentation;

    opens com.guicedee.vertx.tests.websockets to org.junit.platform.commons;
}