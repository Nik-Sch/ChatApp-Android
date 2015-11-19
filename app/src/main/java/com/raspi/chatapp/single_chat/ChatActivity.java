package com.raspi.chatapp.single_chat;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.MainActivity;
import com.raspi.chatapp.R;

import org.jivesoftware.smack.packet.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    private static final String server = "raspi-server.mooo.com";
    private static final String service = "raspi-server.mooo.com";
    private static final int port = 5222;

    private String BuddyJID;

    private GUI gui;
    private XmppManager xmppManager;

    private ChatArrayAdapter caa;
    private ListView listView;
    private EditText textIn;
    private Button send;

    private boolean side = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent in = getIntent();
        BuddyJID = in.getStringExtra(MainActivity.BUDDY_ID);

        //getSupportActionBar().setTitle(BuddyJID);

        gui = new GUI(this);
        xmppManager = new XmppManager(server, service, port, gui);
    }
}
