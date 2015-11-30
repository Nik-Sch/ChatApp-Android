package com.raspi.chatapp.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

public class MessageHistory{
    MessageHistoryDbHelper mDbHelper;

    public MessageHistory(Context context){
       mDbHelper = new MessageHistoryDbHelper(context);
    }

    public Cursor getChats(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.query(MessageHistoryContract.TABLE_NAME_ALL_CHATS, new
                String[]{MessageHistoryContract.COLUMN_NAME_BUDDY_ID, MessageHistoryContract
                        .COLUMN_NAME_NAME},
                null, null, null, null, null);
    }

    public void addChat(String buddyId, String name){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.COLUMN_NAME_NAME, name);
        db.insert(MessageHistoryContract.TABLE_NAME_ALL_CHATS, null, values);
        mDbHelper.createMessageTable(buddyId);
    }

    public Cursor getMessages(String buddyId, int limit){
        return getMessages(buddyId, limit, 0);
    }

    public Cursor getMessages(String buddyId, int amount, int limit){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] columns = new String[]{
                MessageHistoryContract.COLUMN_NAME_BUDDY_ID,
                MessageHistoryContract.COLUMN_NAME_MESSAGE_TYPE,
                MessageHistoryContract.COLUMN_NAME_MESSAGE_CONTENT,
                MessageHistoryContract.COLUMN_NAME_MESSAGE_STATUS,
                MessageHistoryContract.COLUMN_NAME_MESSAGE_TIMESTAMP
        };
        return db.query(buddyId, columns, null, null, null, null, MessageHistoryContract
                .COLUMN_NAME_MESSAGE_TIMESTAMP + " DESC", " LIMIT " + amount + " OFFSET " + limit);
    }

    public void addMessage(String chatId, String buddyId, String type, String content, String
            status){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.COLUMN_NAME_MESSAGE_TYPE, type);
        values.put(MessageHistoryContract.COLUMN_NAME_MESSAGE_CONTENT, content);
        values.put(MessageHistoryContract.COLUMN_NAME_MESSAGE_STATUS, status);
        values.put(MessageHistoryContract.COLUMN_NAME_MESSAGE_TIMESTAMP, new Date().getTime());
        db.insert(chatId, null, values);
    }


}
