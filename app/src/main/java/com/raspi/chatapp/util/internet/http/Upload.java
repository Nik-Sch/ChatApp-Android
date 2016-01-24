package com.raspi.chatapp.util.internet.http;

import android.content.Context;
import android.util.Log;

import com.alexbbb.uploadservice.MultipartUploadRequest;

import java.io.File;

public class Upload{
  public static final String SERVER_URL = "http://raspi-server.ddns" +
          ".net/ChatApp/upload.php";
  private static final String PARAM_FILE = "img_file";
  private static final String PARAM_TYPE = "type";

  public void uploadFile(final Context context, Task task){
    final String uploadID = task.chatId + "|" + task.messageID;
    try{
      new MultipartUploadRequest(context, uploadID, SERVER_URL)
              .addFileToUpload(task.file.getAbsolutePath(), PARAM_FILE)
              .addParameter(PARAM_TYPE, PARAM_FILE)
              .setMaxRetries(1)
              .startUpload();
    }catch (Exception e){
      Log.e("AndroidUploadService", e.getMessage(), e);
    }
  }

  public static class Task{
    public File file;
    public String chatId;
    public long messageID;

    public Task(File file, String chatId, long
            messageID){
      this.file = file;
      this.chatId = chatId;
      this.messageID = messageID;
    }
  }
}
