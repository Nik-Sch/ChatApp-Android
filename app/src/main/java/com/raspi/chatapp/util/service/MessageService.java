package com.raspi.chatapp.util.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.ui.chatting.SendImageFragment;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.util.Globals;
import com.raspi.chatapp.util.MessageXmlParser;
import com.raspi.chatapp.util.Notification;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;

import java.util.Collection;
import java.util.Date;

public class MessageService extends Service{

  private static final String server = "raspi-server.ddns.net";
  private static final String service = "chatapp.com";
  private static final int port = 5222;

  XmppManager xmppManager = null;
  MessageHistory messageHistory;
  private final UploadServiceBroadcastReceiver uploadReceiver =
          new UploadServiceBroadcastReceiver(){

            @Override
            public void onError(String uploadId, Exception exception){
              super.onError(uploadId, exception);
              Log.e("UPLOAD_DEBUG", "An error occured while uploading:" +
                      exception.toString());
              int index = uploadId.indexOf('|');
              String buddyId = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              messageHistory.updateMessageStatus(buddyId, Long.parseLong
                      (messageId), MessageHistory.STATUS_WAITING);
            }

            @Override
            public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage){
              int index = uploadId.indexOf('|');
              String buddyId = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              //apparently the message was already sent by another thread,
              // e.g. if we switched connection while the image was sending
              // and this service restarted
              MessageArrayContent m = messageHistory.getMessage(buddyId,
                      messageId);
              if (MessageHistory.STATUS_SENT.equals(((ImageMessage) m)
                      .status))
                return;
              if (!"invalid".equals(serverResponseMessage)){
                MessageArrayContent mac = messageHistory.getMessage(buddyId,
                        messageId);
                try{
                  String des = ((ImageMessage) mac).description;
                  if (xmppManager.sendImageMessage(serverResponseMessage, des,
                          buddyId))
                    messageHistory.updateMessageStatus(buddyId, Long
                            .parseLong(messageId), MessageHistory
                            .STATUS_SENT);
                }catch (ClassCastException e){
                  Log.e("UPLOAD", "Sending the uploaded image failed");
                  e.printStackTrace();
                }
              }else{
                messageHistory.updateMessageStatus(buddyId, Long.parseLong
                        (messageId), MessageHistory.STATUS_WAITING);
              }
            }
          };
  private boolean isAppRunning = false;

  @Override
  public void onCreate(){
    super.onCreate();
    Log.d("DEBUG", "MessageService created.");
    messageHistory = new MessageHistory(this);
    uploadReceiver.register(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId){
    if (xmppManager == null || !xmppManager.isConnected())
      new Thread(new Runnable(){
        @Override
        public void run(){
          reconnect();
          publicize();
        }
      }).start();
    Log.d("DEBUG", "MessageService launched.");
    if (intent == null){
      Log.d("DEBUG", "MessageService received a null intent.");
    }else if (ChatActivity.RECONNECT.equals(intent.getAction())){
      Log.d("DEBUG", "MessageService reconnect.");
      new Thread(new Runnable(){
        @Override
        public void run(){
          reconnect();
          publicize();
          LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent
                  (ChatActivity.CONN_ESTABLISHED));
        }
      }).start();
    }else if (ChatActivity.APP_LAUNCHED.equals(intent.getAction())){
      Log.d("DEBUG", "MessageService app created.");
      isAppRunning = true;
      while (xmppManager == null)
        try{
          Thread.sleep(10);
        }catch (Exception e){
        }
      xmppManager.setStatus(true, "online");
      publicize();
    }else if (ChatActivity.APP_CLOSED.equals(intent.getAction())){
      Log.d("DEBUG", "MessageService app destroyed.");
      isAppRunning = false;
      while (xmppManager == null)
        try{
          Thread.sleep(10);
        }catch (Exception e){
        }
      xmppManager.setStatus(true, Long.toString(new Date().getTime()));
    }else{
      Log.d("DEBUG", "MessageService received unknown intend.");
    }
    return START_STICKY;
  }

  private void reconnect(){
    Log.d("DEBUG", "MessageService reconnecting");
    ConnectivityManager connManager = (ConnectivityManager) getSystemService
            (Context.CONNECTIVITY_SERVICE);
    if (connManager != null && connManager.getActiveNetworkInfo() != null &&
            connManager.getActiveNetworkInfo().isConnected()){
      //I am connected
      if (xmppManager == null)
        initialize();
      else{
        xmppManager.reconnect();
        xmppManager.performLogin(getUserName(), getPassword());
      }
    }else{
      //I am disconnected
      //xmppManager.disconnect();
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
          }catch (Exception e){
            Log.e("ERROR", "Couldn't load the roster");
            e.printStackTrace();
          }

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries)
          presenceReceived(roster.getPresence(entry.getUser()));
        roster.addRosterListener(new RosterListener(){
          @Override
          public void entriesAdded(Collection<String> collection){

            Roster roster = xmppManager.getRoster();
            Collection<RosterEntry> entries = roster.getEntries();
            for (RosterEntry entry : entries)
              presenceReceived(roster.getPresence(entry.getUser()));
          }

          @Override
          public void entriesUpdated(Collection<String> collection){

            Roster roster = xmppManager.getRoster();
            Collection<RosterEntry> entries = roster.getEntries();
            for (RosterEntry entry : entries)
              presenceReceived(roster.getPresence(entry.getUser()));
          }

          @Override
          public void entriesDeleted(Collection<String> collection){
          }

          @Override
          public void presenceChanged(Presence presence){
            presenceReceived(presence);
          }
        });
      }else{
        Log.e("ERROR", "There was an error with the connection");
      }
      ChatManagerListener managerListener = new MyChatManagerListener();
      ChatManager.getInstanceFor(xmppManager.getConnection())
              .addChatListener(managerListener);
    }catch (Exception e){
      Log.e("ERROR", "An error while running the MessageService occurred.");
      e.printStackTrace();
    }
  }

  private void presenceReceived(Presence presence){
    if (Presence.Type.available.equals(presence.getType()) &&
            presence.getStatus() != null){
      String from = presence.getFrom();
      int index = from.indexOf('@');
      if (index >= 0){
        from = from.substring(0, index);
      }
      String status = presence.getStatus();
      Intent intent = new Intent(ChatActivity.PRESENCE_CHANGED);
      intent.putExtra(ChatActivity.BUDDY_ID, from);
      intent.putExtra(ChatActivity.PRESENCE_STATUS, status);
      LocalBroadcastManager.getInstance(getApplicationContext())
              .sendBroadcast(intent);
      messageHistory.setOnline(from, status);
    }
  }

  private void publicize(){
    Log.d("DEBUG", "MessageService publicizing");
    if (isAppRunning)
      ((Globals) getApplication()).setXmppManager(xmppManager);
  }

  private String getUserName(){
    return getSharedPreferences(ChatActivity.PREFERENCES, 0).getString(ChatActivity.USERNAME, "");
  }

  private String getPassword(){
    return getSharedPreferences(ChatActivity.PREFERENCES, 0).getString(ChatActivity.PASSWORD, "");
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

  public static class RaiseMessageNotification extends BroadcastReceiver{
    public RaiseMessageNotification(){
    }

    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      String buddyId = extras.getString(ChatActivity.BUDDY_ID);
      String name = extras.getString(ChatActivity.CHAT_NAME);
      String msg = extras.getString(ChatActivity.MESSAGE_BODY);
      new Notification(context).createNotification(buddyId, name, msg);
    }
  }

  private class MyChatMessageListener implements ChatMessageListener{
    @Override
    public void processMessage(Chat chat, Message message){
      Log.d("DEBUG", "Received message and processing it.");
      Roster roster = xmppManager.getRoster();
      if (!roster.isLoaded())
        try{
          roster.reloadAndWait();
        }catch (Exception e){
          Log.e("ERROR", "An error occurred while reloading the roster");
        }
      String buddyId = message.getFrom();
      MessageXmlParser.Message msg = MessageXmlParser.parse(message.getBody());
      String name = roster.contains(buddyId)
              ? roster.getEntry(buddyId).getName()
              : buddyId;

      messageHistory.addChat(buddyId, buddyId);
      Intent msgIntent = new Intent(ChatActivity.RECEIVE_MESSAGE)
              .putExtra(ChatActivity.BUDDY_ID, buddyId)
              .putExtra(ChatActivity.CHAT_NAME, name);
      if (MessageHistory.TYPE_TEXT.equals(msg.type)){
        messageHistory.addMessage(buddyId, buddyId, MessageHistory.TYPE_TEXT,
                msg.content, MessageHistory.STATUS_RECEIVED);
        msgIntent.putExtra(ChatActivity.MESSAGE_BODY, msg.content);
      }else if (MessageHistory.TYPE_IMAGE.equals(msg.type)){
        try{
          MyFileUtils mfu = new MyFileUtils();
          messageHistory.addMessage(
                  buddyId,
                  buddyId,
                  msg.type,
                  SendImageFragment.createJSON(mfu.getFileName()
                                  .getAbsolutePath(),
                          msg.description).toString(),
                  "http://" + server + "/ChatApp/" + msg.url,
                  "0",
                  MessageHistory.STATUS_WAITING);
        }catch (Exception e){
        }
      }
      getApplicationContext().sendOrderedBroadcast(msgIntent, null);
    }
  }

  private class MyChatManagerListener implements ChatManagerListener{
    @Override
    public void chatCreated(Chat chat, boolean b){
      chat.addMessageListener(new MyChatMessageListener());

    }
  }
}
