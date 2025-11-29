package com.guicedee.vertx.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScope;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.services.websocket.IGuicedWebSocket;
import com.guicedee.client.services.websocket.WebSocketMessageReceiver;
import com.guicedee.client.services.websocket.GuicedWebSocketOnAddToGroup;
import com.guicedee.client.services.websocket.GuicedWebSocketOnPublish;
import com.guicedee.client.services.websocket.GuicedWebSocketOnRemoveFromGroup;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@CallScope
@Log4j2
public class GuicedWebSocket extends AbstractVerticle implements IGuicedWebSocket
{
    @Inject
    private CallScopeProperties callScopeProperties;

    @Inject
    Vertx vertx;

    /**
     * Adds this WebSocket connection to a named group.
     * 
     * Attempts to use registered SPI handlers first; if none are found or if they
     * do not handle the add, falls back to the default group management system.
     * 
     * @param groupName the group identifier (not null, e.g., "chat:lobby")
     * @throws WebSocketException if the group operation fails
     * @throws NullPointerException if groupName is null
     * 
     * @see GuicedWebSocketOnAddToGroup
     * @see #removeFromGroup(String)
     */
    @Override
    public void addToGroup(@NonNull String groupName) throws WebSocketException
    {
        try {
            Set<GuicedWebSocketOnAddToGroup> onAdd = IGuiceContext.loaderToSet(ServiceLoader.load(GuicedWebSocketOnAddToGroup.class));
            CompletableFuture<Boolean> performed = new CompletableFuture<>();
            if(onAdd.isEmpty()) {
                performed.complete(false);
            } else {
                for (var guicedWebSocketOnAddToGroup : onAdd) {
                    performed = guicedWebSocketOnAddToGroup.onAddToGroup(groupName);
                    if(performed.get())
                        break;
                }
            }
            if(!performed.get()) {
                ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
                VertxSocketHttpWebSocketConfigurator.addToGroup(groupName, serverWebSocket1);
            }
        } catch (Exception e) {
            throw new WebSocketException("Failed to add to group: " + groupName, e);
        }
    }

    /**
     * Removes this WebSocket connection from a named group.
     * 
     * Attempts to use registered SPI handlers first; if none are found or if they
     * do not handle the removal, falls back to the default group management system.
     * 
     * @param groupName the group identifier (not null, e.g., "chat:lobby")
     * @throws WebSocketException if the group operation fails
     * @throws NullPointerException if groupName is null
     * 
     * @see GuicedWebSocketOnRemoveFromGroup
     * @see #addToGroup(String)
     */
    @Override
    public void removeFromGroup(@NonNull String groupName) throws WebSocketException
    {
        try {
            Set<GuicedWebSocketOnRemoveFromGroup> onRemove = IGuiceContext.loaderToSet(ServiceLoader.load(GuicedWebSocketOnRemoveFromGroup.class));
            CompletableFuture<Boolean> performed = new CompletableFuture<>();
            if(onRemove.isEmpty()) {
                performed.complete(false);
            } else {
                for (var guicedWebSocketOnRemoveFromGroup : onRemove) {
                    performed = guicedWebSocketOnRemoveFromGroup.onRemoveFromGroup(groupName);
                    if(performed.get())
                        break;
                }
            }
            if(!performed.get()) {
                ServerWebSocket serverWebSocket1 = IGuiceContext.get(ServerWebSocket.class);
                VertxSocketHttpWebSocketConfigurator.removeFromGroup(groupName, serverWebSocket1);
            }
        } catch (Exception e) {
            throw new WebSocketException("Failed to remove from group: " + groupName, e);
        }
    }

