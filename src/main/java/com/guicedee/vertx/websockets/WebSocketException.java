package com.guicedee.vertx.websockets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Unchecked exception for WebSocket operation failures.
 * 
 * This exception is thrown when WebSocket operations fail, replacing checked Exception
 * throughout the codebase to improve API clarity and error handling.
 * 
 * @since 2.0.0
 */
public class WebSocketException extends RuntimeException {
    
    /**
     * Creates a new WebSocketException with the given message.
     * 
     * @param message the error message (not null)
     */
    public WebSocketException(@NonNull String message) {
        super(message);
    }
    
    /**
     * Creates a new WebSocketException with the given message and cause.
     * 
     * @param message the error message (not null)
     * @param cause the underlying cause (may be null)
     */
    public WebSocketException(@NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new WebSocketException with the given cause.
     * 
     * @param cause the underlying cause (may be null)
     */
    public WebSocketException(@Nullable Throwable cause) {
        super(cause);
    }
}
