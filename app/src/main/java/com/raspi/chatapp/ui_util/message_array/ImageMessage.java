package com.raspi.chatapp.ui_util.message_array;

import java.io.File;


public class ImageMessage extends MessageArrayContent{
  public boolean left;
  public File file;
  public String description;
  public double progress;
  public long time;
  public String status;
  public long _ID;
  public String chatId;

  public ImageMessage(boolean left, File file, String description,
                      double progress, long time, String status, long _ID,
                      String chatId){
    this.left = left;
    this.file = file;
    this.description = description;
    this.progress = progress;
    this.time = time;
    this.status = status;
    this._ID = _ID;
    this.chatId = chatId;
  }
}
