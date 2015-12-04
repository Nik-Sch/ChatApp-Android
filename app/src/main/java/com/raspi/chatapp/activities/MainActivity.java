package com.raspi.chatapp.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.service.MessageService;
import com.raspi.chatapp.sqlite.MessageHistory;
import com.raspi.chatapp.ui_util.ChatArrayAdapter;
import com.raspi.chatapp.ui_util.ChatEntry;
import com.raspi.chatapp.util.MyNotification;

public class MainActivity extends AppCompatActivity{

    public static final String PREFERENCES = "com.raspi.chatapp.activities.MainActivity.PREFERENCES";
    public static final String USERNAME = "com.raspi.chatapp.activities.MainActivity.USERNAME";
    public static final String PASSWORD = "com.raspi.chatapp.activities.MainActivity.PASSWORD";
    public static final String CONN_TIMEOUT = "com.raspi.chatapp.activities.MainActivity.CONN_TIMEOUT";
    public static final String RECONNECT = "com.raspi.chatapp.activities.MainActivity.RECONNECT";
    public static final String APP_CREATED = "con.raspi.chatapp.MainActivity.APP_CREATED";
    public static final String APP_DESTROYED = "con.raspi.chatapp.MainActivity.APP_DESTROYED";
    public static final String BUDDY_ID = "com.raspi.chatapp.activities.MainActivity.BUDDY_ID";
    public static final String CHAT_NAME = "com.raspi.chatapp.activities.MainActivity.CHAT_NAME";
    public static final String MESSAGE_BODY = "com.raspi.chatapp.activities.MainActivity.MESSAGE_BODY";
    public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.activities.MainActivity.RECEIVE_MESSAGE";
    public static final String CONN_ESTABLISHED = "com.raspi.chatapp.activities.MainActivity.CONN_ESTABLISHED";
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    //private MessageReceiver messageReceiver;
    private ChatArrayAdapter caa;
    private ListView lv;

/*
    private BroadcastReceiver onConnectionEstablished = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
            if (xmppManager != null){
                Roster roster = xmppManager.getRoster();
                if (roster != null){
                    for (RosterEntry re : roster.getEntries())
                        raa.add(re);
                } else
                    Log.e("ERROR", "There was an error while receiving the roster");
            } else
                Log.e("ERROR", "There was an error while receiving the roster");
        }
    };
*/

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Intent CallingIntent = getIntent();
        if (CallingIntent != null && CallingIntent.getAction().equals(MyNotification
                .NOTIFICATION_CLICK)){
            Bundle extras = CallingIntent.getExtras();
            if (extras != null && extras.containsKey(BUDDY_ID) && extras.containsKey(CHAT_NAME)){
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(BUDDY_ID, extras.getString(BUDDY_ID));
                intent.putExtra(CHAT_NAME, extras.getString(CHAT_NAME));
                startActivity(intent);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setUserPwd();

        initUI();
//        LocalBroadcastManager.getInstance(this).registerReceiver(onConnectionEstablished, new
//                IntentFilter(CONN_ESTABLISHED));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel
                (MyNotification.NOTIFICATION_ID);
        new MyNotification(this).reset();

        //signal the service that the app is running
        this.startService(new Intent(this, MessageService.class).setAction(APP_CREATED));
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
        for (ChatEntry entry : entries)
            caa.add(entry);
        caa.registerDataSetObserver(new DataSetObserver(){
            @Override
            public void onChanged(){
                super.onChanged();
                lv.setSelection(caa.getCount() - 1);
            }
        });
    }

    @Override
    protected void onDestroy(){
        //signal the service that the app is about to get destroyed
        this.startService(new Intent(this, MessageService.class).setAction(APP_DESTROYED));
        super.onDestroy();
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
}
