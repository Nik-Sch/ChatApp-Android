package com.raspi.chatapp.ui_util.message_array;

import com.raspi.chatapp.sqlite.MessageHistory;

public class TextMessage extends MessageArrayContent{
  public boolean left;
  public String message;
  public long time;
  public String status;

  public TextMessage(boolean left, String message, long time){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = MessageHistory.STATUS_WAITING;
  }

  public TextMessage(boolean left, String message, long time, String status){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = status;
  }
}
