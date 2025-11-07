package com.memorygame.common;

public class Message {
    private final String type;
    private final Object payload;

    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
