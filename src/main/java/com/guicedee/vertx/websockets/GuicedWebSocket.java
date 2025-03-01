package com.guicedee.vertx.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.guicedservlets.websockets.options.IGuicedWebSocket;
import com.guicedee.guicedservlets.websockets.options.WebSocketMessageReceiver;
import com.guicedee.guicedservlets.websockets.services.GuicedWebSocketOnAddToGroup;
import com.guicedee.guicedservlets.websockets.services.GuicedWebSocketOnPublish;
import com.guicedee.guicedservlets.websockets.services.GuicedWebSocketOnRemoveFromGroup;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    public void addToGroup(String groupName)  throws Exception
    {
        Set<GuicedWebSocketOnAddToGroup> onAdd = IGuiceContext.loaderToSet(ServiceLoader.load(GuicedWebSocketOnAddToGroup.class));
        CompletableFuture<Boolean> performed = new CompletableFuture<>();
        if(onAdd.isEmpty()) {
            performed.complete(false);
        }else
            for (var guicedWebSocketOnAddToGroup : onAdd) {
            performed = guicedWebSocketOnAddToGroup.onAddToGroup(groupName);
                if(performed.get())
                    break;
        }
        if(!performed.get()) {
            ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
            VertxSocketHttpWebSocketConfigurator.addToGroup(groupName, serverWebSocket1);
        }
    }

    @Override
    public void removeFromGroup(String groupName)  throws Exception
    {
        Set<GuicedWebSocketOnRemoveFromGroup> onRemove = IGuiceContext.loaderToSet(ServiceLoader.load(GuicedWebSocketOnRemoveFromGroup.class));
        CompletableFuture<Boolean> performed = new CompletableFuture<>();
        if(onRemove.isEmpty()) {
            performed.complete(false);
        }
        else for (var guicedWebSocketOnRemoveFromGroup : onRemove) {
            performed = guicedWebSocketOnRemoveFromGroup.onRemoveFromGroup(groupName);
            if(performed.get())
                break;
        }
        if(!performed.get()) {
            ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
            VertxSocketHttpWebSocketConfigurator.removeFromGroup(groupName, serverWebSocket1);
        }
    }

    /**
     * Broadcast a given message to the web socket
     *
     * @param groupName The broadcast group to send to
     * @param message   The message to send
     */
    public void broadcastMessage(String groupName, String message)
    {
        String contextId = null;
        if(callScopeProperties.getProperties()
                .get("RequestContextId")!= null)
        {
            contextId = callScopeProperties.getProperties()
                    .get("RequestContextId")
                    .toString();
        }

        Set<GuicedWebSocketOnPublish> onRemove = IGuiceContext.loaderToSet(ServiceLoader.load(GuicedWebSocketOnPublish.class));
        CompletableFuture<Boolean> performed = new CompletableFuture<>();
        if(Strings.isNullOrEmpty(contextId)) {
            if(onRemove.isEmpty()) {
                performed.complete(false);
            }
            else for (var guicedWebSocketOnRemoveFromGroup : onRemove) {
                try {
                    performed.complete(guicedWebSocketOnRemoveFromGroup.publish(groupName,message));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                try {
                    if (performed.get())
                        break;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }else {
            performed.complete(false);
        }
        try {
            if(!performed.get()) {

                if (!VertxSocketHttpWebSocketConfigurator.groupSockets.containsKey(groupName)) {
                    log.warning("WS Group " + groupName + " not found, creating empty placeholder");
                    VertxSocketHttpWebSocketConfigurator.groupSockets.put(groupName, new ArrayList<>());
                }
                VertxSocketHttpWebSocketConfigurator.groupSockets.get(groupName).forEach(socket -> {
                    writeMessageToSocket(message, socket);
                });
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void writeMessageToSocket(String message, ServerWebSocket socket)
    {
        socket.writeTextMessage(message);
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
        writeMessageToSocket(message, ((ServerWebSocket) callScopeProperties.getProperties()
                .get("ServerWebSocket")));
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

    public void receiveMessage(WebSocketMessageReceiver<?> messageReceived)
    {
        try
        {
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
            log.log(Level.SEVERE, "ERROR Message Received - Message=" + messageReceived.toString(), e);
        }
    }


}
