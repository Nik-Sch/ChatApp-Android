package com.raspi.chatapp.single_chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.MainActivity;
import com.raspi.chatapp.MessageService;
import com.raspi.chatapp.R;

import org.jivesoftware.smack.packet.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatActivity extends AppCompatActivity{

    private String buddyJID;

    private ChatArrayAdapter caa;

    private ListView listView;
    private EditText textIn;
    private Button sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent in = getIntent();
        Bundle extras = in.getExtras();
        if (extras != null)
            buddyJID = extras.getString(MainActivity.BUDDY_ID);
        getSupportActionBar().setTitle(buddyJID);

        initUI();

        Intent initIntent = new Intent(this, MessageService.class);
        initIntent.setData(Uri.parse(MainActivity.INIT_XMPP));
        this.startService(initIntent);


        IntentFilter messageFilter = new IntentFilter(MainActivity.RECEIVE_MESSAGE);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    private void initUI(){
        caa = new ChatArrayAdapter(this, R.layout.chat);

        listView = (ListView) findViewById(R.id.chat_listview);
        textIn = (EditText) findViewById(R.id.chat_in);
        sendBtn = (Button) findViewById(R.id.chat_sendBtn);

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
    }
    private void sendMessage(String message){
        Intent sendIntent = new Intent(this, MessageService.class);
        sendIntent.setData(Uri.parse(MainActivity.SEND_MESSAGE));
        sendIntent.putExtra(MainActivity.BUDDY_ID, buddyJID);
        sendIntent.putExtra(MainActivity.MESSAGE_BODY, message);
        startService(sendIntent);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        caa.add(new ChatMessage(false, message, df.format(new Date())));
        textIn.setText("");
    }

    private class MessageReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent){
            Bundle extras = intent.getExtras();
            String message = "";
            if (extras != null)
                message = extras.getString(MainActivity.MESSAGE_BODY);
            caa.add(new ChatMessage(true, message, (new SimpleDateFormat("HH:mm").format(new Date()))));
        }
    }
}