    /**
     * Broadcasts a message to all WebSocket connections in a named group.
     *
     * <p>This method publishes the message to the group's EventBus address;
     * all connected clients subscribed to the group receive the message.
     * If the group does not exist, it is created implicitly.</p>
     *
     * @param groupName the group identifier (not null, e.g., "chat:lobby")
     * @param message the message payload (not null; will be text-encoded)
     * @throws WebSocketException if broadcast fails (e.g., EventBus error)
     * @throws NullPointerException if groupName or message is null
     * 
     * @see #broadcastMessage(String)
     * @see #broadcastMessageSync(String, String)
     */
    public void broadcastMessage(@NonNull String groupName, @NonNull String message) throws WebSocketException
    {
        try {
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
                        throw new WebSocketException(e);
                    }
                    try {
                        if (performed.get())
                            break;
                    } catch (InterruptedException e) {
                        throw new WebSocketException(e);
                    } catch (ExecutionException e) {
                        throw new WebSocketException(e);
                    }
                }
            }else {
                performed.complete(false);
            }
            if(!performed.get()) {

                if (!VertxSocketHttpWebSocketConfigurator.groupSockets.containsKey(groupName)) {
                    log.warn("WS Group " + groupName + " not found, creating empty placeholder");
                    VertxSocketHttpWebSocketConfigurator.groupSockets.put(groupName, new ArrayList<>());
                }
                VertxSocketHttpWebSocketConfigurator.groupSockets.get(groupName).forEach(socket -> {
                    writeMessageToSocket(message, socket);
                });
            }
        } catch (InterruptedException e) {
            throw new WebSocketException("Interrupted while broadcasting to " + groupName, e);
        } catch (ExecutionException e) {
            throw new WebSocketException("Failed to broadcast to " + groupName, e);
        }
    }

    /**
     * Writes a text message to a WebSocket.
     * 
     * @param message the message to write (not null)
     * @param socket the WebSocket destination (not null)
     * @throws NullPointerException if message or socket is null
     */
    public static synchronized void writeMessageToSocket(@NonNull String message, @NonNull ServerWebSocket socket)
    {
        socket.writeTextMessage(message);
    }

    /**
     * Broadcasts a message to the current WebSocket connection.
     *
     * <p>Sends the message only to this connection's request context ID.</p>
     *
     * @param message the message payload (not null)
     * @throws NullPointerException if message is null
     * 
     * @see #broadcastMessage(String, String)
     */
    public void broadcastMessage(@NonNull String message)
    {
        vertx.eventBus()
             .send(callScopeProperties.getProperties()
                                      .get("RequestContextId")
                                      .toString(), message);
    }

    /**
     * Broadcasts a message synchronously to the current WebSocket connection.
     *
     * <p>This is a synchronous variant that immediately writes to the socket,
     * without going through the EventBus.</p>
     *
     * @param groupName the group identifier (not null; preserved for context)
     * @param message the message payload (not null)
     * @throws NullPointerException if groupName or message is null
     * 
     * @see #broadcastMessage(String, String)
     */
    public void broadcastMessageSync(@NonNull String groupName, @NonNull String message)
    {
        writeMessageToSocket(message, ((ServerWebSocket) callScopeProperties.getProperties()
                .get("ServerWebSocket")));
    }

    /**
     * Receives and processes a text message from the WebSocket.
     *
     * <p>Deserializes the message as JSON, dispatches to registered handlers,
     * and returns an async Uni for non-blocking composition.</p>
     *
     * @param message the JSON message payload (not null)
     * @return a Uni that completes when message processing is done
     * @throws NullPointerException if message is null
     * 
     * @see #receiveMessage(WebSocketMessageReceiver)
     */
    public io.smallrye.mutiny.Uni<Void> receiveMessage(@NonNull String message)
    {
        return io.smallrye.mutiny.Uni.createFrom().item(() -> {
                    try {
                        return IGuiceContext.get(ObjectMapper.class)
                                .readValue(message, WebSocketMessageReceiver.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .onItem().transformToUni(messageReceived -> {
                    String requestContextId = callScopeProperties.getProperties()
                            .get("RequestContextId")
                            .toString();
                    messageReceived.setBroadcastGroup(requestContextId);
                    if (IGuicedWebSocket.getMessagesListeners()
                            .containsKey(messageReceived.getAction()))
                    {
                        return IGuicedWebSocket.getMessagesListeners()
                                .get(messageReceived.getAction())
                                .receiveMessage(messageReceived);
                    }
                    else
                    {
                        log.warn("No web socket action registered for " + messageReceived.getAction());
                        return io.smallrye.mutiny.Uni.createFrom().voidItem();
                    }
                })
                .onFailure().invoke(e -> log.error("ERROR Message Received - Message=" + message, e))
                .replaceWithVoid();
    }

    /**
     * Receives and processes a deserialized message from the WebSocket.
     *
     * <p>Dispatches to registered handlers and returns an async Uni for
     * non-blocking composition.</p>
     *
     * @param messageReceived the deserialized message (not null)
     * @return a Uni that completes when message processing is done
     * @throws NullPointerException if messageReceived is null
     * 
     * @see #receiveMessage(String)
     */
    public io.smallrye.mutiny.Uni<Void> receiveMessage(@NonNull WebSocketMessageReceiver<?> messageReceived)
    {
        return io.smallrye.mutiny.Uni.createFrom().item(() -> {
                    String requestContextId = callScopeProperties.getProperties()
                            .get("RequestContextId")
                            .toString();
                    messageReceived.setBroadcastGroup(requestContextId);
                    return messageReceived;
                })
                .onItem().transformToUni(msg -> {
                    if (IGuicedWebSocket.getMessagesListeners()
                            .containsKey(msg.getAction()))
                    {
                        return IGuicedWebSocket.getMessagesListeners()
                                .get(msg.getAction())
                                .receiveMessage(msg);
                    }
                    else
                    {
                        log.warn("No web socket action registered for " + msg.getAction());
                        return io.smallrye.mutiny.Uni.createFrom().voidItem();
                    }
                })
                .onFailure().invoke(e -> log.error("ERROR Message Received - Message=" + messageReceived.toString(), e))
                .replaceWithVoid();
    }


}
