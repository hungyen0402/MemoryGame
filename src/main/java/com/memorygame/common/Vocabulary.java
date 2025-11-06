package com.memorygame.common;

public class Vocabulary {
    private final int id;
    private final String phrase;
    private final int length;

    public Vocabulary() {
        this.id = 0;
        this.phrase = null;
        this.length = 0;
    }

    public int getLength() {
        return this.length;
    }
}
