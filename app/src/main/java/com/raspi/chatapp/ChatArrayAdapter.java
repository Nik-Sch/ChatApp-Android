package com.raspi.chatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

    private TextView chatText;
    private List<ChatMessage> MessageList = new ArrayList<ChatMessage>();
    private LinearLayout layout;

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public void add(ChatMessage chatMessage) {
        MessageList.add(chatMessage);
        notifyDataSetChanged();
        for (ChatMessage cm : MessageList)
            System.out.println("    " + cm.message);
    }

    public int getCount() {
        return MessageList.size();
    }

    public ChatMessage getItem(int i) {
        return MessageList.get(i);
    }

    public View getView(int position, View ConvertView, ViewGroup parent) {
        View v = ConvertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.chat, parent, false);
        }

        layout = (LinearLayout) v.findViewById(R.id.chat_messages);
        ChatMessage msgObj = getItem(position);

        chatText = (TextView) v.findViewById(R.id.chat_singleMessage);
        chatText.setText(msgObj.message);
        chatText.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 : R.drawable.bubble_b1);

        layout.setGravity(msgObj.left ? Gravity.LEFT : Gravity.RIGHT);

        return v;
    }

    public boolean saveToFile(File file){
        return true;
    }

    public boolean loadFromFile(File file){
        return true;
    }
}
