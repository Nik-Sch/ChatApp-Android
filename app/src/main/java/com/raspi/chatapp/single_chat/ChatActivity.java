package com.raspi.chatapp.single_chat;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.MainActivity;
import com.raspi.chatapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity{

    private String buddyId;
    private String chatName;

    private ChatArrayAdapter caa;

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
        Bundle extras = in.getExtras();
        if (extras != null){
            if (extras.containsKey(MainActivity.BUDDY_ID))
                buddyId = extras.getString(MainActivity.BUDDY_ID);
            if (extras.containsKey(MainActivity.CHAT_NAME))
                chatName = extras.getString(MainActivity.CHAT_NAME);
        }
        getSupportActionBar().setTitle((chatName != null) ? chatName : buddyId);

        initUI();
    }

    private void initUI(){
        caa = new ChatArrayAdapter(this, R.layout.chat);

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
    }

    private void sendMessage(String message){
        Intent sendIntent = new Intent(MainActivity.SEND_MESSAGE);
        sendIntent.putExtra(MainActivity.BUDDY_ID, buddyId);
        sendIntent.putExtra(MainActivity.MESSAGE_BODY, message);
        Log.d("DEBUG", "UI: Sent Sent message (Intent)");
        LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.GERMANY);
        caa.add(new ChatMessage(false, message, df.format(new Date())));
        textIn.setText("");
    }

}
