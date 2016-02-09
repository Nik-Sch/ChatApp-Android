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
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MessageService extends Service{

  public static final String ACTION_SEND_TEXT = "sendTextMessage";
  public static final String ACTION_SEND_IMAGE = "sendImageMessage";
  public static final String ACTION_SEND_ACKNOWLEDGE = "sendAcknowledge";

  public static final String KEY_MESSAGE = "message";
  public static final String KEY_SERVER_FILE = "serverFile";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_ACKNOWLEDGE_TYPE = "ackType";
  public static final String KEY_BUDDYID = "buddyId";
  public static final String KEY_ID = "id";
  public static final String KEY_IS_FIRST = "isFirst";

  public static final String server = "raspi-server.ddns.net";
  private static final int packetReplyTime = 5000;
  private static final String service = "chatapp.com";
  private static final int port = 5222;
  private static LocalBroadcastManager LBMgr;
  MessageHistory messageHistory;
  private XMPPTCPConnection connection;
  private boolean isAppRunning = false;

  private RosterListener rosterListener = new RosterListener(){
    @Override
    public void entriesAdded(Collection<String> collection){

      Roster roster = getRoster();
      Collection<RosterEntry> entries = roster.getEntries();
      for (RosterEntry entry : entries)
        presenceReceived(roster.getPresence(entry.getUser()));
    }

    @Override
    public void entriesUpdated(Collection<String> collection){

      Roster roster = getRoster();
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
  private ConnectionListener connectionListener = new ConnectionListener(){
    @Override
    public void connected(XMPPConnection connection){
      Log.d("XMPP_MANAGER", "connected successfully");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed){
      if (resumed)
        Log.d("XMPP_MANAGER", "authenticated successfully a resumed " +
                "connection");
      else
        Log.d("XMPP_MANAGER", "authenticated successfully a not resumed " +
                "connection");

    }

    @Override
    public void connectionClosed(){
      Log.d("XMPP_MANAGER", "closed the connection successfully");
      LBMgr.sendBroadcast(new Intent(ChatActivity.DISCONNECTED));
    }

    @Override
    public void connectionClosedOnError(Exception e){
      Log.d("XMPP_MANAGER", "Connection closed on error");
      LBMgr.sendBroadcast(new Intent(ChatActivity.DISCONNECTED));
    }

    @Override
    public void reconnectionSuccessful(){
      Log.d("XMPP_MANAGER", "reconnected successfully");
      LBMgr.sendBroadcast(new Intent(ChatActivity.RECONNECTED));

    }

    @Override
    public void reconnectingIn(int seconds){

    }

    @Override
    public void reconnectionFailed(Exception e){
      Log.d("XMPP_MANAGER", "reconnecting failed");
    }
  };

  @Override
  public void onCreate(){
    super.onCreate();
    Log.d("SERVICE_DEBUG", "MessageService created.");
    messageHistory = new MessageHistory(this);
    LBMgr = LocalBroadcastManager.getInstance(getApplicationContext());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId){
    new InitAsyncTask().execute();
    if (intent != null){
      String action = intent.getAction();
      Bundle extras = intent.getExtras();
      switch (action){
        case ACTION_SEND_TEXT:
          if (extras != null && extras.containsKey(KEY_MESSAGE) && extras
                  .containsKey(KEY_BUDDYID) && extras.containsKey(KEY_ID)){
            String message = extras.getString(KEY_MESSAGE);
            String buddyJID = extras.getString(KEY_BUDDYID);
            long id = extras.getLong(KEY_ID);
            sendTextMessage(message, buddyJID, id);
          }
          break;
        case ACTION_SEND_IMAGE:
          if (extras != null && extras.containsKey(KEY_SERVER_FILE) && extras
                  .containsKey(KEY_DESCRIPTION) && extras.containsKey
                  (KEY_BUDDYID) && extras.containsKey(KEY_ID)){
            String serverFile = extras.getString(KEY_SERVER_FILE);
            String description = extras.getString(KEY_DESCRIPTION);
            String buddyJID = extras.getString(KEY_BUDDYID);
            long id = extras.getLong(KEY_ID);
            sendImageMessage(serverFile, description, buddyJID, id);
          }
          break;
        case ACTION_SEND_ACKNOWLEDGE:
          if (extras != null && extras.containsKey(KEY_BUDDYID) && extras
                  .containsKey(KEY_ID) && extras.containsKey(KEY_ACKNOWLEDGE_TYPE)){
            String buddyId = extras.getString(KEY_BUDDYID);
            long id = extras.getLong(KEY_ID);
            String ackType = extras.getString(KEY_ACKNOWLEDGE_TYPE);
            sendAcknowledgement(buddyId, id, ackType);
          }
          break;
        case ChatActivity.APP_LAUNCHED:
          isAppRunning = true;
          break;
        case ChatActivity.APP_CLOSED:
          isAppRunning = false;
          break;
      }
    }

    return START_STICKY;
  }

  /**
   * initializes the connection with the server
   */
  private void initXmpp(){
    if (connection == null){
      SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTime);
      ReconnectionManager.setEnabledPerDefault(true);
      ReconnectionManager.setDefaultReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);
      ReconnectionManager.setDefaultFixedDelay(5);

      XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
              .setServiceName(service)
              .setHost(server)
              .setPort(port)
              .setSendPresence(false)
              .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible).build();
      connection = new XMPPTCPConnection(config);
      connection.setUseStreamManagement(true);
      connection.addConnectionListener(connectionListener);
    }
    if (!isConnected()){
      try{
        connection.connect();
        connection.login(getUserName(), getPassword());
        new ReloadRosterTask().execute();
        Log.d("DEBUG", "Success: Logged in.");
        OfflineMessageManager offlineMessageManager = new
                OfflineMessageManager(connection);
        if (offlineMessageManager.supportsFlexibleRetrieval()){
          processMessages(offlineMessageManager.getMessages().toArray(new
                  Message[offlineMessageManager.getMessageCount()]));
        }
        setStatus(true, isAppRunning ? "online" : Long.toString(new Date()
                .getTime()));
        ChatManager.getInstanceFor(connection).addChatListener(new
                MyChatManagerListener());
      }catch (Exception e){
        Log.e("ERROR", "Couldn't log in.");
        e.printStackTrace();
        return;
      }
    }
    Log.d("DEBUG", "Success: Initialized XmppManager.");
  }

  /**
   * returns the roster for the current connection
   *
   * @return the roster and null if the roster cannot be accessed
   */
  private Roster getRoster(){
    if (connection != null && connection.isConnected()){
      Log.d("DEBUG", "Success: returning roster.");
      return Roster.getInstanceFor(connection);
    }else{
      Log.d("DEBUG", "Couldn't get the roster: No connection.");
      return null;
    }
  }

  /**
   * sends a text message
   *
   * @param message  the message text to send
   * @param buddyJID the Buddy to receive the message
   * @param id the id of the message to send
   */
  private void sendTextMessage(String message, String buddyJID, long id){
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      if (buddyJID.indexOf('@') == -1)
        buddyJID += "@" + service;
      Chat chat = chatManager.createChat(buddyJID);
      try{
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element msg = doc.createElement("message");
        doc.appendChild(msg);
        msg.setAttribute("type", MessageHistory.TYPE_TEXT);
        msg.setAttribute("id", String.valueOf(id));
        Element file = doc.createElement("content");
        msg.appendChild(file);
        file.setTextContent(message);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);

        message = writer.toString();
        chat.sendMessage(message);
        messageHistory.updateMessageStatus(buddyJID, id, MessageHistory
                .STATUS_SENT);
        updateUIMessageStatus(buddyJID, MessageHistory.STATUS_SENT, id);
        Log.d("DEBUG", "Success: Sent message");
      }catch (Exception e){
        Log.e("ERROR", "Couldn't send message.");
        e.printStackTrace();
      }
    }
  }

  /**
   * sends an image message
   *
   * @param serverFile  the file on the server
   * @param description the description of the sent image
   * @param buddyJID    the Buddy to receive the message
   */
  private void sendImageMessage(String serverFile, String description, String
          buddyJID, long id){
    if (buddyJID.indexOf('@') == -1)
      buddyJID += "@" + service;
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      Chat chat = chatManager.createChat(buddyJID);
      try{
        //generate the message in order to set the type to image

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element msg = doc.createElement("message");
        doc.appendChild(msg);
        msg.setAttribute("type", MessageHistory.TYPE_IMAGE);
        msg.setAttribute("id", String.valueOf(id));
        Element file = doc.createElement("file");
        msg.appendChild(file);
        file.setTextContent(serverFile);
        Element desc = doc.createElement("description");
        msg.appendChild(desc);
        desc.setTextContent(description);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);

        String message = writer.toString();

        chat.sendMessage(message);
        messageHistory.updateMessageStatus(buddyJID, id, MessageHistory
                .STATUS_SENT);
        Log.d("DEBUG", "Success: Sent message");
      }catch (Exception e){
        Log.e("ERROR", "Couldn't send message.");
        e.printStackTrace();
      }
    }
  }

  private boolean sendAcknowledgement(String buddyId, long id, String
          type){
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      if (buddyId.indexOf('@') == -1)
        buddyId += "@" + service;
      Chat chat = chatManager.createChat(buddyId);
      try{
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element ack = doc.createElement("acknowledgement");
        doc.appendChild(ack);
        ack.setAttribute("id", String.valueOf(id));
        ack.setAttribute("type", type);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);

        String message = writer.toString();
        chat.sendMessage(message);
        Log.d("DEBUG", "Success: Sent message");
        return true;
      }catch (Exception e){
        Log.e("ERROR", "Couldn't send message.");
        Log.e("ERROR", e.toString());
        return false;
      }
    }
    Log.e("ERROR", "Sending failed: No connection.");
    return false;
  }

  /**
   * sets the status
   *
   * @param available if true the status type will be set to available otherwise to unavailable
   * @param status    the status message
   * @return true if setting the status was successful
   */
  private boolean setStatus(boolean available, String status){
    if (connection != null && connection.isConnected()){
      Presence.Type type = available ? Presence.Type.available : Presence.Type.unavailable;
      Presence presence = new Presence(type);

      presence.setStatus(status);
      try{
        connection.sendStanza(presence);
        Log.d("DEBUG", "Success: Set status.");
        return true;
      }catch (Exception e){
        System.err.println(e.toString());
        Log.e("ERROR", "Error while setting status.");
        return false;
      }
    }
    Log.e("ERROR", "Setting status failed: No connection.");
    return false;
  }

  private boolean isConnected(){
    return connection != null && connection.isConnected();
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
      String buddyId = message.getFrom();
      MessageXmlParser.Message msg = MessageXmlParser.parse(message.getBody());
      if (!"acknowledgement".equals(msg.type)){
        long id = msg.id;
        String name = messageHistory.getName(buddyId);

        messageHistory.addChat(buddyId, buddyId);
        Intent msgIntent = new Intent(ChatActivity.RECEIVE_MESSAGE)
                .putExtra(ChatActivity.BUDDY_ID, buddyId)
                .putExtra(ChatActivity.CHAT_NAME, name);
        if (MessageHistory.TYPE_TEXT.equals(msg.type)){
          messageHistory.addMessage(buddyId, buddyId, MessageHistory
                  .TYPE_TEXT, msg.content, MessageHistory
                  .STATUS_RECEIVED, id);
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
                    "http://" + server + "/ChatApp/" + msg.url,
                    "0",
                    MessageHistory.STATUS_WAITING, id);
          }catch (Exception e){
          }
        }
        msgIntent.putExtra("id", id);
        getApplicationContext().sendOrderedBroadcast(msgIntent, null);
      }else{
        messageHistory.updateMessageStatus(buddyId, msg.id, msg.content);
        updateUIMessageStatus(buddyId, msg.content, msg.id);
      }
    }
  }

  private void updateUIMessageStatus(String buddyId, String status, long id){
    Intent intent = new Intent(ChatActivity.MESSAGE_STATUS_CHANGED);
    intent.putExtra("id", id);
    intent.putExtra("status", status);
    intent.putExtra(ChatActivity.BUDDY_ID, buddyId);
    LocalBroadcastManager.getInstance(getApplicationContext())
            .sendBroadcast(intent);
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
      Intent ackIntent = new Intent(context, MessageService.class);
      ackIntent.setAction(MessageService.ACTION_SEND_ACKNOWLEDGE);
      ackIntent.putExtra(MessageService.KEY_ACKNOWLEDGE_TYPE,
              MessageHistory.STATUS_RECEIVED);
      ackIntent.putExtra(MessageService.KEY_BUDDYID, buddyId);
      ackIntent.putExtra(MessageService.KEY_ID, id);
      context.startService(ackIntent);
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
      initXmpp();
      return null;
    }
  }

  private class ReloadRosterTask extends AsyncTask<Void, Void, Void>{
    @Override
    protected Void doInBackground(Void... params){
      Roster roster = getRoster();
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
      return null;
    }
  }
}