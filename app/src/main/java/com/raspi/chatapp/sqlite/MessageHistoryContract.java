package com.raspi.chatapp.sqlite;

import android.provider.BaseColumns;

public class MessageHistoryContract{
  public MessageHistoryContract(){
  }

  public static abstract class ChatEntry implements BaseColumns{
    public static final String TABLE_NAME_ALL_CHATS = "allchats";
    public static final String COLUMN_NAME_BUDDY_ID = "buddyid";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_LAST_ONLINE = "online";

  }

  public static abstract class MessageEntry implements BaseColumns{
    public static final String COLUMN_NAME_BUDDY_ID = "buddyid";
    public static final String COLUMN_NAME_MESSAGE_TYPE = "type";
    public static final String COLUMN_NAME_MESSAGE_CONTENT = "content";
    public static final String COLUMN_NAME_MESSAGE_STATUS = "status";
    public static final String COLUMN_NAME_MESSAGE_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_MESSAGE_PROGRESS = "progress";
  }
}
