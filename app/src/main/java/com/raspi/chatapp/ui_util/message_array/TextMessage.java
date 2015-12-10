package com.raspi.chatapp.ui_util.message_array;

import com.raspi.chatapp.sqlite.MessageHistory;

public class TextMessage extends MessageArrayContent{
  public boolean left;
  public String message;
  public long time;
  public String status;
  public long _ID;

  public TextMessage(boolean left, String message, long time, long _ID){
    this(left, message, time, MessageHistory.STATUS_WAITING, _ID);
  }

  public TextMessage(boolean left, String message, long time, String status, long _ID){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = status;
    this._ID = _ID;
  }
}
