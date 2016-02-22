package com.raspi.chatapp.ui.util.message_array;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import com.github.ankushsachdeva.emojicon.EmojiconTextView;
import com.raspi.chatapp.R;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MessageArrayAdapter extends ArrayAdapter<MessageArrayContent>
        implements Iterable<MessageArrayContent>{
  private List<MessageArrayContent> messageList = new ArrayList<>();

  public MessageArrayAdapter(Context context, int textViewResourceId){
    super(context, textViewResourceId);
  }

  @Override
  public void insert(MessageArrayContent object, int index){
    messageList.add(index, object);
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
    messageList.add(obj);
    notifyDataSetChanged();
  }

  @Override
  public void clear(){
    messageList.clear();
    notifyDataSetChanged();
  }

  public int getCount(){
    return messageList.size();
  }

  public MessageArrayContent getItem(int i){
    return messageList.get(i);
  }

  public View getView(final int position, View ConvertView, ViewGroup parent){
    return getView(position, ConvertView, parent, true);
  }

  public View getView(final int position, View ConvertView, ViewGroup parent,
                      boolean reloadImage){
    View v = ConvertView;
    MessageArrayContent Obj = getItem(position);

    if (Obj.getClass() == TextMessage.class){
      final TextMessage msgObj = (TextMessage) Obj;
      if (v == null || v.findViewById(R.id.message_text) == null){
        LayoutInflater inflater = (LayoutInflater) this.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.message_text, parent, false);
      }

      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id.message_text);
      LinearLayout layoutInner = (LinearLayout) v.findViewById(R.id
              .message_text_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 :
              R.drawable.bubble_b1);
      EmojiconTextView chatText = (EmojiconTextView) v.findViewById(R.id.message_text_text);
      chatText.setExpandedSize(true);
      chatText.setText(msgObj.message);
      TextView chatTime = (TextView) v.findViewById(R.id.message_text_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format
              (msgObj.time));
      if (msgObj.left){
        layoutOuter.setGravity(Gravity.START);
        v.findViewById(R.id.message_text_status).setVisibility(View.GONE);
        layoutInner.setAlpha(1f);
        //I don't really care about the status, obviously read is right...
      }else{
        layoutOuter.setGravity(Gravity.END);
        v.findViewById(R.id.message_text_status).setVisibility(View.VISIBLE);
        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            layoutInner.setAlpha(0.5f);
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
      final ImageMessage msgObj = (ImageMessage) Obj;
      if (v == null || v.findViewById(R.id.message_image) == null){
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.message_image, parent, false);
      }

      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id
              .message_image);
      RelativeLayout layoutInner = (RelativeLayout) v.findViewById(R.id
              .message_image_inner);
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_a1 :
              R.drawable.bubble_b1);
      EmojiconTextView description = (EmojiconTextView) v.findViewById(R.id
              .message_image_description);
      description.setExpandedSize(true);
      description.setText(msgObj.description);

      ImageView imageView = (ImageView) v.findViewById(R.id
              .message_image_image);


      TextView chatTime = (TextView) v.findViewById(R.id.message_image_timeStamp);
      chatTime.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format
              (msgObj.time));


      if (msgObj.left){
        layoutOuter.setGravity(Gravity.START);
        v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
        if (MessageHistory.STATUS_RECEIVING.equals(msgObj.status)){
          v.findViewById(R.id.message_image_progress).setVisibility(View.VISIBLE);
          layoutInner.setAlpha(0.5f);
        }else{
          v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
          layoutInner.setAlpha(1f);
          try{
            if (reloadImage)
              loadBitmap(new File(msgObj.file), imageView);
          }catch (Exception e){
          }
        }
      }else{
        try{
          if (reloadImage)
            loadBitmap(new File(msgObj.file), imageView);
        }catch (Exception e){
        }
        layoutOuter.setGravity(Gravity.END);
        v.findViewById(R.id.message_image_status).setVisibility(View.VISIBLE);
        ProgressBar progressBar;

        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            ((ImageView) v.findViewById(R.id.message_image_status))
                    .setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(1);
            layoutInner.setAlpha(0.5f);
            break;
          case MessageHistory.STATUS_SENDING:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(msgObj.progress);
            layoutInner.setAlpha(0.5f);
            break;
          case MessageHistory.STATUS_SENT:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.single_grey_hook);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_RECEIVED:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.two_grey_hook);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_READ:
            imageView = (ImageView) v.findViewById(R.id
                    .message_image_status);
            imageView.setImageResource(R.drawable.two_blue_hook);
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
        }
      }
    }else if (Obj.getClass() == Date.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_date, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      date.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(((Date) Obj).date));
    }else if (Obj.getClass() == LoadMoreMessages.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_load_more, parent, false);
    }else if (Obj.getClass() == NewMessage.class){
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_new_messages, parent, false);

      TextView nm = (TextView) v.findViewById(R.id.new_messages);
      nm.setText(((NewMessage) Obj).status);
    }
    return v;
  }

  /**
   * Removes the object at the specified location from this {@code List}.
   *
   * @param position
   *            the index of the object to remove.
   * @return the removed object.
   * @throws UnsupportedOperationException
   *                if removing from this {@code MessageArrayAdapter} is not
   *                supported.
   * @throws IndexOutOfBoundsException
   *                if {@code location < 0 || location >= size()}
   */
  public MessageArrayContent remove(int position) throws UnsupportedOperationException,
          IndexOutOfBoundsException{
    return messageList.remove(position);
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

  @Override
  public Iterator<MessageArrayContent> iterator(){
    return new Iterator<MessageArrayContent>(){

      private int currentIndex = 0;

      @Override
      public boolean hasNext(){
        return currentIndex < messageList.size() && messageList.get
                (currentIndex) != null;
      }

      @Override
      public MessageArrayContent next(){
        return messageList.get(currentIndex++);
      }

      @Override
      public void remove(){
        throw new UnsupportedOperationException();
      }
    };
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
