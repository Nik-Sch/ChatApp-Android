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
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MessageService extends Service{

  private static final String server = "raspi-server.ddns.net";
  private static final String service = "chatapp.com";
  private static final int port = 5222;

  private static final int packetReplyTime = 5000;
  MessageHistory messageHistory;
  private XMPPTCPConnection connection;
  private LocalBroadcastManager LBMgr = LocalBroadcastManager.getInstance
          (getApplicationContext());
  private boolean isAppRunning = false;


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
    initXMPP();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId){
    new InitAsyncTask().execute();
    if (intent != null)
      if (ChatActivity.APP_LAUNCHED.equals(intent.getAction())){
        isAppRunning = true;
      }else if (ChatActivity.APP_CLOSED.equals(intent.getAction())){
        isAppRunning = false;
      }

    return START_STICKY;
  }

  private void initXMPP(){
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
    if (!connection.isConnected()){
      try{
        connection.connect();
      }catch (Exception e){
        Log.e("ERROR", "Couldn't connect.");
        e.printStackTrace();
        return;
      }
      Log.d("DEBUG", "Success: Initialized XmppManager.");

      if (!connection.isAuthenticated()){
        //logging in
        try{
          connection.login(getUserName(), getPassword());
          Log.d("DEBUG", "Success: Logged in.");
        }catch (Exception e){
          Log.e("ERROR", "Couldn't log in.");
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * sends a text message
   *
   * @param message  the message text to send
   * @param buddyJID the Buddy to receive the message
   * @return true if sending was successful
   */
  private boolean sendTextMessage(String message, String buddyJID, long id){
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
   * sends a text message
   *
   * @param serverFile  the file on the server
   * @param description the description of the sent image
   * @param buddyJID    the Buddy to receive the message
   * @return true if sending was successful
   */
  private boolean sendImageMessage(String serverFile, String description, String
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

  private boolean sendAcknowledgement(String buddyId, long id, String type){
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
      LBMgr.sendBroadcast(intent);
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
                    "http://" + server + "/ChatApp/" + msg.url,
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
    Log.d("SERVICE_DEBUG", "disconnecting xmpp");
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
