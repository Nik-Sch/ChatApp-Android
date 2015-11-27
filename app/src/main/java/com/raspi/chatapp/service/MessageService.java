package com.raspi.chatapp.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.raspi.chatapp.Globals;
import com.raspi.chatapp.MainActivity;
import com.raspi.chatapp.R;
import com.raspi.chatapp.XmppManager;
import com.raspi.chatapp.single_chat.ChatActivity;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;

public class MessageService extends Service{

    private static final String server = "raspi-server.mooo.com";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;
    XmppManager xmppManager = null;
    private boolean isAppRunning = false;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("DEBUG", "MessageService created.");
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
            if (xmppManager == null)
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
        return "niklas";
    }

    private String getPassword(){
        return "passwNiklas";
    }

    private void createNotification(String buddyId, String name, String message){
        Log.d("DEBUG", "creating notification");
        Intent resultIntent = new Intent(this, ChatActivity.class);
        resultIntent.putExtra(MainActivity.BUDDY_ID, buddyId);
        resultIntent.putExtra(MainActivity.CHAT_NAME, name);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("New message from " + buddyId)
                .setContentText(message)
                .setStyle(new NotificationCompat.InboxStyle())
                .setAutoCancel(true)
                .setVibrate(new long[]{500, 300, 500, 300})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setLights(Color.BLUE, 500, 500)
                .setStyle(new NotificationCompat.InboxStyle())
                .setContentIntent(resultPendingIntent);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify
                (MainActivity.NOTIFICATION_ID, mBuilder.build());
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

            Intent msgIntent = new Intent(MainActivity.RECEIVE_MESSAGE)
                    .putExtra(MainActivity.BUDDY_ID, buddyId)
                    .putExtra(MainActivity.MESSAGE_BODY, msg);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgIntent);
            createNotification(buddyId, name, message.getBody());
        }
    }

    private class MyChatManagerListener implements ChatManagerListener{
        @Override
        public void chatCreated(Chat chat, boolean b){
            chat.addMessageListener(new MyChatMessageListener());

        }
    }
}
