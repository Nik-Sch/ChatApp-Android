package com.raspi.chatapp.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class MessageHistoryDbHelper extends SQLiteOpenHelper{
  public static final int DATABASE_VERSION = 2;
  public static final String DATABASE_NAME = "MessageHistory.db";
  private static final String CREATE_ALL_CHATS = "CREATE TABLE IF NOT EXISTS " +
          MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS + " (" + MessageHistoryContract
          .ChatEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MessageHistoryContract
          .ChatEntry.COLUMN_NAME_BUDDY_ID + " TEXT UNIQUE, " + MessageHistoryContract
          .ChatEntry
          .COLUMN_NAME_NAME + " TEXT, " + MessageHistoryContract.ChatEntry.COLUMN_NAME_LAST_ONLINE + " TEXT)";


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
    db.execSQL("DROP TABLE IF EXISTS " + MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS);
    onCreate(db);
  }

  @Override
  public void onCreate(SQLiteDatabase db){
    db.execSQL(CREATE_ALL_CHATS);
  }

  //just for debugging purpose
  public ArrayList<Cursor> getData(String Query){
    //get writable database
    SQLiteDatabase sqlDB = this.getWritableDatabase();
    String[] columns = new String[]{"mesage"};
    //an array list of cursor to save two cursors one has results from the query
    //other cursor stores error message if any errors are triggered
    ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
    MatrixCursor Cursor2 = new MatrixCursor(columns);
    alc.add(null);
    alc.add(null);


    try{
      String maxQuery = Query;
      //execute the query results will be save in Cursor c
      Cursor c = sqlDB.rawQuery(maxQuery, null);


      //add value to cursor2
      Cursor2.addRow(new Object[]{"Success"});

      alc.set(1, Cursor2);
      if (null != c && c.getCount() > 0){


        alc.set(0, c);
        c.moveToFirst();

        return alc;
      }
      return alc;
    }catch (SQLException sqlEx){
      Log.d("printing exception", sqlEx.getMessage());
      //if any exceptions are triggered save the error message to cursor an return the arraylist
      Cursor2.addRow(new Object[]{"" + sqlEx.getMessage()});
      alc.set(1, Cursor2);
      return alc;
    }catch (Exception ex){

      Log.d("printing exception", ex.getMessage());

      //if any exceptions are triggered save the error message to cursor an return the arraylist
      Cursor2.addRow(new Object[]{"" + ex.getMessage()});
      alc.set(1, Cursor2);
      return alc;
    }


  }
}
