package com.guicedee.vertx.websockets;

import com.google.inject.Singleton;
import lombok.Data;
import org.jspecify.annotations.NonNull;

/**
 * Configurable options for WebSocket server behavior.
 * 
 * This injectable singleton allows applications to customize WebSocket
 * server settings without modifying the framework configuration.
 * 
 * @since 2.0.0
 */
@Data
@Singleton
public class WebSocketServerOptions {
    
    /** Enable per-message WebSocket compression (RFC 7692). Default: true */
    private boolean perMessageCompressionSupported = true;
    
    /** Compression level (0-9). Default: 9 */
    private int compressionLevel = 9;
    
    /** Max frame size in bytes. Default: 65536 */
    private int maxFrameSize = 65536;
    
    /** Max chunk size in bytes. Default: 65536 */
    private int maxChunkSize = 65536;
    
    /** Max form attribute size in bytes. Default: 65536 */
    private int maxFormAttributeSize = 65536;
    
    /** Enable WebSocket write handlers. Default: true */
    private boolean registerWebSocketWriteHandlers = true;
    
    /** Connection idle timeout in seconds. Default: 300 */
    private int idleTimeoutSeconds = 300;
    
    /** Max WebSocket connections per group. Default: 10000 */
    private int maxGroupSize = 10000;
    
    /**
     * Validates the configured options.
     * 
     * @throws IllegalArgumentException if any option value is invalid
     */
    public void validate() throws IllegalArgumentException {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize must be > 0");
        }
        if (maxFrameSize <= 0) {
            throw new IllegalArgumentException("maxFrameSize must be > 0");
        }
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel must be 0-9");
        }
        if (maxGroupSize <= 0) {
            throw new IllegalArgumentException("maxGroupSize must be > 0");
        }
        if (idleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("idleTimeoutSeconds must be > 0");
        }
    }
}
