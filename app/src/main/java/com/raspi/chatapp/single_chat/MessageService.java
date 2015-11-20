package com.raspi.chatapp.single_chat;

import android.app.IntentService;
import android.content.Intent;

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

public class MessageService extends IntentService{

    private static final String server = "raspi-server.mooo.com";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;

    private XmppManager xmppManager;


    public MessageService(){
        super("ChatAppMessageService");
        xmppManager = new XmppManager(server, service, port, this);
        if (!xmppManager.init() ||
                !xmppManager.performLogin(getUserName(), getPassword()))
            System.err.println("There was an error with the connection");
    }

    @Override
    protected void onHandleIntent(Intent intent){

    }

    private String getUserName(){
        return "niklas";
    }

    private String getPassword(){
        return "passwNiklas";
    }


    private class XmppManager{

        private static final int packetReplyTime = 1000;

        private IntentService context;

        private String server;
        private String service;
        private int port;

        private XMPPTCPConnectionConfiguration config;
        private XMPPTCPConnection connection;

        private ChatManager chatManager;
        private ChatMessageListener messageListener;
        private ChatManagerListener managerListener;


        /**
         * creates a IM Manager with the given server ID
         *
         * @param server  host address
         * @param service service name
         * @param context the IntentService which sends and receives the messages
         */
        public XmppManager(String server, String service, int port, IntentService context){
            this.server = server;
            this.service = service;
            this.port = port;
            this.context = context;
        }

        /**
         * initializes the connection with the server
         *
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
            try{
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
         *
         * @param username the username to use
         * @param password the corresponding password
         * @return true if the login was successful
         */
        public boolean performLogin(String username, String password){
            if (connection != null && connection.isConnected())
                try{
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
         * returns the roster for the current connection
         *
         * @return the roster and null if the roster cannot be accessed
         */
        public Roster getRoster(){
            if (connection != null && connection.isConnected())
                return Roster.getInstanceFor(connection);
            else
                return null;
        }

        /**
         * sends a text message
         *
         * @param message  the message text to send
         * @param buddyJID the Buddy to receive the message
         * @return true if sending was successful
         */
        public boolean sendMessage(String message, String buddyJID){
            Chat chat = chatManager.createChat(buddyJID, messageListener);
            try{
                chat.sendMessage(message);
            } catch (Exception e){
                System.err.println(e.toString());
                return false;
            }
            return true;
        }

        /**
         * sets the status
         *
         * @param available if true the status type will be set to available otherwise to unavailable
         * @param status    the status message
         * @return true if setting the status was successful
         */
        public boolean setStatus(boolean available, String status){
            Presence.Type type = available ? Presence.Type.available : Presence.Type.unavailable;
            Presence presence = new Presence(type);

            presence.setStatus(status);
            try{
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
            public void processMessage(Chat chat, Message message){
                //TODO: create an Intent from context to Activity
            }
        }

        private class MyChatManagerListener implements ChatManagerListener{
            @Override
            public void chatCreated(Chat chat, boolean b){
                chat.addMessageListener(messageListener);
            }
        }
    }

}
