package com.raspi.chatapp.ui_util.message_array;

import java.io.File;

/**
 * Created by gamer on 12/22/2015.
 */
public class ImageMessage extends MessageArrayContent{
  public boolean left;
  public File file;
  public String description;
  public long time;
  public double progress;
  public String status;
  public long _ID;

  public ImageMessage(boolean left, File file, String description, long time,
                      double progress, String status, long _ID){
    this.left = left;
    this.file = file;
    this.description = description;
    this.time = time;
    this.progress = progress;
    this.status = status;
    this._ID = _ID;
  }
}
