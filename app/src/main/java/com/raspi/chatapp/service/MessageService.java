package com.raspi.chatapp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.raspi.chatapp.sqlite.MessageHistory;
import com.raspi.chatapp.util.Globals;
import com.raspi.chatapp.util.MyNotification;
import com.raspi.chatapp.util.XmppManager;
import com.raspi.chatapp.activities.MainActivity;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;

public class MessageService extends Service{

    private static final String server = "raspi-server.ddns.net";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;

    XmppManager xmppManager = null;
    MessageHistory messageHistory;
    private boolean isAppRunning = false;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("DEBUG", "MessageService created.");
        messageHistory = new MessageHistory(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("DEBUG", "MessageService launched.");
        if (intent == null){
            Log.d("DEBUG", "MessageService received a null intent.");
            new Thread(new Runnable(){
                @Override
                public void run(){
                    initialize();
                    publicize();
                    LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent
                            (MainActivity.CONN_ESTABLISHED));
                }
            }).start();
        } else if (MainActivity.RECONNECT.equals(intent.getAction())){
            Log.d("DEBUG", "MessageService reconnect.");
            new Thread(new Runnable(){
                @Override
                public void run(){
                    reconnect();
                    publicize();
                    LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent
                            (MainActivity.CONN_ESTABLISHED));
                }
            }).start();
        } else if (MainActivity.APP_CREATED.equals(intent.getAction())){
            Log.d("DEBUG", "MessageService app created.");
            isAppRunning = true;
            new Thread(new Runnable(){
                @Override
                public void run(){
                    initialize();
                    publicize();
                    LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent
                            (MainActivity.CONN_ESTABLISHED));
                }
            }).start();
        } else if (MainActivity.APP_DESTROYED.equals(intent.getAction())){
            Log.d("DEBUG", "MessageService app destroyed.");
            isAppRunning = false;
            onStartCommand(null, flags, startId);
        } else {
            Log.d("DEBUG", "MessageService received unknown intend.");
        }
        return START_STICKY;
    }

    private void reconnect(){
        Log.d("DEBUG", "MessageService reconnecting");
        if (((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo().isConnected()){
            //I am connected
            if (xmppManager == null)
                initialize();
            else {
                xmppManager.reconnect();
                xmppManager.performLogin(getUserName(), getPassword());
            }
        } else {
            //I am disconnected
            xmppManager.disconnect();
        }
    }

    private void initialize(){
        try{
            //initialize xmpp:
            Log.d("DEBUG", "MessageService initializing");
            xmppManager = new XmppManager(server,
                    service, port,
                    getApplication());
            if (xmppManager.init() && xmppManager.performLogin(getUserName(),
                    getPassword())){
                Log.d("DEBUG", "Success: Connected.");
                Roster roster = xmppManager.getRoster();
                if (roster != null && !roster.isLoaded())
                    try{
                        roster.reloadAndWait();
                        Log.d("ConnectionChangeReceive", "reloaded roster");
                    } catch (Exception e){
                        Log.e("ERROR", "Couldn't load the roster");
                        e.printStackTrace();
                    }
            } else {
                Log.e("ERROR", "There was an error with the connection");
            }

            ChatManagerListener managerListener = new MyChatManagerListener();
            ChatManager.getInstanceFor(xmppManager.getConnection())
                    .addChatListener(managerListener);
        } catch (Exception e){
            Log.e("ERROR", "An error while running the MessageService occurred.");
            e.printStackTrace();
        }
    }

    private void publicize(){
        Log.d("DEBUG", "MessageService publicizing");
        if (isAppRunning)
            ((Globals) getApplication()).setXmppManager(xmppManager);
    }

    private String getUserName(){
        return getSharedPreferences(MainActivity.PREFERENCES, 0).getString(MainActivity.USERNAME, "");
    }

    private String getPassword(){
        return getSharedPreferences(MainActivity.PREFERENCES, 0).getString(MainActivity.PASSWORD, "");
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onDestroy(){
        Log.d("DEBUG", "disconnecting xmpp");
        Log.d("ConnectionChangeReceive", "Stopped service");
        ((Globals) getApplication()).getXmppManager().disconnect();
        super.onDestroy();
    }

    private class MyChatMessageListener implements ChatMessageListener{
        @Override
        public void processMessage(Chat chat, Message message){
            Log.d("DEBUG", "Received message and processing it.");
            Roster roster = xmppManager.getRoster();
            if (!roster.isLoaded())
                try{
                    roster.reloadAndWait();
                } catch (Exception e){
                    Log.e("ERROR", "An error occurred while reloading the roster");
                }
            String buddyId = message.getFrom();
            String msg = message.getBody();
            String name = roster.contains(buddyId) ? roster.getEntry(buddyId).getName() : buddyId;
            new MyNotification(getApplicationContext()).createNotification(buddyId, name, message.getBody());

            messageHistory.addChat(buddyId, buddyId);
            messageHistory.addMessage(buddyId, buddyId, MessageHistory.TYPE_TEXT, msg,
                    MessageHistory.STATUS_WAITING);

            Intent msgIntent = new Intent(MainActivity.RECEIVE_MESSAGE)
                    .putExtra(MainActivity.BUDDY_ID, buddyId)
                    .putExtra(MainActivity.MESSAGE_BODY, msg);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgIntent);
        }
    }

    private class MyChatManagerListener implements ChatManagerListener{
        @Override
        public void chatCreated(Chat chat, boolean b){
            chat.addMessageListener(new MyChatMessageListener());

        }
    }
}
