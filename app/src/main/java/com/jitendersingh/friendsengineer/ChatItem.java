package com.jitendersingh.friendsengineer;

public class ChatItem {
    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_DATE_HEADER = 0;

    private int type;
    private ChatMessage message;
    private String dateHeader;

    // Constructor for message
    public ChatItem(ChatMessage message) {
        this.type = TYPE_MESSAGE;
        this.message = message;
    }

    // Constructor for date header
    public ChatItem(String dateHeader) {
        this.type = TYPE_DATE_HEADER;
        this.dateHeader = dateHeader;
    }

    public int getType() {
        return type;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public String getDateHeader() {
        return dateHeader;
    }
}
