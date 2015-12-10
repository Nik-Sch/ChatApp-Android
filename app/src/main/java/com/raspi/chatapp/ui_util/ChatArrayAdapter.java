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
      v = inflater.inflate(R.layout.chat_list_entry, parent, false);
    }

    ChatEntry chatObj = getItem(position);

    ((TextView) v.findViewById(R.id.chat_list_entry_name)).setText(chatObj.name);
    if (!chatObj.lastMessageStatus.equals("")){
      ((TextView) v.findViewById(R.id.chat_list_entry_time)).setText(chatObj.lastMessageDate);
      ((TextView) v.findViewById(R.id.chat_list_entry_mess)).setText(chatObj.lastMessageMessage);
      ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageResource(R.drawable.haken_tmp);
    }else{
      ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageDrawable(null);
      ((TextView) v.findViewById(R.id.chat_list_entry_time)).setText("");
      ((TextView) v.findViewById(R.id.chat_list_entry_mess)).setText("");
    }
    return v;
  }
}
