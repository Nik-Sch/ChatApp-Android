package com.raspi.chatapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.raspi.chatapp.service.MessageService;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

public class XmppManager{

    private static final int packetReplyTime = 1000;

    private Context context;

    private String server;
    private String service;
    private int port;

    private XMPPTCPConnection connection;

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
        this.context = context;
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
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled).build();
        connection = new XMPPTCPConnection(config);
        try{
            connection.connect();
        } catch (Exception e){
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
            } catch (Exception e){
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
        } else {
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
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        if (connection != null && connection.isConnected() && chatManager != null){
            Chat chat = chatManager.createChat(buddyJID);
            try{
                chat.sendMessage(message);
                Log.d("DEBUG", "Success: Sent message");
            } catch (Exception e){
                Log.e("ERROR", "Couldn't send message.");
                Log.e("ERROR", e.toString());
                return false;
            }
            return true;
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
            } catch (Exception e){
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
        }
        Log.e("ERROR", "Disconnecting failed: No connection.");
    }

    public boolean isConnected(){
        return connection != null && connection.isConnected();
    }

    public XMPPTCPConnection getConnection(){
        return connection;
    }
}
