package com.raspi.chatapp.ui.util.chat_array;

import com.raspi.chatapp.util.storage.MessageHistory;

public class ChatEntry{

  public String buddyId;
  public String name;
  public String lastMessageType;
  public String lastMessageStatus;
  public String lastMessageDate;
  public String lastMessageMessage;
  public boolean read;
  public boolean sent;

  public ChatEntry(String buddyId, String name, String lastMessageType, String
                   lastMessageStatus, String lastMessageDate, String
          lastMessageMessage, boolean sent){
    this.buddyId = buddyId;
    this.name = name;
    this.lastMessageType = lastMessageType;
    this.lastMessageStatus = lastMessageStatus;
    this.lastMessageDate = lastMessageDate;
    this.lastMessageMessage = lastMessageMessage;
    this.read = MessageHistory.STATUS_READ.equals(lastMessageStatus);
    this.sent = sent;
  }
}
