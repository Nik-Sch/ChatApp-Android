package com.raspi.chatapp.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
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

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("DEBUG", "MessageService: Launched.");
        //receiving messages in a new thread
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Log.d("ConnectionChangeReceive", "started Service");
                    //initialize xmpp:
                    XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
                    if (xmppManager == null || !xmppManager.isConnected()){
                        Log.d("ConnectionChangeReceive", "xmpp is not connected or null");
                        xmppManager = new XmppManager(server,
                                service, port,
                                getApplication());
                        int i = 0;
                        while (i < 5 && !(xmppManager.init() && xmppManager.performLogin(getUserName(), getPassword()))){
                            Log.d("ConnectionChangeReceive", i + "'s try to connect");
                            i++;
                        }
                        ((Globals) getApplication()).setXmppManager(xmppManager);
                        if (i < 5){
                            Log.d("ConnectionChangeReceive", "connected");
                            Log.d("DEBUG", "Success: Connected.");
                            Roster roster = xmppManager.getRoster();
                            if (roster != null && !roster.isLoaded())
                                try{
                                    roster.reloadAndWait();
                                    Log.d("ConnectionChangeReceive", "reloaded the roster");
                                } catch (Exception e){
                                    Log.e("ERROR", "Couldn't load the roster");
                                    e.printStackTrace();
                                }
                            Log.d("DEBUG", "Success: Loaded roster.");
                        } else {
                            Log.e("ERROR", "There was an error with the connection");
                        }

                        ((Globals) getApplication()).setXmppManager(xmppManager);

                        ChatManagerListener managerListener = new MyChatManagerListener();
                        ChatManager.getInstanceFor(xmppManager.getConnection())
                                .addChatListener(managerListener);

                    }
                } catch (Exception e){
                    Log.e("ERROR", "An error while running the MessageService occurred.");
                    e.printStackTrace();
                }
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent
                        (MainActivity.CONN_ESTABLISHED));
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
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

    private String getUserName(){
        return "niklas";
    }

    private String getPassword(){
        return "passwNiklas";
    }

    private void createNotification(String buddyId, String name, String message){
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

    private class MyChatMessageListener implements ChatMessageListener{
        @Override
        public void processMessage(Chat chat, Message message){
            XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();
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
            Log.d("DEBUG", "Received message and created Notification/Intent");
        }
    }

    private class MyChatManagerListener implements ChatManagerListener{
        @Override
        public void chatCreated(Chat chat, boolean b){
            chat.addMessageListener(new MyChatMessageListener());

        }
    }
}
