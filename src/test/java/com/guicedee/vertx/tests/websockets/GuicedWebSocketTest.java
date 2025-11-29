package com.guicedee.vertx.tests.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Injector;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.websocket.WebSocketMessageReceiver;
import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

class GuicedWebSocketTest
{
    @Test
    void testSocket() throws ExecutionException, InterruptedException
    {
        Injector inject = IGuiceContext.instance()
                                       .inject();

        Vertx vertx = IGuiceContext.get(Vertx.class);
        HttpClient client = vertx.createHttpClient();

        CompletableFuture<WebSocket> webSocketCompletableFuture = java.net.http.HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8080"), new WebSocket.Listener()
                {
                    @Override
                    public void onOpen(WebSocket webSocket)
                    {
                        WebSocket.Listener.super.onOpen(webSocket);
                        WebSocketMessageReceiver<?> action = new WebSocketMessageReceiver<>()
                                .setAction("Action")
                                .setData(new HashMap<>());
                        try
                        {
                            webSocket.sendText(IJsonRepresentation.getObjectMapper()
                                                                  .writeValueAsString(action),true);
                        }
                        catch (JsonProcessingException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
                    {
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last)
                    {
                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message)
                    {
                        return WebSocket.Listener.super.onPing(webSocket, message);
                    }

                    @Override
                    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message)
                    {
                        return WebSocket.Listener.super.onPong(webSocket, message);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
                    {
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error)
                    {
                        WebSocket.Listener.super.onError(webSocket, error);
                    }
                });
        webSocketCompletableFuture.get();
    }
}
