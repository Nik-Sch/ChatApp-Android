package com.raspi.chatapp.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MessageHistoryDbHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MessageHistory.db";


    public MessageHistoryDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void createMessageTable(String tableName){
        getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                MessageHistoryContract
                .COLUMN_NAME_MESSAGE_ID + " INTEGER PRIMARY KEY, " + MessageHistoryContract
                .COLUMN_NAME_BUDDY_ID + " TEXT, " + MessageHistoryContract
                .COLUMN_NAME_MESSAGE_TYPE + " TEXT, " + MessageHistoryContract
                .COLUMN_NAME_MESSAGE_CONTENT + " TEXT, " + MessageHistoryContract
                .COLUMN_NAME_MESSAGE_STATUS + " TEXT, " + MessageHistoryContract
                .COLUMN_NAME_MESSAGE_TIMESTAMP + " INTEGER)");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        super.onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

    }

    @Override
    public void onCreate(SQLiteDatabase db){
        getWritableDatabase().execSQL("CREATE TABLE " + MessageHistoryContract
                .TABLE_NAME_ALL_CHATS + " (" + MessageHistoryContract
                .COLUMN_NAME_BUDDY_ID + "TEXT PRIMARY KEY, " + MessageHistoryContract
                .COLUMN_NAME_NAME + " TEXT)");
    }
}
