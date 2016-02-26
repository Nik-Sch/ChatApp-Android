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

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.chatting.SendImageFragment;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.ui.util.message_array.TextMessage;
import com.raspi.chatapp.util.Constants;
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

public class MessageService extends Service{

  private static final String recUnav = "recipient-unavailable:";

  XmppManager xmppManager = null;
  MessageHistory messageHistory;

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
    try{
      xmppManager.getConnection().connect();
    }catch (Exception e){
      e.printStackTrace();
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
        new Thread(reloadRoster).start();
      }

      ChatManager chatManager = ChatManager.getInstanceFor(xmppManager
              .getConnection());
      chatManager.addChatListener(new MyChatManagerListener());
      xmppManager.setStatus(true, String.valueOf(getSharedPreferences(Constants
              .PREFERENCES, 0).getLong(Constants.LAST_PRESENCE_SENT, 0)));
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
      Intent intent = new Intent(Constants.PRESENCE_CHANGED);
      intent.putExtra(Constants.BUDDY_ID, from);
      intent.putExtra(Constants.PRESENCE_STATUS, status);
      LocalBroadcastManager.getInstance(getApplicationContext())
              .sendBroadcast(intent);
      messageHistory.setOnline(from, status);
    }
  }

  private void processMessages(Message... messages){
    for (Message message : messages){
      Log.d("SERVICE_DEBUG", "Received message and processing it.");
      String buddyId = message.getFrom();
      String body = message.getBody();
      //my server is not sending an error on recipient-unavailable but a
      // message that starts with recUnav followed by the body that was sent
      if (body.startsWith(recUnav)){
        xmppManager.sendRaw(body.substring(recUnav.length() + 1), buddyId);
        Log.d("StreamManagement", "resending a message");
        return;
      }
      MessageXmlParser.Message msg = MessageXmlParser.parse(message.getBody());
      if (!"acknowledgement".equals(msg.type)){
        long id = msg.id;
        String name = messageHistory.getName(buddyId);

        messageHistory.addChat(buddyId, buddyId);
        Intent msgIntent = new Intent(Constants.MESSAGE_RECEIVED)
                .putExtra(Constants.BUDDY_ID, buddyId)
                .putExtra(Constants.CHAT_NAME, name);
        if (MessageHistory.TYPE_TEXT.equals(msg.type)){
          messageHistory.addMessage(buddyId, buddyId, MessageHistory
                  .TYPE_TEXT, msg.content, MessageHistory
                  .STATUS_RECEIVED, id);
          msgIntent.putExtra(Constants.MESSAGE_BODY, msg.content);
          msgIntent.putExtra(Constants.MESSAGE_TYPE, MessageHistory
                  .TYPE_TEXT);
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
                    MessageHistory.STATUS_WAITING, id);
            msgIntent.putExtra(Constants.MESSAGE_TYPE, MessageHistory
                    .TYPE_IMAGE);
            msgIntent.putExtra(Constants.MESSAGE_BODY, msg.description
                    .isEmpty()?getResources().getString(R.string.image):msg
                    .description);
          }catch (Exception e){
            e.printStackTrace();
          }
        }
        msgIntent.putExtra("id", id);
        getApplicationContext().sendOrderedBroadcast(msgIntent, null);
      }else{
        MessageArrayContent msgToUpdate = messageHistory.getMessage(buddyId,
                String.valueOf(msg.id));
        String oldStatus = msgToUpdate instanceof TextMessage?((TextMessage)
                msgToUpdate).status : ((ImageMessage)msgToUpdate).status;
        if (!MessageHistory.STATUS_READ.equals(oldStatus)){
          messageHistory.updateMessageStatus(buddyId, msg.id, msg.content);
          Intent intent = new Intent(Constants.MESSAGE_STATUS_CHANGED);
          intent.putExtra("id", msg.id);
          intent.putExtra("status", msg.content);
          intent.putExtra(Constants.BUDDY_ID, buddyId);
          LocalBroadcastManager.getInstance(getApplicationContext())
                  .sendBroadcast(intent);
        }
      }
    }
  }

  private String getUserName(){
    return getSharedPreferences(Constants.PREFERENCES, 0).getString(Constants.USERNAME, "");
  }

  private String getPassword(){
    return getSharedPreferences(Constants.PREFERENCES, 0).getString(Constants.PASSWORD, "");
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
      String buddyId = extras.getString(Constants.BUDDY_ID);
      String name = extras.getString(Constants.CHAT_NAME);
      String msg = extras.getString(Constants.MESSAGE_BODY);
      String type = extras.getString(Constants.MESSAGE_TYPE);
      long id = extras.getLong("id");
      new Notification(context).createNotification(buddyId, name, msg, type);
      //also send the received acknowledgement
      try{
        new MessageHistory(context).updateMessageStatus(buddyId, id,
                MessageHistory.STATUS_RECEIVED);
        XmppManager.getInstance().sendAcknowledgement(buddyId, id,
                MessageHistory.STATUS_RECEIVED);
      }catch (Exception e){
        e.printStackTrace();
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
      Log.d("CHAT MANAGER", "created a new chat");
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
