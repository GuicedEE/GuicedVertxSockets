package com.guicedee.vertx.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.guicedservlets.websockets.options.WebSocketMessageReceiver;
import com.guicedee.vertx.websockets.implementations.VertxHttpWebSocketConfigurator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.java.Log;

import java.util.logging.Level;

@CallScope
@Log
public class GuicedWebSocket extends AbstractVerticle implements IGuicedWebSocket
{
    @Inject
    private CallScopeProperties callScopeProperties;

    @Inject
    Vertx vertx;

    @Override
    public void addToGroup(String groupName)
    {
        ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
        VertxHttpWebSocketConfigurator.addToGroup(groupName, serverWebSocket1);
    }

    @Override
    public void removeFromGroup(String groupName)
    {
        ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
        VertxHttpWebSocketConfigurator.removeFromGroup(groupName, serverWebSocket1);
    }

    /**
     * Broadcast a given message to the web socket
     *
     * @param groupName The broadcast group to send to
     * @param message   The message to send
     */
    public void broadcastMessage(String groupName, String message)
    {
        vertx.eventBus()
             .send(groupName, message);
    }

    /**
     * Broadcast a given message to the web socket of the current context id
     *
     * @param message The message to send
     */
    public void broadcastMessage(String message)
    {
        vertx.eventBus()
             .send(callScopeProperties.getProperties()
                                      .get("RequestContextId")
                                      .toString(), message);
    }

    /**
     * Broadcast a given message to the web socket
     *
     * @param groupName The broadcast group to send to
     * @param message   The message to send
     */
    public void broadcastMessageSync(String groupName, String message)
    {
        ((ServerWebSocket) callScopeProperties.getProperties()
                                              .get("ServerWebSocket")).writeTextMessage(message);
    }

    public void receiveMessage(String message)
    {
        try
        {
            WebSocketMessageReceiver<?> messageReceived = IGuiceContext.get(ObjectMapper.class)
                                                                       .readValue(message, WebSocketMessageReceiver.class);
            String requestContextId = callScopeProperties.getProperties()
                                                         .get("RequestContextId")
                                                         .toString();
            messageReceived.setBroadcastGroup(requestContextId);
            if (IGuicedWebSocket.getMessagesListeners()
                                .containsKey(messageReceived.getAction()))
            {
                IGuicedWebSocket.getMessagesListeners()
                                .get(messageReceived.getAction())
                                .receiveMessage(messageReceived);
            }
            else
            {
                log.warning("No web socket action registered for " + messageReceived.getAction());
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "ERROR Message Received - Message=" + message, e);
        }
    }


}
