/*
 * Copyright 2016 Niklas Schelten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raspi.chatapp.util.internet;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * XmppManager is the wrapper Singleton for the xmppConnection which provides
 * all important functions. It is a Singleton to prohibit that one
 * message is received twice and to make sure it always exists :)
 */
public class XmppManager{

  /**
   * the HttpUrl to the server
   */
  public static final String SERVER = "raspi-server.ddns.net";
  private static final String SERVICE = "chatapp.com";
  private static final int PORT = 5222;


  private static class Holder{
    static final XmppManager INSTANCE = new XmppManager(SERVER, SERVICE, PORT);
  }

  private static final int packetReplyTime = 5000;

  private String server;
  private String service;
  private int port;

  private XMPPTCPConnection connection;

  private static LocalBroadcastManager LBMgr;

  /**
   * returns an instance of the xmppManager
   *
   * @param context - the context with which to initialize a
   *                LocalBroadCastManager, if this is not the first call of
   *                this function it might also be null
   * @return
   */
  @Nullable
  public static XmppManager getInstance(Context context){
    //yes this is the lazy implementation for the LBMgr but I think for the
    // LBMgr it is not that important that there might be a second
    // initialization
    if (LBMgr == null && context != null)
      LBMgr = LocalBroadcastManager.getInstance(context);
    return Holder.INSTANCE;
  }

  public static XmppManager getInstance(){
    return Holder.INSTANCE;
  }

  /**
   * creates a IM Manager with the given server ID
   *
   * @param server  host address
   * @param service service name
   * @param port    port
   */
  protected XmppManager(String server, String service, int port){
    this.server = server;
    this.service = service;
    this.port = port;
    Log.d("DEBUG", "Success: created xmppManager");
  }

  /**
   * initializes the connection with the server
   *
   * @return true if a connection could be established
   */
  public boolean init(){
    // configure the smack
    SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTime);
    // set smack to try reconnect every5 secs when loosing connection
    ReconnectionManager.setEnabledPerDefault(true);
    ReconnectionManager.setDefaultReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);
    ReconnectionManager.setDefaultFixedDelay(5);

    // build the connectionConfig
    XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setServiceName(service)
            .setHost(server)
            .setPort(port)
            .setSendPresence(false)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible).build();
    // create the connection and enable necessary features
    connection = new XMPPTCPConnection(config);
    // stream management prevents message loss
    connection.setUseStreamManagement(true);
    connection.addConnectionListener(connectionListener);
    connection.addAsyncStanzaListener(stanzaListener, new StanzaFilter(){
      @Override
      public boolean accept(Stanza stanza){
        return (stanza.getError() != null);
      }
    });
    // enable the pingManager to ping every 60 seconds and try to reconnect in order to  maintain
    // the connection
    PingManager pingManager = PingManager.getInstanceFor(connection);
    pingManager.setPingInterval(60);
    pingManager.registerPingFailedListener(new PingFailedListener(){
      @Override
      public void pingFailed(){
        try{
          connection.connect();
        }catch (Exception e){
          Log.e("ERROR", "Couldn't connect.");
          Log.e("ERROR", e.toString());
        }
      }
    });
    // finally try to connect to the server
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

  private StanzaListener stanzaListener = new StanzaListener(){
    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException{
      Log.d("STANZA RECEIVED", packet.toString());
    }
  };

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
   * send the raw string as a message without wrapping it with xml attributes
   * @param message the message to be sent
   * @param buddyJID the buddy to send the message to
   */
  public void sendRaw(String message, String buddyJID){
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      try{
        Chat chat = chatManager.createChat(buddyJID);
        chat.sendMessage(message);
      }catch (Exception e){
        e.printStackTrace();
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
  public boolean sendTextMessage(String message, String buddyJID, long id){
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      if (buddyJID.indexOf('@') == -1)
        buddyJID += "@" + service;
      Chat chat = chatManager.createChat(buddyJID);
      try{
        // wrap the message with all necessary xml attributes
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element msg = doc.createElement("message");
        doc.appendChild(msg);
        msg.setAttribute("type", MessageHistory.TYPE_TEXT);
        msg.setAttribute("id", String.valueOf(id));
        Element file = doc.createElement("content");
        msg.appendChild(file);
        file.setTextContent(message);

        // transform everything to a string
        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);
        message = writer.toString();

        // send the message
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
   * sends an image message
   *
   * @param serverFile  the file on the server
   * @param description the description of the sent image
   * @param buddyJID    the Buddy to receive the message
   * @return true if sending was successful
   */
  public boolean sendImageMessage(String serverFile, String description, String
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

        // create the string
        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);
        String message = writer.toString();

        // send the message
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
   * send an acknowledgement
   * @param buddyId the buddyId to receive the acknowledgement
   * @param othersId the id the buddy has sent the message with
   * @param type the type of acknowledgement to send
   * @return true if sending was successful
   */
  public boolean sendAcknowledgement(String buddyId, long othersId, String type){
    ChatManager chatManager = ChatManager.getInstanceFor(connection);
    if (connection != null && connection.isConnected() && chatManager != null){
      if (buddyId.indexOf('@') == -1)
        buddyId += "@" + service;
      Chat chat = chatManager.createChat(buddyId);
      try{
        // create the message structure
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element ack = doc.createElement("acknowledgement");
        doc.appendChild(ack);
        ack.setAttribute("id", String.valueOf(othersId));
        ack.setAttribute("type", type);

        // create the string representation of the message
        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult r = new StreamResult(writer);
        t.transform(new DOMSource(doc), r);
        String message = writer.toString();

        // send the message
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
   * creates a list of all RosterEntries
   * @return the rosterEntryArray
   */
  public RosterEntry[] listRoster(){
    try{
      // get the roster and if it is not loaded reload it
      Roster roster = Roster.getInstanceFor(connection);
      if (!roster.isLoaded())
        roster.reloadAndWait();
      RosterEntry[] result = new RosterEntry[roster.getEntries().size()];
      int i = 0;
      // loop through all roster entries and append them to the array
      for (RosterEntry entry: roster.getEntries()){
        result[i++] = entry;
      }
      return result;
    }catch (Exception e){
      e.printStackTrace();
    }
    return new RosterEntry[0];
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
      // set the presence type
      Presence presence = new Presence(available
              ? Presence.Type.available
              : Presence.Type.unavailable);

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
   * returns whether the app has an active connection to the server
   * @return true if the connection is connected to the xmpp server
   */
  public boolean isConnected(){
    return connection != null && connection.isConnected();
  }

  /**
   * returns the underlying connection to the server
   * @return the connection
   */
  public XMPPTCPConnection getConnection(){
    return connection;
  }

  // just for debug purposes
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
    }

    @Override
    public void connectionClosedOnError(Exception e){
      Log.d("XMPP_MANAGER", "Connection closed on error");
    }

    @Override
    public void reconnectionSuccessful(){
      Log.d("XMPP_MANAGER", "reconnected successfully");
      LBMgr.sendBroadcast(new Intent(Constants.RECONNECTED));

    }

    @Override
    public void reconnectingIn(int seconds){

    }

    @Override
    public void reconnectionFailed(Exception e){
      Log.d("XMPP_MANAGER", "reconnecting failed");
    }
  };
}
