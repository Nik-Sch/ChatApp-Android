package com.raspi.chatapp.ui_util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.raspi.chatapp.R;

import java.util.ArrayList;
import java.util.List;

public class ChatArrayAdapter extends ArrayAdapter<ChatEntry>{
    private List<ChatEntry> chatList = new ArrayList<ChatEntry>();

    public ChatArrayAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    @Override
    public void add(ChatEntry chatEntry){
        chatList.add(chatEntry);
        notifyDataSetChanged();
    }

    public int getCount(){
        return chatList.size();
    }

    public ChatEntry getItem(int i){
        return chatList.get(i);
    }

    public View getView(int position, View ConvertView, ViewGroup parent){
        View v = ConvertView;
        if (v == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.chat, parent, false);
        }

        ChatEntry chatObj = getItem(position);

        TextView name = (TextView) v.findViewById(R.id.chat_list_entry_name);
        name.setText(chatObj.name);
        TextView time = (TextView) v.findViewById(R.id.chat_list_entry_time);
        time.setText(chatObj.lastMessageDate);
        TextView msg = (TextView) v.findViewById(R.id.chat_list_entry_mess);
        msg.setText(chatObj.lastMessageMessage);
        ImageView status = (ImageView) v.findViewById(R.id.chat_list_entry_status);
        status.setImageResource(R.drawable.haken_tmp);

        return v;
    }
}
