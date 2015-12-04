package com.raspi.chatapp.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MessageHistoryDbHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MessageHistory.db";
    private static final String CREATE_ALL_CHATS = "CREATE TABLE IF NOT EXISTS " +
            MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS + " (" + MessageHistoryContract
            .ChatEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MessageHistoryContract
            .ChatEntry.COLUMN_NAME_BUDDY_ID + " TEXT UNIQUE, " + MessageHistoryContract
            .ChatEntry
            .COLUMN_NAME_NAME + " TEXT)";


    public MessageHistoryDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void createMessageTable(String tableName){
        getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                MessageHistoryContract.MessageEntry
                        ._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MessageHistoryContract
                .MessageEntry
                .COLUMN_NAME_BUDDY_ID + " TEXT, " + MessageHistoryContract.MessageEntry
                .COLUMN_NAME_MESSAGE_TYPE + " TEXT, " + MessageHistoryContract.MessageEntry
                .COLUMN_NAME_MESSAGE_CONTENT + " TEXT, " + MessageHistoryContract.MessageEntry
                .COLUMN_NAME_MESSAGE_STATUS + " TEXT, " + MessageHistoryContract.MessageEntry
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
        db.execSQL(CREATE_ALL_CHATS);
    }
}
