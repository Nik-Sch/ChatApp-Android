package com.raspi.chatapp;

public class ChatMessage {
    boolean left;
    String message;
    String time;

    public ChatMessage(boolean left, String message, String time){
        super();
        this.left = left;
        this.message = message;
        this.time = time;
    }
}
