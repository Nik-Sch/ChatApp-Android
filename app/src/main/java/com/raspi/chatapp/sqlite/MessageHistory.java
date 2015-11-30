package com.raspi.chatapp.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

    public Cursor getMessages(String buddyId, int limit){
        return null;
    }
}
