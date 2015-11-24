package com.raspi.chatapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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

import com.raspi.chatapp.single_chat.ChatActivity;
import com.raspi.chatapp.single_chat.RosterArrayAdapter;

public class MainActivity extends AppCompatActivity{

    public static final String BUDDY_ID = "com.raspi.chatapp.BUDDY_ID";
    public static final String CHAT_NAME = "com.raspi.chatapp.CHAT_NAME";
    public static final String MESSAGE_BODY = "com.raspi.chatapp.MESSAGE_BODY";
    public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.RECEIVE_MESSAGE";
    public static final String SEND_MESSAGE = "com.raspi.chatapp.SEND_MESSAGE";
    public static final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    private static final String server = "raspi-server.mooo.com";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;
    private static final int NOTIFICATION_ID = 42;

    private XmppManager xmppManager;
    private MessageReceiver messageReceiver;
    private RosterArrayAdapter raa;

    //sending messages
    private BroadcastReceiver messageSendingReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent){
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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (xmppManager == null){
            xmppManager = new XmppManager(server, service, port, this);
            new initXMPP().execute("");
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(messageSendingReceiver, new IntentFilter(SEND_MESSAGE));

        if (messageReceiver == null){
            messageReceiver = new MessageReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(RECEIVE_MESSAGE));
        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);

        //UI:
        raa = new RosterArrayAdapter(this, R.layout.roster);
        ListView lv = (ListView) findViewById(R.id.main_listview);
        lv.setAdapter(raa);
        lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                RosterItem ri = raa.getItem(position);
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra(BUDDY_ID, ri.buddyId);
                intent.putExtra(CHAT_NAME, ri.name);
                startActivity(intent);
            }
        });

        //TODO retrieving roster entries and adding them to raa

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

    private String getUserName(){
        return "niklas";
    }

    private String getPassword(){
        return "passwNiklas";
    }

    private class initXMPP extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... params){
            if (xmppManager != null){
                int i = 0;
                while (i < 5 && !(xmppManager.init() && xmppManager.performLogin(getUserName(), getPassword())))
                    i++;
                if (i < 5){
                    Log.d("DEBUG", "Success: Connected");
                    //TODO reload roster and wait
                } else
                    Log.e("ERROR", "There was an error with the connection");
            }
            return null;
        }
    }

    //receiving system Intents
    public class SystemReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent){
            if (intent != null && intent.getAction() != null)
                if (intent.getAction().equals(BOOT_COMPLETED)){
                    //TODO do something on boot
                    xmppManager = new XmppManager(server, service, port, context);
                    new initXMPP().execute("");

                    messageReceiver = new MessageReceiver();
                    LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(RECEIVE_MESSAGE));

                } else if (intent.getAction().equals(CONNECTIVITY_CHANGE)){
                    NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                    if (info != null){
                        if (info.isConnected()){
                            //TODO do I need to do something here?
                        } else {

                        }
                    }
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

                        createNotification(buddyId, message);
                        //create Intent for LocalBroadcastListener in ChatActivity
                    }
                }
        }
    }

    private void createNotification(String buddyId, String message){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("New message from " + buddyId)
                .setContentText(message)
                .setStyle(new NotificationCompat.InboxStyle())
                .setAutoCancel(true)
                .setVibrate(new long[]{500, 300, 500, 300})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setLights(Color.BLUE, 500, 500)
                .setStyle(new NotificationCompat.InboxStyle());

        Intent resultIntent = new Intent(this, ChatActivity.class);
        resultIntent.putExtra(BUDDY_ID, buddyId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, mBuilder.build());
    }
}
