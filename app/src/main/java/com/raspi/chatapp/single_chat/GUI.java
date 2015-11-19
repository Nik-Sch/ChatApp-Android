package com.raspi.chatapp.single_chat;

import android.database.DataSetObserver;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.R;

import org.jivesoftware.smack.packet.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GUI {

    private ChatArrayAdapter caa;

    private ListView listView;
    private EditText textIn;
    private Button sendBtn;

    protected GUI(ChatActivity context) {
        caa = new ChatArrayAdapter(context, R.layout.chat);

        listView = (ListView) context.findViewById(R.id.chat_listview);
        textIn = (EditText) context.findViewById(R.id.chat_in);
        sendBtn = (Button) context.findViewById(R.id.chat_sendBtn);

        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage(false, textIn.getText().toString());
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(caa);

        caa.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(caa.getCount() - 1);
            }
        });
    }

    protected void sendMessage(boolean left, String message) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        caa.add(new ChatMessage(left, textIn.getText().toString(), df.format(new Date())));
        textIn.setText("");
    }

    protected void receiveMessage(Message message){

    }
}
