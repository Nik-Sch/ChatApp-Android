package com.raspi.chatapp.ui_util.message_array;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raspi.chatapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageArrayAdapter extends ArrayAdapter<MessageArrayContent>{
  private List<MessageArrayContent> MessageList = new ArrayList<MessageArrayContent>();

  public MessageArrayAdapter(Context context, int textViewResourceId){
    super(context, textViewResourceId);
  }

  @Override
  public void add(MessageArrayContent obj){
    MessageList.add(obj);
    notifyDataSetChanged();
  }

  public int getCount(){
    return MessageList.size();
  }

  public MessageArrayContent getItem(int i){
    return MessageList.get(i);
  }

  public View getView(int position, View ConvertView, ViewGroup parent){
    View v = ConvertView;
    MessageArrayContent Obj = getItem(position);

    if (Obj.getClass() == TextMessage.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.text_message, parent, false);

      TextMessage msgObj = (TextMessage) Obj;
      TextView chatText = (TextView) v.findViewById(R.id.chat_messageText);
      chatText.setText(msgObj.message);
      TextView chatTime = (TextView) v.findViewById(R.id.chat_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(msgObj.time));

      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id.text_message);
      RelativeLayout layoutInner = (RelativeLayout) v.findViewById(R.id.chat_message_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 : R.drawable.bubble_b1);
      layoutOuter.setGravity(msgObj.left ? Gravity.START : Gravity.END);
    }else if (Obj.getClass() == Date.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.date_message, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      date.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(((Date) Obj).date));
    }
    return v;
  }
}
