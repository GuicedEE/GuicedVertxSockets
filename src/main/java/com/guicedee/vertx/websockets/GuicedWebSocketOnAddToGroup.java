package com.guicedee.vertx.websockets;

import com.guicedee.guicedinjection.interfaces.IDefaultService;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface GuicedWebSocketOnAddToGroup<J extends GuicedWebSocketOnAddToGroup<J>> extends IDefaultService<J> {
    /**
     * Returns true if the process is already complete
     * @param groupName
     * @return
     */
    CompletableFuture<Boolean> onAddToGroup(String groupName);
}
