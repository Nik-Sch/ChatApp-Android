package com.raspi.chatapp.ui.util.message_array;

public class TextMessage extends MessageArrayContent{
  public boolean left;
  public String message;
  public long time;
  public String status;
  public long _ID;

  public TextMessage(boolean left, String message, long time, String status,
                     long _ID){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = status;
    this._ID = _ID;
  }
}
