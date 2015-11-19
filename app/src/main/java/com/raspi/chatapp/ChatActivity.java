package com.raspi.chatapp;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ChatActivity extends AppCompatActivity {

    private ChatArrayAdapter caa;
    private ListView listView;
    private EditText textIn;
    private Button send;
    Intent intent;

    private boolean side = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent in = getIntent();

        caa = new ChatArrayAdapter(getApplicationContext(), R.layout.chat);
        listView = (ListView)findViewById(R.id.chat_listview);
        textIn = (EditText)findViewById(R.id.chat_in);
        send = (Button)findViewById(R.id.chat_sendBtn);

        textIn.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))
                    return sendChatMessage();
                else
                    return false;
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(caa);

        caa.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                System.out.println("Change observed");
                listView.setSelection(caa.getCount() - 1);
            }
        });
    }

    private boolean sendChatMessage() {
        System.out.println("Sending message: " + textIn.getText().toString());
        caa.add(new ChatMessage(side, textIn.getText().toString()));
        textIn.setText("");
        side=!side;
        return true;
    }
}
