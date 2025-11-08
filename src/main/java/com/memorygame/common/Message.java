package com.memorygame.common;

public class Message {
    private final String type;
    private final Object payload;

    public Message() {
        this.type = null;
        this.payload = null;
    }

    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}
