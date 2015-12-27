package com.raspi.chatapp.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.raspi.chatapp.sqlite.MessageHistory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

public class XmppManager{

  public static final int STATUS_ERROR = 0;
  public static final int STATUS_SENT = 1;
  public static final int STATUS_SENDING = 2;

  private static final int packetReplyTime = 5000;

  private String server;
  private String service;
  private int port;

  private XMPPTCPConnection connection;
  private MessageHistory messageHistory;

  /**
   * creates a IM Manager with the given server ID
   *
   * @param server  host address
   * @param service service name
   * @param context the IntentService which sends and receives the messages
   */
  public XmppManager(String server, String service, int port, Context context){
    this.server = server;
    this.service = service;
    this.port = port;
    messageHistory = new MessageHistory(context);
    Log.d("DEBUG", "Success: created xmppManager");
  }

  /**
   * initializes the connection with the server
   *
   * @return true if a connection could be established
   */
  public boolean init(){
    SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTime);

    XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setServiceName(service)
            .setHost(server)
            .setPort(port)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible).build();
    connection = new XMPPTCPConnection(config);
    connection.setUseStreamManagement(true);
    try{
      connection.connect();
    }catch (Exception e){
      Log.e("ERROR", "Couldn't connect.");
      Log.e("ERROR", e.toString());
      return false;
    }
    Log.d("DEBUG", "Success: Initialized XmppManager.");
    return true;
  }

  /**
   * logs in to the server
   *
   * @param username the username to use
   * @param password the corresponding password
   * @return true if the login was successful
   */
  public boolean performLogin(String username, String password){
    if (connection != null && connection.isConnected())
      try{
        connection.login(username, password);
        Log.d("DEBUG", "Success: Logged in.");
        return true;
      }catch (Exception e){
        Log.e("ERROR", "Couldn't log in.");
        Log.e("ERROR", e.toString());
        return false;
      }
    Log.d("DEBUG", "Couldn't log in: No connection.");
    return false;
  }

  /**
   * returns the roster for the current connection
   *
   * @return the roster and null if the roster cannot be accessed
   */
  public Roster getRoster(){
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
   * @return true if sending was successful
   */
  public boolean sendMessage(String message, String buddyJID){
    if (buddyJID.indexOf('@') == -1)
      buddyJID += "@" + service;
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      Chat chat = chatManager.createChat(buddyJID);
      try{
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
   * uploads the image that is specified by the given task
   * @param task the uploadTask that should be executed
   */
  public void sendImage(final UploadTask task, final Handler mHandler){
    new Thread(new Runnable(){
      @Override
      public void run(){
        //reporting to the UI Thread
        FileTransferManager manager = FileTransferManager.getInstanceFor
                (connection);
        String chatId = task.chatId;
        long messageID = task.messageID;
        messageHistory = task.messageHistory;
        messageHistory.updateMessageStatus(chatId, messageID, MessageHistory.STATUS_SENDING);
        try{
        OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer
                (task.chatId + "@" + service);
          transfer.sendFile(task.file, task.description);
        while (!transfer.isDone()){
          if (transfer.getStatus().equals(FileTransfer.Status.error)){
            //connection lost?
            if (mHandler != null){
              Message message = mHandler.obtainMessage(XmppManager
                      .STATUS_ERROR, new UploadHandlerTask(transfer
                      .getProgress(), task.chatId, task.messageID));
              message.sendToTarget();
            }
            messageHistory.updateMessageStatus(task.chatId, task.messageID,
                    MessageHistory.STATUS_WAITING);
            messageHistory.updateMessageProgress(task.chatId, task.messageID,
                    0);
          }else if (FileTransfer.Status.cancelled.equals(transfer.getStatus())){
            //user interruption
            if (mHandler != null){
              Message message = mHandler.obtainMessage(XmppManager
                      .STATUS_ERROR, new UploadHandlerTask(transfer
                      .getProgress(), task.chatId, task.messageID));
              message.sendToTarget();
            }
            messageHistory.updateMessageStatus(task.chatId, task.messageID,
                    MessageHistory.STATUS_WAITING);
            messageHistory.updateMessageProgress(task.chatId, task.messageID,
                    0);
          }else if (FileTransfer.Status.refused.equals(transfer.getStatus())){
            //buddy doesn't want the file
            if (mHandler != null){
              Message message = mHandler.obtainMessage(XmppManager
                      .STATUS_ERROR, new UploadHandlerTask(transfer
                      .getProgress(), task.chatId, task.messageID));
              message.sendToTarget();
            }
            messageHistory.updateMessageStatus(task.chatId, task.messageID,
                    MessageHistory.STATUS_WAITING);
            messageHistory.updateMessageProgress(task.chatId, task.messageID,
                    0);
          }
          //report progress
          if (mHandler != null){
            Message message = mHandler.obtainMessage(XmppManager
                    .STATUS_SENDING, new UploadHandlerTask(transfer
                    .getProgress(), task.chatId, task.messageID));
            message.sendToTarget();
          }else{
            messageHistory.updateMessageProgress(task.chatId, task.messageID,
                    transfer.getProgress());
          }
          try{
            Thread.sleep(100);
          }catch (InterruptedException e){
            e.printStackTrace();
          }
        }
        if (mHandler != null){
          Message message = mHandler.obtainMessage(XmppManager
                  .STATUS_SENT, new UploadHandlerTask(transfer
                  .getProgress(), task.chatId, task.messageID));
          message.sendToTarget();
        }
        messageHistory.updateMessageProgress(task.chatId, task.messageID, 1);
        messageHistory.updateMessageStatus(task.chatId, task.messageID,
                MessageHistory.STATUS_SENT);
        }catch (SmackException e){
          e.printStackTrace();
        }
      }
    }).start();
  }

  public class UploadHandlerTask{
    public double progress;
    public String chatId;
    public long messageID;

    public UploadHandlerTask(double progress, String chatId, long messageID){
      this.progress = progress;
      this.chatId = chatId;
      this.messageID = messageID;
    }
  }

  /**
   * sets the status
   *
   * @param available if true the status type will be set to available otherwise to unavailable
   * @param status    the status message
   * @return true if setting the status was successful
   */
  public boolean setStatus(boolean available, String status){
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

  /**
   * disconnects
   */
  public void disconnect(){
    if (connection != null && connection.isConnected()){
      connection.disconnect();
      Log.d("DEBUG", "Success: Disconnected.");
    }else
      Log.e("ERROR", "Disconnecting failed: No connection.");
  }

  public boolean reconnect(){
    if (connection == null)
      init();
    else if (!connection.isConnected())
      try{
        connection.connect();
      }catch (Exception e){
        Log.e("ERROR", "Couldn't connect.");
        Log.e("ERROR", e.toString());
        return false;
      }
    return true;
  }

  public boolean isConnected(){
    return connection != null && connection.isConnected();
  }

  public XMPPTCPConnection getConnection(){
    return connection;
  }
}
