package com.raspi.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.raspi.chatapp.single_chat.ChatActivity;

public class MainActivity extends AppCompatActivity{

    public static final String BUDDY_ID = "com.raspi.chatapp.BUDDY_ID";
    public static final String MESSAGE_BODY = "com.raspi.chatapp.MESSAGE_BODY";
    public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.RECEIVE_MESSAGE";
    public static final String SEND_MESSAGE = "com.raspi.chatapp.SEND_MESSAGE";
    public static final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    private static final String server = "raspi-server.mooo.com";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;
    private XmppManager xmppManager;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        xmppManager = new XmppManager(server, service, port, this);
        new initXMPP().execute("");

        LocalBroadcastManager.getInstance(this).registerReceiver(messageSendingReceiver, new IntentFilter(SEND_MESSAGE));
    }

    @Override
    protected void onDestroy(){

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageSendingReceiver);
        super.onDestroy();
    }

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

    public void openChat(View view){
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(BUDDY_ID, "aylin@raspi-server.mooo.com");
        startActivity(intent);
    }

    private String getUserName(){
        return "niklas";
    }

    private String getPassword(){
        return "passwNiklas";
    }

    public class SystemReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent){
            if (intent.getAction() != null)
                if (intent.getAction().equals(BOOT_COMPLETED)){
                    //TODO do something on boot
                    xmppManager = new XmppManager(server, service, port, context);
                    new initXMPP().execute("");
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

    private class initXMPP extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... params){
            if (xmppManager != null){
                int i = 0;
                while (i < 5 && !(xmppManager.init() && xmppManager.performLogin(getUserName(), getPassword())))
                    i++;
                if (i < 5)
                    Log.d("DEBUG", "Success: Connected");
                else
                    Log.e("ERROR", "There was an error with the connection");
            }
            return null;
        }
    }
}
