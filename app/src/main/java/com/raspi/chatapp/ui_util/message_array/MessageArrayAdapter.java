package com.raspi.chatapp.ui_util.message_array;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_text, parent, false);

      TextMessage msgObj = (TextMessage) Obj;
      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id.message_text);
      LinearLayout layoutInner = (LinearLayout) v.findViewById(R.id
              .message_text_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 :
              R.drawable.bubble_b1);
      TextView chatText = (TextView) v.findViewById(R.id.message_text_text);
      chatText.setText(msgObj.message);
      TextView chatTime = (TextView) v.findViewById(R.id.message_text_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format
              (msgObj.time));
      if (msgObj.left){
        layoutOuter.setGravity(Gravity.START);
        v.findViewById(R.id.message_text_status).setVisibility(View.GONE);
        //I don't really care about the status, obviously read is right...
      }else{
        layoutOuter.setGravity(Gravity.END);
        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            v.findViewById(R.id.message_text_status).setVisibility(View.GONE);
            layoutInner.setAlpha(0.2f);
            break;
          case MessageHistory.STATUS_SENT:
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.single_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_RECEIVED:
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.two_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_READ:
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.two_blue_hook);
            layoutInner.setAlpha(1f);
            break;
        }
      }
    }else if (Obj.getClass() == ImageMessage.class){
      LayoutInflater inflater = (LayoutInflater) getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_image, parent, false);

      final ImageMessage msgObj = (ImageMessage) Obj;
      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id
              .message_image);
      RelativeLayout layoutInner = (RelativeLayout) v.findViewById(R.id
              .message_image_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 :
              R.drawable.bubble_b1);
      TextView description = (TextView) v.findViewById(R.id.message_image_description);
      description.setText(msgObj.description);
      ImageView image = (ImageView) v.findViewById(R.id.message_image_image);
      if (image.getDrawable() != null)
        ((BitmapDrawable) image.getDrawable()).getBitmap().recycle();

      // First decode with inJustDecodeBounds=true to check dimensions
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(msgObj.file.getAbsolutePath(), options);

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, image
                      .getLayoutParams().width / 4,
              image.getLayoutParams().height / 4);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      image.setImageBitmap(BitmapFactory.decodeFile(msgObj.file
              .getAbsolutePath(), options));

      TextView chatTime = (TextView) v.findViewById(R.id.message_image_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format
              (msgObj.time));

      if (msgObj.left){
        layoutOuter.setGravity(Gravity.START);
        v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
        v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
        v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
      }else{
        layoutOuter.setGravity(Gravity.END);
        ImageView imageView;
        ProgressBar progressBar;

        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(1);
            v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
            break;
          case MessageHistory.STATUS_CANCELED:
            v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.GONE);
            progressBar.setProgress(0);
            v.findViewById(R.id.message_image_retry).setVisibility(View.VISIBLE);
            final MessageArrayAdapter maa = this;
            v.findViewById(R.id.message_image_retry).setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                //cannot cast to globals...
                /*
                XmppManager xmppManager = ((Globals) getContext())
                        .getXmppManager();
                if (xmppManager != null)
                  xmppManager.new sendImage(msgObj, maa).execute(new Upload
                          .Task(msgObj.file, msgObj.description, msgObj.chatId,
                          msgObj._ID, xmppManager.getConnection(), new
                          MessageHistory(getContext())));*/
              }
            });
            break;
          case MessageHistory.STATUS_SENDING:
            v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress((int) (msgObj.progress * 100));
            v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
            break;
          case MessageHistory.STATUS_SENT:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.single_grey_hook);
            imageView.setVisibility(View.VISIBLE);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
            break;
          case MessageHistory.STATUS_RECEIVED:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.two_grey_hook);
            imageView.setVisibility(View.VISIBLE);
            v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            break;
          case MessageHistory.STATUS_READ:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.two_blue_hook);
            imageView.setVisibility(View.VISIBLE);
            v.findViewById(R.id.message_image_retry).setVisibility(View.GONE);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            break;
        }
      }
    }else if (Obj.getClass() == Date.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_date, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      date.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(((Date) Obj).date));
    }
    return v;
  }

  private int calculateInSampleSize(
          BitmapFactory.Options options, int reqWidth, int reqHeight){
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth){

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight
              && (halfWidth / inSampleSize) > reqWidth){
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }
}
