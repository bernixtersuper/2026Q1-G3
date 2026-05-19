package com.menudigital.application.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class OrderEventBroadcaster {
    
    private final Map<UUID, SseBroadcaster> orderBroadcasters = new ConcurrentHashMap<>();
    private Sse sse;
    
    public void setSse(Sse sse) {
        this.sse = sse;
    }
    
    public SseBroadcaster getBroadcaster(UUID orderId) {
        return orderBroadcasters.computeIfAbsent(orderId, id -> {
            if (sse == null) {
                throw new IllegalStateException("SSE not initialized");
            }
            SseBroadcaster broadcaster = sse.newBroadcaster();
            broadcaster.onClose(eventSink -> {
                // Clean up if no more listeners
            });
            return broadcaster;
        });
    }
    
    public void broadcastOrderUpdate(UUID orderId, String status, String data) {
        SseBroadcaster broadcaster = orderBroadcasters.get(orderId);
        if (broadcaster != null && sse != null) {
            OutboundSseEvent event = sse.newEventBuilder()
                .name("order-update")
                .data(String.class, data)
                .build();
            broadcaster.broadcast(event);
        }
    }
    
    public void removeBroadcaster(UUID orderId) {
        SseBroadcaster broadcaster = orderBroadcasters.remove(orderId);
        if (broadcaster != null) {
            broadcaster.close();
        }
    }
}
