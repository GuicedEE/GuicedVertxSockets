package com.guicedee.vertx.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.guicedservlets.websockets.options.WebSocketMessageReceiver;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.java.Log;

import java.util.*;
import java.util.logging.Level;

@CallScope
@Log
public class GuicedWebSocket extends AbstractVerticle implements IGuicedWebSocket
{
    private static final Map<String, Set<Class<? extends IWebSocketMessageReceiver>>> messageListeners = new HashMap<>();

    @Inject
    private CallScopeProperties callScopeProperties;

    @Inject
    Vertx vertx;

    private Set<String> listedGroups = new HashSet<>();

    @Override
    public void addToGroup(String groupName)
    {
        if (listedGroups.contains(groupName))
        {
            vertx.eventBus()
                 .consumer(groupName, message -> {
                     ServerWebSocket serverWebSocket = (ServerWebSocket) callScopeProperties.getProperties()
                                                                                            .get("ServerWebSocket");
                     serverWebSocket.writeTextMessage((String) message.body());
                 });
        }
    }

    @Override
    public void removeFromGroup(String groupName)
    {

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
             .publish(groupName, message);
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
        if (messageListeners.isEmpty())
        {
            try
            {
                loadWebSocketReceivers();
            }
            catch (Throwable T)
            {
                log.log(Level.SEVERE, "Failed to load WebSocketReceivers", T);
            }
        }
        try
        {
            WebSocketMessageReceiver<?> messageReceived = IGuiceContext.get(ObjectMapper.class)
                                                                       .readValue(message, WebSocketMessageReceiver.class);
            if (messageListeners.containsKey(messageReceived.getAction()))
            {
                for (Class<? extends IWebSocketMessageReceiver> iWebSocketMessageReceiver : messageListeners.get(messageReceived.getAction()))
                {
                    IWebSocketMessageReceiver messageReceiver = IGuiceContext.get(iWebSocketMessageReceiver);
                    messageReceiver.receiveMessage(messageReceived);
                }
            }
            else {
                log.warning("No web socket action registered for " + messageReceived.getAction());
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "ERROR Message Received - Message=" + message, e);
        }
    }

    public void loadWebSocketReceivers()
    {
        if (messageListeners.isEmpty())
        {
            Set<IWebSocketMessageReceiver> messageReceivers = IGuiceContext
                    .instance()
                    .getLoader(IWebSocketMessageReceiver.class, ServiceLoader.load(IWebSocketMessageReceiver.class));
            for (IWebSocketMessageReceiver messageReceiver : messageReceivers)
            {
                for (String s : messageReceiver.messageNames())
                {
                    addReceiver(messageReceiver, s);
                }
            }
        }
    }

    public void addWebSocketMessageReceiver(IWebSocketMessageReceiver receiver)
    {
        for (String messageName : receiver.messageNames())
        {
            addReceiver(receiver, messageName);
        }
    }

    public boolean isWebSocketReceiverRegistered(String name)
    {
        return messageListeners.containsKey(name);
    }

    private void addReceiver(IWebSocketMessageReceiver messageReceiver, String action)
    {
        if (!messageListeners.containsKey(action))
        {
            messageListeners.put(action, new HashSet<>());
        }
        messageListeners.get(action)
                        .add(messageReceiver.getClass());
    }


}
