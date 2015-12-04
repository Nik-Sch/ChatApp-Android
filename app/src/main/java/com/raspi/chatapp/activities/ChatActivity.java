package com.raspi.chatapp.activities;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.sqlite.MessageHistory;
import com.raspi.chatapp.ui_util.MessageArrayAdapter;
import com.raspi.chatapp.ui_util.ChatMessage;
import com.raspi.chatapp.util.Globals;
import com.raspi.chatapp.R;
import com.raspi.chatapp.util.XmppManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity{
    private static final int MESSAGE_LIMIT = 30;

    private String buddyId;
    private String chatName;

    private MessageArrayAdapter caa;
    private MessageHistory messageHistory;

    private ListView listView;
    private EditText textIn;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Intent in = getIntent();
        if (in != null){
            Bundle extras = in.getExtras();
            if (extras != null){
                if (extras.containsKey(MainActivity.BUDDY_ID))
                    buddyId = extras.getString(MainActivity.BUDDY_ID);
                if (extras.containsKey(MainActivity.CHAT_NAME))
                    chatName = extras.getString(MainActivity.CHAT_NAME);
            }
        }
        getSupportActionBar().setTitle((chatName != null) ? chatName : buddyId);
        messageHistory = new MessageHistory(this);

        initUI();
    }

    private void initUI(){
        caa = new MessageArrayAdapter(this, R.layout.chat);

        listView = (ListView) findViewById(R.id.chat_listview);
        textIn = (EditText) findViewById(R.id.chat_in);
        Button sendBtn = (Button) findViewById(R.id.chat_sendBtn);

        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sendMessage(textIn.getText().toString());
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(caa);

        caa.registerDataSetObserver(new DataSetObserver(){
            @Override
            public void onChanged(){
                super.onChanged();
                listView.setSelection(caa.getCount() - 1);
            }
        });
        messageHistory.getMessages(buddyId, MESSAGE_LIMIT);
    }

    private void sendMessage(String message){
        XmppManager xmppManager = ((Globals) getApplication()).getXmppManager();

        if (xmppManager != null && xmppManager.isConnected()){
            xmppManager.sendMessage(message, buddyId);
        } else
            Log.e("ERROR", "There was an error with the connection while sending a message.");

        SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.GERMANY);
        caa.add(new ChatMessage(false, message, df.format(new Date())));
        textIn.setText("");
//        messageHistory.addMessage(buddyId, getSharedPreferences(MainActivity.PREFERENCES, 0)
//                .getString(MainActivity.USERNAME, ""), MessageHistory.TYPE_TEXT, message,
//                MessageHistory.STATUS_WAITING);
    }

}
