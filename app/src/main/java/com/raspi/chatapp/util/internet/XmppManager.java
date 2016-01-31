package com.raspi.chatapp.util.internet;

import android.util.Log;

import com.raspi.chatapp.util.storage.MessageHistory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
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
 * message is received twice
 */
public class XmppManager{

  public static final String SERVER = "raspi-server.ddns.net";
  private static final String SERVICE = "chatapp.com";
  private static final int PORT = 5222;

  private static class Holder {
    static final XmppManager INSTANCE = new XmppManager(SERVER, SERVICE, PORT);
  }

  private static final int packetReplyTime = 5000;

  private String server;
  private String service;
  private int port;

  private XMPPTCPConnection connection;

  public static XmppManager getInstance(){
    return Holder.INSTANCE;
  }

  /**
   * creates a IM Manager with the given server ID
   * @param server  host address
   * @param service service name
   * @param port port
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
    SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTime);
    ReconnectionManager.setEnabledPerDefault(true);

    XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setServiceName(service)
            .setHost(server)
            .setPort(port)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible).build();
    connection = new XMPPTCPConnection(config);
    connection.setUseStreamManagement(true);
    connection.addConnectionListener(connectionListener);
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
  public boolean sendTextMessage(String message, String buddyJID){
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
   * @param buddyJID the Buddy to receive the message
   * @return true if sending was successful
   */
  public boolean sendImageMessage(String serverFile, String description, String
          buddyJID){
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

  public boolean isConnected(){
    return connection != null && connection.isConnected();
  }

  public XMPPTCPConnection getConnection(){
    return connection;
  }

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
