package com.jitendersingh.friendsengineer;

public class ChatMessage {

    private String text;
    private long timestamp;

    public ChatMessage() {
        // Required for Firestore deserialization
    }

    public ChatMessage(String text, long timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
