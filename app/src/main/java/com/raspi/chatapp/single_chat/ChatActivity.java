package com.raspi.chatapp.single_chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.MainActivity;
import com.raspi.chatapp.R;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

public class ChatActivity extends AppCompatActivity{

    private String BuddyJID;

    private MessageService messageService;
    private GUI gui;

    private ChatArrayAdapter caa;
    private ListView listView;
    private EditText textIn;
    private Button send;

    private boolean side = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent in = getIntent();
        BuddyJID = in.getStringExtra(MainActivity.BUDDY_ID);

        //getSupportActionBar().setTitle(BuddyJID);

        gui = new GUI(this);
        messageService = new MessageService();


    }
}
