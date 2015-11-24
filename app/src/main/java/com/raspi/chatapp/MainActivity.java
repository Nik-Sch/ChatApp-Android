package com.raspi.chatapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
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

import com.raspi.chatapp.service.MessageService;
import com.raspi.chatapp.single_chat.ChatActivity;
import com.raspi.chatapp.single_chat.RosterArrayAdapter;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

public class MainActivity extends AppCompatActivity{

    public static final String BUDDY_ID = "com.raspi.chatapp.BUDDY_ID";
    public static final String CHAT_NAME = "com.raspi.chatapp.CHAT_NAME";
    public static final String MESSAGE_BODY = "com.raspi.chatapp.MESSAGE_BODY";
    public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.RECEIVE_MESSAGE";
    public static final String SEND_MESSAGE = "com.raspi.chatapp.SEND_MESSAGE";
    public static final String CONN_ESTABLISHED = "com.raspi.chatapp.CONN_ESTABLISHED";
    public static final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final int NOTIFICATION_ID = 42;
    private MessageReceiver messageReceiver;
    private RosterArrayAdapter raa;
    private ListView lv;

    //sending messages
    private BroadcastReceiver messageSendingReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent){
            XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
            Log.d("DEBUG", "Received any Intent");
            if (intent != null && intent.getAction() != null)
                if (intent.getAction().equals(SEND_MESSAGE) && intent.getExtras() != null){
                    Bundle extras = intent.getExtras();
                    if (xmppManager != null && xmppManager.isConnected() && extras.containsKey(BUDDY_ID) && extras.containsKey(MESSAGE_BODY)){
                        xmppManager.sendMessage(extras.getString(MESSAGE_BODY), extras.getString(BUDDY_ID));
                        Log.d("DEBUG", "Success: Sent message");
                    } else
                        Log.e("ERROR", "There was an error with the connection while sending a message.");
                } else
                    Log.e("ERROR", "There was an error with the Intent while sending a message.");
        }
    };

    private BroadcastReceiver onConnectionEstablished = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
            Log.d("DEBUG", "this is after the connection is established");
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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        XmppManager xmppManager = ((Globals) this.getApplication()).getXmppManager();

        //UI:
        raa = new RosterArrayAdapter(this, R.layout.roster);
        lv = (ListView) findViewById(R.id.main_listview);
        lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                RosterEntry ri = raa.getItem(position);
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra(BUDDY_ID, ri.getUser());
                intent.putExtra(CHAT_NAME, ri.getName());
                startActivity(intent);
            }
        });
        lv.setAdapter(raa);
        raa.registerDataSetObserver(new DataSetObserver(){
            @Override
            public void onChanged(){
                super.onChanged();
                lv.setSelection(raa.getCount() - 1);
            }
        });

        if (xmppManager == null){
            Log.d("DEBUG", "xmppManager is null");
            this.startService(new Intent(this, MessageService.class));
        }
        ((Globals) this.getApplication()).setXmppManager(xmppManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageSendingReceiver, new IntentFilter(SEND_MESSAGE));

        if (messageReceiver == null){
            messageReceiver = new MessageReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(RECEIVE_MESSAGE));
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(onConnectionEstablished, new
                IntentFilter(CONN_ESTABLISHED));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }

    @Override
    protected void onDestroy(){

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageSendingReceiver);
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

    //receiving system Intents
    public class SystemReceiver extends BroadcastReceiver{

        public SystemReceiver(){
        }

        @Override
        public void onReceive(Context context, Intent intent){
            XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
            if (intent != null && intent.getAction() != null){
                if (intent.getAction().equals(BOOT_COMPLETED)){
                    context.startService(new Intent(context, MessageService.class));

                    messageReceiver = new MessageReceiver();
                    LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(RECEIVE_MESSAGE));

                } else if (intent.getAction().equals(CONNECTIVITY_CHANGE)){
                    NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context
                            .CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                    if (info != null){
                        if (info.isConnected()){
                            context.startService(new Intent(context, MessageService.class));
                        } else {
                            context.stopService(new Intent(context, MessageService.class));
                        }
                    }
                }
                ((Globals) getApplication()).setXmppManager(xmppManager);
            }
        }
    }

    //receiving messages
    private class MessageReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent){
            if (intent != null && intent.getAction() != null)
                if (intent.getAction().equals(RECEIVE_MESSAGE)){
                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.containsKey(BUDDY_ID) && extras.containsKey(MESSAGE_BODY)){
                        Log.d("DEBUG", "Success: Received Message");
                        String buddyId = extras.getString(BUDDY_ID);
                        String message = extras.getString(MESSAGE_BODY);
                        //create Intent for LocalBroadcastListener in ChatActivity
                    }
                }
        }
    }
}
