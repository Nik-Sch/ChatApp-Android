package com.raspi.chatapp.ui.util.message_array;

import com.raspi.chatapp.util.storage.MessageHistory;

public class TextMessage extends MessageArrayContent{
  public boolean left;
  public String message;
  public long time;
  public String status;

  public TextMessage(boolean left, String message, long time){
    this(left, message, time, MessageHistory.STATUS_WAITING);
  }

  public TextMessage(boolean left, String message, long time, String status){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = status;
  }
}
