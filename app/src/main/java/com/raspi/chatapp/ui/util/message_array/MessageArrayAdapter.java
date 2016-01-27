package com.raspi.chatapp.ui.util.message_array;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
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
import com.raspi.chatapp.util.internet.http.DownloadService;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageArrayAdapter extends ArrayAdapter<MessageArrayContent>{
  private List<MessageArrayContent> MessageList = new ArrayList<MessageArrayContent>();

  public MessageArrayAdapter(Context context, int textViewResourceId){
    super(context, textViewResourceId);
  }

  private static boolean cancelPotentialWork(File file, ImageView imageView){
    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
    if (bitmapWorkerTask != null){
      final File bitmapFile = bitmapWorkerTask.data;
      if (bitmapFile == null || bitmapFile != file)
        bitmapWorkerTask.cancel(true);
      else return true;
    }
    return true;
  }

  private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView){
    if (imageView != null){
      final Drawable drawable = imageView.getDrawable();
      if (drawable instanceof AsyncDrawable){
        final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
        return asyncDrawable.getBitmapWorkerTask();
      }
    }
    return null;
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

  public View getView(final int position, View ConvertView, ViewGroup parent){
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

      ImageView imageView = (ImageView) v.findViewById(R.id
              .message_image_image);
      try{
        loadBitmap(new File(msgObj.file), imageView);
      }catch (Exception e){
      }

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
              //TODO
              @Override
              public void onClick(View v){
                try{
                  MyFileUtils mfu = new MyFileUtils();
                  if (!mfu.isExternalStorageWritable())
                    throw new Exception("ext storage not writable. Cannot save " +
                            "image");
                  final ImageMessage msg = (ImageMessage) maa.getItem(position);
                  Intent intent = new Intent(getContext(), DownloadService.class);
                  intent.setAction(DownloadService.DOWNLOAD_ACTION);
                  intent.putExtra(DownloadService.PARAM_URL, msg.url);
                  final MessageHistory messageHistory = new MessageHistory
                          (getContext());
                  intent.putExtra(DownloadService.PARAM_RECEIVER, new
                          ResultReceiver(new Handler()){
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData){
                              super.onReceiveResult(resultCode, resultData);
                              if (resultCode == DownloadService.UPDATE_PROGRESS){
                                Long messageId = resultData.getLong(DownloadService.PARAM_MESSAGE_ID);
                                int progress = resultData.getInt(DownloadService.PARAM_PROGRESS);
                                int size = maa.getCount();
                                MessageArrayContent mac;
                                for (int i = 0; i < size; i++){
                                  mac = maa.getItem(i);
                                  if (mac instanceof ImageMessage){
                                    if (msg._ID == messageId){
                                      Log.d("DEBUG DOWNLOAD", "progress: " + progress);
                                      if (progress < 100)
                                        msg.progress = progress;
                                      else{
                                        msg.status = MessageHistory.STATUS_READ;
                                        messageHistory.updateMessageStatus
                                                (msg.chatId, messageId,
                                                        MessageHistory.STATUS_READ);
                                      }
                                      maa.notifyDataSetChanged();
                                    }
                                  }
                                }
                              }
                            }
                          });
                  intent.putExtra(DownloadService.PARAM_FILE, msg.file);
                  intent.putExtra(DownloadService.PARAM_MESSAGE_ID, msg._ID);
                  getContext().startService(intent);
                }catch (Exception e){

                }
              }
            });
            break;
          case MessageHistory.STATUS_SENDING:
          case MessageHistory.STATUS_RECEIVING:
            v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(msgObj.progress);
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
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_date, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      date.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(((Date) Obj).date));
    }else if (Obj.getClass() == NewMessage.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_new_messages, parent, false);

      TextView nm = (TextView) v.findViewById(R.id.new_messages);
      nm.setText(((NewMessage) Obj).status);
    }
    return v;
  }

  private void loadBitmap(File file, ImageView imageView) throws Exception{
    if (cancelPotentialWork(file, imageView)){
      final BitmapWorkerTask task = new BitmapWorkerTask(imageView, imageView
              .getLayoutParams().width, imageView.getLayoutParams()
              .height);
      imageView.setImageDrawable(new AsyncDrawable(getContext().getResources
              (), BitmapFactory.decodeResource(getContext().getResources(),
              R.drawable.placeholder), task));
      task.execute(file);

    }
  }

  static class AsyncDrawable extends BitmapDrawable{
    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

    public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask
            bitmapWorkerTask){
      super(res, bitmap);
      bitmapWorkerTaskWeakReference = new WeakReference<BitmapWorkerTask>
              (bitmapWorkerTask);
    }

    public BitmapWorkerTask getBitmapWorkerTask(){
      return bitmapWorkerTaskWeakReference.get();
    }
  }

  private class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap>{
    private final WeakReference<ImageView> imageViewWeakReference;
    private File data;
    private int width, height;

    public BitmapWorkerTask(ImageView imageView, int width, int height){
      imageViewWeakReference = new WeakReference<ImageView>(imageView);
      this.width = width;
      this.height = height;
    }

    @Override
    protected Bitmap doInBackground(File... params){
      data = params[0];

      // First decode with inJustDecodeBounds=true to check dimensions
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(data.getAbsolutePath(), options);

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, width, height);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      return BitmapFactory.decodeFile(data.getAbsolutePath(), options);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap){
      if (isCancelled())
        bitmap = null;

      if (imageViewWeakReference != null && bitmap != null){
        final ImageView imageView = imageViewWeakReference.get();
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask
                (imageView);
        if (this == bitmapWorkerTask && imageView != null)
          imageView.setImageBitmap(bitmap);
      }
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
}
