package com.raspi.chatapp.ui_util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raspi.chatapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatArrayAdapter extends ArrayAdapter<ChatMessage>{

    private TextView chatText;
    private TextView chatTime;
    private List<ChatMessage> MessageList = new ArrayList<ChatMessage>();
    private RelativeLayout layout;

    public ChatArrayAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    @Override
    public void add(ChatMessage chatMessage){
        MessageList.add(chatMessage);
        notifyDataSetChanged();
    }

    public int getCount(){
        return MessageList.size();
    }

    public ChatMessage getItem(int i){
        return MessageList.get(i);
    }

    public View getView(int position, View ConvertView, ViewGroup parent){
        View v = ConvertView;
        if (v == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.chat, parent, false);
        }

        layout = (RelativeLayout) v.findViewById(R.id.chat_message);
        ChatMessage msgObj = getItem(position);

        chatText = (TextView) v.findViewById(R.id.chat_singleMessage);
        chatText.setText(msgObj.message);
        chatTime = (TextView) v.findViewById(R.id.chat_timeStamp);
        chatTime.setText(msgObj.time);
        chatText.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 : R.drawable.bubble_b1);

        layout.setGravity(msgObj.left ? Gravity.LEFT : Gravity.RIGHT);

        return v;
    }
}
