package com.raspi.chatapp.util.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.ui.chatting.SendImageFragment;
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
import org.jivesoftware.smackx.offline.OfflineMessageManager;

import java.util.Collection;
import java.util.Date;

public class MessageService extends Service{

  XmppManager xmppManager = null;
  MessageHistory messageHistory;

  private boolean isAppRunning = false;
  private RosterListener rosterListener = new RosterListener(){
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
  };

  private Runnable reloadRoster = new Runnable(){
    @Override
    public void run(){
      Roster roster = xmppManager.getRoster();
      if (roster != null && !roster.isLoaded())
        try{
          roster.reloadAndWait();
          Log.d("SERVICE_DEBUG", "reloaded roster");
        }catch (Exception e){
          Log.e("SERVICE_ERROR", "Couldn't load the roster");
          e.printStackTrace();
        }

      if (roster != null){
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries)
          presenceReceived(roster.getPresence(entry.getUser()));
        roster.addRosterListener(rosterListener);
      }
    }
  };

  @Override
  public void onCreate(){
    super.onCreate();
    Log.d("SERVICE_DEBUG", "MessageService created.");
    messageHistory = new MessageHistory(this);
    xmppManager = XmppManager.getInstance(getApplicationContext());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId){
    new InitAsyncTask().execute();
    if (intent != null)
      if (ChatActivity.APP_LAUNCHED.equals(intent.getAction())){
        try{
          xmppManager.getConnection().connect();
        }catch (Exception e){
        }
        isAppRunning = true;
      }else if (ChatActivity.APP_CLOSED.equals(intent.getAction())){
        isAppRunning = false;
      }

    return START_STICKY;
  }

  private void initialize(){
    try{
      //initialize xmpp:
      Log.d("SERVICE_DEBUG", "MessageService initializing");
      if (xmppManager == null)
        xmppManager = XmppManager.getInstance(getApplicationContext());
      if (!xmppManager.isConnected()){
        xmppManager.init();
        xmppManager.performLogin(getUserName(), getPassword());
        OfflineMessageManager offlineMessageManager = new
                OfflineMessageManager(xmppManager.getConnection());
        if (offlineMessageManager.supportsFlexibleRetrieval())
          processMessages(offlineMessageManager.getMessages().toArray(new
                  Message[offlineMessageManager.getMessageCount()]));
        new Thread(reloadRoster).run();
      }
      xmppManager.setStatus(true, isAppRunning ? "online" : Long.toString(new
              Date().getTime()));

      ChatManager.getInstanceFor(xmppManager.getConnection())
              .addChatListener(new MyChatManagerListener());
    }catch (Exception e){
      Log.e("SERVICE_ERROR", "An error while initializing the MessageService " +
              "occurred.");
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

  private void processMessages(Message... messages){
    for (Message message : messages){
      Log.d("SERVICE_DEBUG", "Received message and processing it.");
      Roster roster = xmppManager.getRoster();
      if (!roster.isLoaded())
        try{
          roster.reloadAndWait();
        }catch (Exception e){
          Log.e("SERVICE_ERROR", "An error occurred while reloading the roster");
        }
      String buddyId = message.getFrom();
      MessageXmlParser.Message msg = MessageXmlParser.parse(message.getBody());
      if (!"acknowledgement".equals(msg.type)){
        long id = msg.id;
        String name = roster.contains(buddyId)
                ? roster.getEntry(buddyId).getName()
                : buddyId;

        messageHistory.addChat(buddyId, buddyId);
        Intent msgIntent = new Intent(ChatActivity.RECEIVE_MESSAGE)
                .putExtra(ChatActivity.BUDDY_ID, buddyId)
                .putExtra(ChatActivity.CHAT_NAME, name);
        if (MessageHistory.TYPE_TEXT.equals(msg.type)){
          messageHistory.addMessage(buddyId, buddyId, MessageHistory
                  .TYPE_TEXT, msg.content, MessageHistory
                  .STATUS_RECEIVED);
          msgIntent.putExtra(ChatActivity.MESSAGE_BODY, msg.content);
        }else if (MessageHistory.TYPE_IMAGE.equals(msg.type)){
          try{
            MyFileUtils mfu = new MyFileUtils();
            messageHistory.addMessage(
                    buddyId,
                    buddyId,
                    msg.type,
                    SendImageFragment.createJSON(
                            mfu.getFileName().getAbsolutePath(),
                            msg.description).toString(),
                    "http://" + XmppManager.SERVER + "/ChatApp/" + msg.url,
                    "0",
                    MessageHistory.STATUS_WAITING);
          }catch (Exception e){
          }
        }
        msgIntent.putExtra("id", id);
        getApplicationContext().sendOrderedBroadcast(msgIntent, null);
      }else{
        messageHistory.updateMessageStatus(buddyId, msg.id, msg.content);
        Intent intent = new Intent(ChatActivity.MESSAGE_STATUS_CHANGED);
        intent.putExtra("id", msg.id);
        intent.putExtra("status", msg.content);
        intent.putExtra(ChatActivity.BUDDY_ID, buddyId);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(intent);
      }
    }
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
      long id = extras.getLong("id");
      new Notification(context).createNotification(buddyId, name, msg);
      //also send the received acknowledgement
      try{
        XmppManager.getInstance(context).sendAcknowledgement(buddyId, id,
                MessageHistory.STATUS_RECEIVED);
      }catch (Exception e){
      }
    }
  }


  private class MyChatMessageListener implements ChatMessageListener{

    @Override
    public void processMessage(Chat chat, Message message){
      processMessages(message);
    }
  }

  private class MyChatManagerListener implements ChatManagerListener{
    @Override
    public void chatCreated(Chat chat, boolean createdLocally){
      if (chat.getListeners().isEmpty())
        chat.addMessageListener(new MyChatMessageListener());
    }
  }

  private class InitAsyncTask extends AsyncTask<Void, Void, Void>{
    @Override
    protected Void doInBackground(Void... params){
      initialize();
      return null;
    }
  }

}
