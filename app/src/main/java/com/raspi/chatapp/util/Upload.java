package com.raspi.chatapp.util;

import com.raspi.chatapp.util.sqlite.MessageHistory;

import org.jivesoftware.smack.XMPPConnection;

import java.io.File;

public class Upload{
  public static class Task{
    File file;
    String description;
    String chatId;
    long messageID;
    XMPPConnection connection;
    MessageHistory messageHistory;

    public Task(File file, String description, String chatId, long
            messageID, XMPPConnection connection, MessageHistory messageHistory){
      this.file = file;
      this.description = description;
      this.chatId = chatId;
      this.messageID = messageID;
      this.connection = connection;
      this.messageHistory = messageHistory;
    }
  }

  public static class Result{
    double progress;
    String chatId;
    long messageID;

    public Result(double result, String chatId, long messageID){
      this.progress = result;
      this.chatId = chatId;
      this.messageID = messageID;
    }
  }
}
