package com.memorygame.common;

public class Vocabulary {
    private int id; 
    private String phrase; 
    private int length; 

    public Vocabulary(int id, int length, String phrase) {
        this.id = id;
        this.length = length;
        this.phrase = phrase;
    }

    public int getId() {
        return id;
    }

    public String getPhrase() {
        return phrase;
    }

    public int getLength() {
        return length;
    }
    
    
}
