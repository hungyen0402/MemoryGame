package com.memorygame.common;

public class Vocabulary {
    private int id; 
    private String phrase; 
    private int length; 

    public Vocabulary(int id, String phrase, int length) {
        this.id = id;
        this.phrase = phrase;
        this.length = length;
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
