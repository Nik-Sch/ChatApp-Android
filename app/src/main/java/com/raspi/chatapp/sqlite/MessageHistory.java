package com.raspi.chatapp.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;

public class MessageHistory{
    MessageHistoryDbHelper mDbHelper;

    public MessageHistory(Context context){
        mDbHelper = new MessageHistoryDbHelper(context);
    }

    public Cursor getChats(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.query(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS, new
                        String[]{MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID,
                        MessageHistoryContract.ChatEntry
                                .COLUMN_NAME_NAME},
                null, null, null, null, null);
    }

    public void addChat(String buddyId, String name){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_NAME, name);
        try{
            db.insertOrThrow(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS,
                    MessageHistoryContract.ChatEntry._ID, values);
        } catch (SQLException e){
            Log.d("DB_DEBUG", "Couldn't insert --> is already inserted.");
            return;
        } catch (Exception e){
            Log.e("ERROR", "got an error while inserting a row into " + MessageHistoryContract
                    .ChatEntry.TABLE_NAME_ALL_CHATS);
            return;
        }
        mDbHelper.createMessageTable(buddyId);
    }

    public Cursor getMessages(String buddyId, int limit){
        return getMessages(buddyId, limit, 0);
    }

    public Cursor getMessages(String buddyId, int amount, int limit){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] columns = new String[]{
                MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP
        };
        return db.query(buddyId, columns, null, null, null, null, MessageHistoryContract.MessageEntry
                .COLUMN_NAME_MESSAGE_TIMESTAMP + " DESC", " LIMIT " + amount + " OFFSET " + limit);
    }

    public void addMessage(String chatId, String buddyId, String type, String content, String
            status){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE, type);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT, content);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS, status);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP, new Date().getTime());
        db.insert(chatId, MessageHistoryContract.MessageEntry._ID, values);
    }


}
