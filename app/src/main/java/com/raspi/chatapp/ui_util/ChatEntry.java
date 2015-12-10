package com.raspi.chatapp.ui_util;

public class ChatEntry{

  public String buddyId;
  public String name;
  public String lastMessageStatus;
  public String lastMessageDate;
  public String lastMessageMessage;
  public boolean newMessage;
  public boolean sent;

  public ChatEntry(String buddyId, String name, String lastMessageStatus, String
          lastMessageDate, String lastMessageMessage, boolean newMessage, boolean sent){
    this.buddyId = buddyId;
    this.name = name;
    this.lastMessageStatus = lastMessageStatus;
    this.lastMessageDate = lastMessageDate;
    this.lastMessageMessage = lastMessageMessage;
    this.newMessage = newMessage;
    this.sent = sent;
  }
}
