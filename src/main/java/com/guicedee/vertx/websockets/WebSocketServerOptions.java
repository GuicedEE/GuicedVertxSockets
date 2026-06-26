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
 */
@Data
@Singleton
public class WebSocketServerOptions {
    
    /**
     * Enable per-message WebSocket compression (RFC 7692 / permessage-deflate). Default: false.
     *
     * <p>Defaults to {@code false} on purpose. permessage-deflate is only a bandwidth optimization,
     * but several browser STOMP clients (notably {@code @stomp/stompjs}) drop a freshly-upgraded
     * WebSocket immediately when the server negotiates the deflate extension - surfacing server-side
     * as an instant {@code HttpClosedException} and client-side as a code 1006 close. Because all
     * {@code VertxHttpServerOptionsConfigurator}s now share a single {@link io.vertx.core.http.HttpServerOptions}
     * instance, forcing this {@code true} here would clobber other configurators (e.g. JWebMP/STOMP)
     * that disable it. Leave it {@code false} unless an application explicitly opts back in.</p>
     */
    private boolean perMessageCompressionSupported = false;

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
