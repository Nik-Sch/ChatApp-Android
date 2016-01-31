package com.raspi.chatapp.util.storage;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.ui.util.chat_array.ChatEntry;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.ui.util.message_array.TextMessage;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageHistory{
  public static final String TYPE_TEXT = "com.raspi.storage.MessageHistory.TYPE_TEXT";
  public static final String TYPE_IMAGE = "com.raspi.storage.MessageHistory.TYPE_IMAGE";

  public static final String STATUS_WAITING = "com.raspi.storage.MessageHistory.STATUS_WAITING";
  public static final String STATUS_SENDING = "com.raspi.storage.MessageHistory.STATUS_SENDING";
  public static final String STATUS_SENT = "com.raspi.storage.MessageHistory.STATUS_SENT";
  public static final String STATUS_RECEIVING = "com.raspi.storage.MessageHistory.STATUS_RECEIVING";
  public static final String STATUS_RECEIVED = "com.raspi.storage.MessageHistory.STATUS_RECEIVED";
  public static final String STATUS_READ = "com.raspi.storage.MessageHistory.STATUS_READ";

  MessageHistoryDbHelper mDbHelper;
  Context context;

  public MessageHistory(Context context){
    mDbHelper = new MessageHistoryDbHelper(context);
    this.context = context;
  }

  public ChatEntry[] getChats(){
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    Cursor chats = db.query(
            MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS,
            new String[]{
                    MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID,
                    MessageHistoryContract.ChatEntry.COLUMN_NAME_NAME},
            null, null, null, null, null);
    int chatCount = chats.getCount();
    ChatEntry[] resultChats = new ChatEntry[chatCount];
    int i = 0;
    chats.moveToFirst();
    if (chats.getCount() > 0)
      do{
          String buddyId = chats.getString(0);
          String name = chats.getString(1);
          MessageArrayContent mac = getLastMessage(buddyId);

          if (mac instanceof TextMessage){
            TextMessage msg = (TextMessage) mac;

            String lastMessageDate;
            Date msgTime = new Date(msg.time);
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            long diff = startOfDay.getTimeInMillis() - msgTime.getTime();
            if (diff <= 0)
              lastMessageDate = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(msgTime);
            else if (diff > 1000 * 60 * 60 * 24)
              lastMessageDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format
                      (msgTime);
            else
              lastMessageDate = "Yesterday";

            resultChats[i] = new ChatEntry(
                    buddyId,
                    name,
                    MessageHistory.TYPE_TEXT,
                    msg.status,
                    lastMessageDate,
                    ((msg.left)? name + ": " : "") + msg.message,
                    !msg.left);
          }else if (mac instanceof ImageMessage){
            ImageMessage msg = (ImageMessage) mac;

            String lastMessageDate;
            Date msgTime = new Date(msg.time);
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            long diff = startOfDay.getTimeInMillis() - msgTime.getTime();
            if (diff <= 0)
              lastMessageDate = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(msgTime);
            else if (diff > 1000 * 60 * 60 * 24)
              lastMessageDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format
                      (msgTime);
            else
              lastMessageDate = "Yesterday";
            msg.description += "".equals(msg.description)?context
                    .getResources().getString(R.string.image):"";

            resultChats[i] = new ChatEntry(
                    buddyId,
                    name,
                    MessageHistory.TYPE_IMAGE,
                    msg.status,
                    lastMessageDate,
                    msg.description,
                    !msg.left);
          }
        i++;
      }while (chats.move(1));
    chats.close();
    db.close();
    return resultChats;
  }

  public MessageArrayContent getLastMessage(String buddyId, boolean markAsRead){
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    Cursor c = db.query(
            buddyId,
            new String[]{MessageHistoryContract.MessageEntry._ID},
            null, null, null, null,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP
                    + " DESC", "1");
    c.moveToFirst();
    updateMessageStatus(buddyId, c.getLong(0), MessageHistory.STATUS_READ);
    return getLastMessage(buddyId);
  }

  public MessageArrayContent getLastMessage(String buddyId){
    MessageArrayContent mac = null;
    try{
      SQLiteDatabase db = mDbHelper.getReadableDatabase();

      String[] columns = new String[]{
              MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_URL,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_PROGRESS,
              MessageHistoryContract.MessageEntry._ID
      };
      Cursor lastMessage = db.query(buddyId, columns, null, null, null, null,
              MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP
                      + " DESC", "1");
      lastMessage.moveToFirst();
      if (lastMessage.getCount() != 0 && lastMessage.moveToFirst()){
        String type = lastMessage.getString(1);
        SharedPreferences preferences = context.getSharedPreferences(ChatActivity.PREFERENCES, 0);
        String me = preferences.getString(ChatActivity.USERNAME, "");
        boolean sent = me.equals(lastMessage.getString(0));

        if (TYPE_TEXT.equals(type)){
          mac = new TextMessage(
                  !sent,
                  lastMessage.getString(2),
                  lastMessage.getLong(4),
                  lastMessage.getString(3));
        }else if (TYPE_IMAGE.equals(type)){
          JSONArray contentJSON = new JSONArray(lastMessage.getString(2));
          mac = new ImageMessage(
                  !sent,
                  contentJSON.getString(0),
                  contentJSON.getString(1),
                  lastMessage.getString(5),
                  lastMessage.getInt(6),
                  lastMessage.getLong(4),
                  lastMessage.getString(3),
                  lastMessage.getLong(7),
                  lastMessage.getString(1));
        }
      }
    }catch (Exception e){

    }
    return mac;
  }

  public void addChat(String buddyId, String name){
    //Log.d("DATABASE", "Adding a text_message: " + buddyId + " - " + name);
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    //remove everything after @ if it exists
    int index = buddyId.indexOf('@');
    if (index >= 0){
      buddyId = buddyId.substring(0, index);
    }
    index = name.indexOf('@');
    if (index >= 0){
      name = name.substring(0, index);
    }
    ContentValues values = new ContentValues();
    values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID, buddyId);
    values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_NAME, name);
    try{
      db.insertOrThrow(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS,
              MessageHistoryContract.ChatEntry._ID, values);
    }catch (SQLException e){
      Log.d("DATABASE", "Couldn't insert --> is already inserted.");
      return;
    }catch (Exception e){
      Log.e("ERROR", "got an error while inserting a row into " + MessageHistoryContract
              .ChatEntry.TABLE_NAME_ALL_CHATS);
      return;
    }
    mDbHelper.createMessageTable(buddyId);
    db.close();
  }

  public String getOnline(String buddyId){
    int index = buddyId.indexOf('@');
    if (index >= 0){
      buddyId = buddyId.substring(0, index);
    }
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    Cursor c = db.query(MessageHistoryContract.ChatEntry
            .TABLE_NAME_ALL_CHATS, new String[]{MessageHistoryContract
            .ChatEntry.COLUMN_NAME_LAST_ONLINE}, MessageHistoryContract
                    .ChatEntry.COLUMN_NAME_BUDDY_ID + "=?", new
                    String[]{buddyId},
            null, null, null);
    try{
      c.moveToFirst();
      if (c.getCount() > 0)
        return c.getString(0);
    }catch (Exception e){
      e.printStackTrace();
    }
    c.close();
    return null;
  }

  public void setOnline(String buddyId, String status){
    int index = buddyId.indexOf('@');
    if (index >= 0){
      buddyId = buddyId.substring(0, index);
    }
    //Log.d("DATABASE", "Changing OnlineStatus");
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_LAST_ONLINE, status);
    String whereClause = MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID
            + " == ?";
    db.update(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS, values, whereClause, new
            String[]{buddyId});
    db.close();
  }

  public MessageArrayContent[] getMessages(String buddyId, int limit){
    return getMessages(buddyId, limit, 0);
  }

  public MessageArrayContent[] getMessages(String buddyId, int amount, int offset){
    //Log.d("DATABASE", "Getting messages");
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    String[] columns = new String[]{
            MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_URL,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_PROGRESS,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP,
            MessageHistoryContract.MessageEntry._ID
    };
    Cursor messages = db.query(buddyId, columns, null, null, null, null, MessageHistoryContract
            .MessageEntry
            .COLUMN_NAME_MESSAGE_TIMESTAMP + " DESC", offset + "," + amount);

    messages.moveToLast();
    int messageCount = messages.getCount();
    MessageArrayContent[] result = new MessageArrayContent[messageCount];
    int i = 0;
    if (messages.getCount() > 0)
      do{
        String from = messages.getString(0);
        SharedPreferences preferences = context.getSharedPreferences(ChatActivity
                .PREFERENCES, 0);
        String me = preferences.getString(ChatActivity.USERNAME, "");
        String type = messages.getString(1);
        String content = messages.getString(2);
        String url = messages.getString(3);
        int progress = messages.getInt(4);
        String status = messages.getString(5);
        long time = messages.getLong(6);
        long _ID = messages.getLong(7);
        switch (type){
          case (MessageHistory.TYPE_TEXT):
            result[i] = new TextMessage(!me.equals(from), content, time, status);
            if (((TextMessage) result[i]).left)
              updateMessageStatus(from, _ID, STATUS_READ);
            break;
          case (MessageHistory.TYPE_IMAGE):
            try{
              JSONArray contentJSON = new JSONArray(content);
              result[i] = new ImageMessage(
                      !me.equals(from),                   //left
                      contentJSON.getString(0),           //File
                      contentJSON.getString(1),           //description
                      url,                                //url
                      progress,                           //progress
                      time,                               //timeStamp
                      status,                             //status
                      _ID,                                //_ID
                      buddyId);                           //buddyID
            }catch (Exception e){
              e.printStackTrace();
            }
            break;
        }
        i++;
      }while (messages.move(-1));
    db.close();
    messages.close();
    return result;
  }

  public MessageArrayContent getMessage(String buddyId, String messageId){
    MessageArrayContent mac = null;
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    String[] columns = new String[]{
            MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_URL,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_PROGRESS,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
            MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP,
            MessageHistoryContract.MessageEntry._ID
    };
    String sel = MessageHistoryContract.MessageEntry._ID + "=?";
    Cursor message = db.query(buddyId, columns, sel, new
            String[]{messageId}, null, null, null);

    message.moveToFirst();
    String from = message.getString(0);
    SharedPreferences preferences = context.getSharedPreferences(ChatActivity
            .PREFERENCES, 0);
    String me = preferences.getString(ChatActivity.USERNAME, "");
    String type = message.getString(1);
    String content = message.getString(2);
    String url = message.getString(3);
    int progress = message.getInt(4);
    String status = message.getString(5);
    long time = message.getLong(6);
    long _ID = message.getLong(7);
    switch (type){
      case (MessageHistory.TYPE_TEXT):
        mac = new TextMessage(!me.equals(from), content, time, status);
        break;
      case (MessageHistory.TYPE_IMAGE):
        try{
          JSONArray contentJSON = new JSONArray(content);
          mac = new ImageMessage(
                  !me.equals(from),                   //left
                  contentJSON.getString(0),           //File
                  contentJSON.getString(1),           //description
                  url,                                //url
                  progress,                           //progress
                  time,                               //timeStamp
                  status,                             //status
                  _ID,                                //_ID
                  buddyId);                           //buddyID
        }catch (Exception e){
          e.printStackTrace();
        }
        break;
    }
    return mac;
  }

  public void addMessage(String chatId, String buddyId, String type, String
          content, String url, String progress, String status){
    //Log.d("DATABASE", "Adding a message");
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    //remove everything after @ if it exists
    int index = buddyId.indexOf('@');
    if (index >= 0){
      buddyId = buddyId.substring(0, index);
    }
    index = chatId.indexOf('@');
    if (index >= 0){
      chatId = chatId.substring(0, index);
    }

    ContentValues values = new ContentValues();
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID, buddyId);
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE, type);
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT, content);
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_URL, url);
    values.put(MessageHistoryContract.MessageEntry
            .COLUMN_NAME_MESSAGE_PROGRESS, progress);
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS, status);
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP, new Date().getTime());
    db.insert(chatId, MessageHistoryContract.MessageEntry._ID, values);
    db.close();
  }

  public void addMessage(String chatId, String buddyId, String type, String content, String
          status){
    addMessage(chatId, buddyId, type, content, "",  "0", status);
  }

  public void updateMessageStatus(String chatId, long _ID, String newStatus){
    //Log.d("DATABASE", "Changing MessageStatus");
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS, newStatus);
    String whereClause = MessageHistoryContract.MessageEntry._ID + " == ?";
    db.update(chatId, values, whereClause, new String[]{Long.toString(_ID)});
    db.close();
  }

  public void updateMessageProgress(String chatId, long _ID, double
          newProgress){
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(MessageHistoryContract.MessageEntry
            .COLUMN_NAME_MESSAGE_PROGRESS, newProgress);
    String whereClause = MessageHistoryContract.MessageEntry._ID + " == ?";
    db.update(chatId, values, whereClause, new String[]{Long.toString(_ID)});
    db.close();
  }
}
