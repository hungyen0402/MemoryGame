// src/main/java/com/memorygame/common/Message.java
package com.memorygame.common;

import java.io.Serializable;

public class Message implements Serializable {
    private final String type;
    private final Object payload;

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

    @Override
    public String toString() {
        return "Message{type='" + type + "', payload=" + payload + "}";
    }
}