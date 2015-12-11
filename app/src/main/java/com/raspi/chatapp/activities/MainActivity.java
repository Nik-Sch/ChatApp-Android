package com.raspi.chatapp.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.service.MessageService;
import com.raspi.chatapp.sqlite.AndroidDatabaseManager;
import com.raspi.chatapp.sqlite.MessageHistory;
import com.raspi.chatapp.ui_util.ChatArrayAdapter;
import com.raspi.chatapp.ui_util.ChatEntry;
import com.raspi.chatapp.util.MyNotification;

public class MainActivity extends AppCompatActivity{

  public static final String PREFERENCES = "com.raspi.chatapp.activities.MainActivity.PREFERENCES";
  public static final String USERNAME = "com.raspi.chatapp.activities.MainActivity.USERNAME";
  public static final String PASSWORD = "com.raspi.chatapp.activities.MainActivity.PASSWORD";
  public static final String RECONNECT = "com.raspi.chatapp.activities.MainActivity.RECONNECT";
  public static final String APP_LAUNCHED = "con.raspi.chatapp.MainActivity.APP_CREATED";
  public static final String APP_CLOSED = "con.raspi.chatapp.MainActivity.APP_DESTROYED";
  public static final String BUDDY_ID = "com.raspi.chatapp.activities.MainActivity.BUDDY_ID";
  public static final String CHAT_NAME = "com.raspi.chatapp.activities.MainActivity.CHAT_NAME";
  public static final String MESSAGE_BODY = "com.raspi.chatapp.activities.MainActivity.MESSAGE_BODY";
  public static final String PRESENCE_CHANGED = "com.raspi.chatapp.activities.MainActivity.PRESENCE_CHANGED";
  public static final String PRESENCE_STATUS = "com.raspi.chatapp.activities.MainActivity.PRESENCE_STATUS";
  public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.activities.MainActivity.RECEIVE_MESSAGE";
  public static final String CONN_ESTABLISHED = "com.raspi.chatapp.activities.MainActivity.CONN_ESTABLISHED";
  public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

  //private MessageReceiver messageReceiver;
  private ChatArrayAdapter caa;
  private ListView lv;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    setUserPwd();

    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel
            (MyNotification.NOTIFICATION_ID);
    new MyNotification(this).reset();

    Intent callingIntent = getIntent();
    if (callingIntent != null && MyNotification.NOTIFICATION_CLICK.equals
            (callingIntent.getAction())){
      Log.d("DEBUG", "received intend not click");
      Bundle extras = callingIntent.getExtras();
      if (extras != null && extras.containsKey(BUDDY_ID) && extras.containsKey(CHAT_NAME)){
        Intent intent = new Intent(this, ChatActivity.class);
        String buddyId = extras.getString(BUDDY_ID);
        int index = buddyId.indexOf("@");
        if (index != -1)
          buddyId = buddyId.substring(0, index);
        intent.putExtra(BUDDY_ID, buddyId);
        intent.putExtra(CHAT_NAME, extras.getString(CHAT_NAME));
        startActivity(intent);
        Log.d("DEBUG", "starting chatActivity due to pendingIntent");
      }
    }
  }

  @Override
  protected void onResume(){
    super.onResume();
    initUI();
    LocalBroadcastManager.getInstance(this).registerReceiver
            (MessageReceiver, new IntentFilter(MainActivity.RECEIVE_MESSAGE));
    this.startService(new Intent(this, MessageService.class).setAction(APP_LAUNCHED));
    new MyNotification(this).reset();
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
            .cancel(MyNotification.NOTIFICATION_ID);
  }

  @Override
  protected void onPause(){
    LocalBroadcastManager.getInstance(this).unregisterReceiver
            (MessageReceiver);
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onPause();
  }

  @Override
  protected void onDestroy(){
    //signal the service that the app is about to get destroyed
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onDestroy();
  }

  private void initUI(){
    caa = new ChatArrayAdapter(this, R.layout.chat_list_entry);
    lv = (ListView) findViewById(R.id.main_listview);
    lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        ChatEntry chatEntry = caa.getItem(position);
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(BUDDY_ID, chatEntry.buddyId);
        intent.putExtra(CHAT_NAME, chatEntry.name);
        startActivity(intent);
      }
    });
    lv.setAdapter(caa);
    MessageHistory messageHistory = new MessageHistory(this);
    ChatEntry[] entries = messageHistory.getChats();
    for (ChatEntry entry : entries){
      if (entry != null){
        Log.d("DEBUG", "adding entry to view: " + entry);
        caa.add(entry);
      }else{
        Log.d("DEBUG", "a null entry");
      }
    }
    caa.registerDataSetObserver(new DataSetObserver(){
      @Override
      public void onChanged(){
        super.onChanged();
        lv.setSelection(caa.getCount() - 1);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings){
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onSettingsClick(MenuItem menuItem){
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
  }

  public void onAddChatClick(MenuItem menuItem){
    Intent intent = new Intent(this, AddChatActivity.class);
    startActivity(intent);
  }

  public void onDatabaseDebug(MenuItem menuItem){
    Intent intent = new Intent(this, AndroidDatabaseManager.class);
    startActivity(intent);
  }

  private void setUserPwd(){
    SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
    //if (!preferences.contains(USERNAME))
    preferences.edit().putString(USERNAME, "niklas").apply();

    //if (!preferences.contains(PASSWORD))
    preferences.edit().putString(PASSWORD, "passwNiklas").apply();
  }

  //receiving boot intents
  public static class BootReceiver extends BroadcastReceiver{

    public BootReceiver(){
    }

    @Override
    public void onReceive(Context context, Intent intent){
      if (intent != null && intent.getAction() != null){
        if (intent.getAction().equals(BOOT_COMPLETED)){
          context.startService(new Intent(context, MessageService.class));
        }
      }
    }
  }

  //receiving intents from the MessageService that a new message was received
  private BroadcastReceiver MessageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      initUI();
      new MyNotification(getApplicationContext()).reset();
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
              .cancel(MyNotification.NOTIFICATION_ID);
    }
  };
}
