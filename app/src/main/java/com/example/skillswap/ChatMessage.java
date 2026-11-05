package com.example.skillswap;

public class ChatMessage {
    private String senderEmail;
    private String senderName;
    private String text;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderEmail, String senderName, String text, long timestamp) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderEmail() { return senderEmail; }
    public String getSenderName() { return senderName; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}
