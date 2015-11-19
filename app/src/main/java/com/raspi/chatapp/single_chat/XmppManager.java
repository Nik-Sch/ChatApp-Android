package com.raspi.chatapp.single_chat;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

public class XmppManager {

    private static final int packetReplyTime = 1000;

    private String server;
    private String service;
    private int port;

    private GUI gui;

    private XMPPTCPConnectionConfiguration config;
    private XMPPTCPConnection connection;

    private ChatManager chatManager;
    private ChatMessageListener messageListener;
    private ChatManagerListener managerListener;


    /**
     * creates a IM Manager with the given server ID
     * @param server host address
     * @param service service name
     * @param port port
     */
    public XmppManager(String server, String service, int port, GUI gui){
        this.server = server;
        this.service = service;
        this.port = port;
        this.gui = gui;
    }

    /**
     * initializes the connection with the server
     * @return true if a connection could be established
     */
    public boolean init(){
        SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTime);

        config = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(service)
                .setHost(server)
                .setPort(port)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled).build();
        connection = new XMPPTCPConnection(config);
        try {
            connection.connect();
        } catch (Exception e){
            System.err.println(e.toString());
            return false;
        }

        messageListener = new MyChatMessageListener();
        managerListener = new MyChatManagerListener();

        chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addChatListener(managerListener);
        return true;
    }

    /**
     * logs in to the server
     * @param username the username to use
     * @param password the corresponding password
     * @return true if the login was successful
     */
    public boolean performLogin(String username, String password){
        if (connection != null && connection.isConnected())
            try {
                connection.login(username, password);
            } catch (Exception e){
                System.err.println(e.toString());
                return false;
            }
        else
            return false;
        return true;
    }

    /**
     * creates an roster entry
     * @param user the username of the Buddy to create the roster entry for
     * @param name the name to give this Buddy
     * @return true if the creation was successful
     */
    public boolean createEntry(String user, String name){
        Roster roster = Roster.getInstanceFor(connection);
        try {
            roster.createEntry(user, name, null);
        } catch (Exception e) {
            System.err.println(e.toString());
            return false;
        }
        return true;
    }

    /**
     * sends a text message
     * @param message the message text to send
     * @param buddyJID the Buddy to receive the message
     * @return true if sending was successful
     */
    public boolean sendMessage(String message, String buddyJID){
        Chat chat = chatManager.createChat(buddyJID, messageListener);
        try {
            chat.sendMessage(message);
        } catch (Exception e) {
            System.err.println(e.toString());
            return false;
        }
        return true;
    }

    /**
     * sets the status
     * @param available if true the status type will be set to available otherwise to unavailable
     * @param status the status message
     * @return true if setting the status was successful
     */
    public boolean setStatus(boolean available, String status){
        Presence.Type type = available? Presence.Type.available : Presence.Type.unavailable;
        Presence presence = new Presence(type);

        presence.setStatus(status);
        try {
            connection.sendStanza(presence);
        } catch (Exception e){
            System.err.println(e.toString());
            return false;
        }
        return true;
    }

    /**
     * destroys the connection (performs a disconnect)
     */
    public void destroy(){
        if (connection != null && connection.isConnected())
            connection.disconnect();
    }

    private class MyChatMessageListener implements ChatMessageListener{
        @Override
        public void processMessage(Chat chat, Message message) {
            gui.receiveMessage(message);
        }
    }

    private class MyChatManagerListener implements ChatManagerListener{
        @Override
        public void chatCreated(Chat chat, boolean b) {

        }
    }
}
