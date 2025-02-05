package com.guicedee.vertx.websockets;

import com.guicedee.guicedinjection.interfaces.IDefaultService;

@FunctionalInterface
public interface GuicedWebSocketOnPublish<J extends GuicedWebSocketOnPublish<J>> extends IDefaultService<J> {
    /**
     * Returns true if the process is already complete
     * @param groupName
     * @return
     */
    boolean publish(String groupName,String message) throws Exception;
}
