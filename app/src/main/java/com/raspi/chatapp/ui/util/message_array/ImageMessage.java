package com.raspi.chatapp.ui.util.message_array;

public class ImageMessage extends MessageArrayContent{
  public boolean left;
  public String file;
  public String description;
  public int progress;
  public String url;
  public long time;
  public String status;
  public long _ID;
  public String chatId;
  public long othersId;

  public ImageMessage(boolean left, String file, String description,
                      String url, int progress, long time, String status,
                      long _ID, String chatId, long othersId){
    this.left = left;
    this.file = file;
    this.description = description;
    this.progress = progress;
    this.url = url;
    this.time = time;
    this.status = status;
    this._ID = _ID;
    this.chatId = chatId;
    this.othersId = othersId;
  }
}
