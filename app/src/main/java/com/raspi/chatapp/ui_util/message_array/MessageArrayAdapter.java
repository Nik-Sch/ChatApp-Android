package com.raspi.chatapp.ui_util.message_array;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.sqlite.MessageHistory;

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

  @Override
  public void clear(){
    MessageList.clear();
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
      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id.text_message);
      LinearLayout layoutInner = (LinearLayout) v.findViewById(R.id
              .text_message_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 : R.drawable.bubble_b1);
      TextView chatText = (TextView) v.findViewById(R.id.text_message_text);
      chatText.setText(msgObj.message);
      TextView chatTime = (TextView) v.findViewById(R.id.text_message_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(msgObj.time));
      if (msgObj.left){
        layoutOuter.setGravity(Gravity.START);
        ((ImageView)v.findViewById(R.id.chat_status)).setImageDrawable(null);
        //I don't really care about the status, obviously read is right...
      }else{
        layoutOuter.setGravity(Gravity.END);
        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            ((ImageView)v.findViewById(R.id.chat_status)).setImageDrawable(null);
            layoutInner.setAlpha(0.2f);
            break;
          case MessageHistory.STATUS_SENT:
            ((ImageView)v.findViewById(R.id.chat_status)).setImageResource(R.drawable.single_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_RECEIVED:
            ((ImageView)v.findViewById(R.id.chat_status)).setImageResource(R.drawable.two_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_READ:
            ((ImageView)v.findViewById(R.id.chat_status)).setImageResource(R.drawable.two_blue_hook);
            layoutInner.setAlpha(1f);
            break;
        }
      }
    }else if (Obj.getClass() == Date.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.date_message, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      date.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(((Date) Obj).date));
    }
    return v;
  }
}
