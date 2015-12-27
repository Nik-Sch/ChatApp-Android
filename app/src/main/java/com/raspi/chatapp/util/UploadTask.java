package com.raspi.chatapp.util;

import com.raspi.chatapp.sqlite.MessageHistory;

import org.jivesoftware.smack.XMPPConnection;

import java.io.File;

public class UploadTask{
  File file;
  String description;
  String chatId;
  long messageID;
  XMPPConnection connection;
  MessageHistory messageHistory;

  public UploadTask(File file, String description, String chatId, long
          messageID, XMPPConnection connection, MessageHistory messageHistory){
    this.file = file;
    this.description = description;
    this.chatId = chatId;
    this.messageID = messageID;
    this.connection = connection;
    this.messageHistory = messageHistory;
  }
}
