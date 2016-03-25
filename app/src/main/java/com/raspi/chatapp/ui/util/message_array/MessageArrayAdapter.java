/*
 * Copyright 2016 Niklas Schelten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raspi.chatapp.ui.util.message_array;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.ankushsachdeva.emojicon.EmojiconTextView;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.image.AsyncDrawable;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * this class is an arrayAdapter containing messageArrayContents and
 * implementing an iterable for making it easier to iterate through all items
 * in this adapter.
 */
public class MessageArrayAdapter extends ArrayAdapter<MessageArrayContent>
        implements Iterable<MessageArrayContent>{
  // the underlying structure is a basic arrayList
  private List<MessageArrayContent> messageList = new ArrayList<>();

  public MessageArrayAdapter(Context context, int textViewResourceId){
    super(context, textViewResourceId);
  }

  @Override
  public void insert(MessageArrayContent object, int index){
    // inserting an item will insert it into the arrayList
    messageList.add(index, object);
  }

  @Override
  public void add(MessageArrayContent obj){
    // add the data to the arrayList and notify for a change
    messageList.add(obj);
    notifyDataSetChanged();
  }

  @Override
  public void clear(){
    // clear the arrayList and notify for a change
    messageList.clear();
    notifyDataSetChanged();
  }

  @Override
  public int getCount(){
    // return the size of the arrayList
    return messageList.size();
  }

  @Override
  public MessageArrayContent getItem(int i){
    // return the specified item of the arrayList
    return messageList.get(i);
  }

  @Override
  public View getView(final int position, View ConvertView, ViewGroup parent){
    return getView(position, ConvertView, parent, true);
  }

  /**
   * Get a View that displays the data at the specified position in the data
   * set. You can either create a View manually or inflate it from an XML
   * layout file. When the View is inflated, the parent View (GridView,
   * ListView...) will apply default layout parameters unless you use
   * {@link android.view.LayoutInflater#inflate(int, ViewGroup, boolean)} to
   * specify a root view and to prevent attachment to the root.
   *
   * @param position    The position of the item within the adapter's data set
   *                    of the item whose view we want.
   * @param ConvertView The view that the item at this position previously
   *                    had. It may be null or from another class. It is used
   *                    to recycle as much as possible.
   * @param parent      The parent that this view will eventually be attached to
   * @param reloadImage if false and both, the old and the new, items are
   *                    imageViews and both contain the same bitmap the
   *                    bitmap will not be reloaded and stays the same. If
   *                    true any imageView will be reloaded no matter what.
   * @return the new view of the item at the specified position.
   */
  public View getView(final int position, View ConvertView, ViewGroup parent,
                      boolean reloadImage){
    // just for shorter access
    View v = ConvertView;
    // this is item we want to convert
    MessageArrayContent obj = getItem(position);

//    Typeface typeface = Typeface.createFromAsset(getContext().getAssets(),
//            "fonts/Aileron-SemiBold.otf");

    // switch through all possible classes. and yep switch statement only
    // allows some data, classes not included.
    if (obj instanceof TextMessage){
      final TextMessage msgObj = (TextMessage) obj;
      // if the view is null or it's layout is not / wrongly inflated
      if (v == null || v.findViewById(R.id.message_text) == null){
        LayoutInflater inflater = (LayoutInflater) this.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.message_text, parent, false);
      }

      LinearLayout layoutOuter = (LinearLayout) v.findViewById(R.id
              .message_text);
      // the inner layout contains everything that is really visible
      LinearLayout layoutInner = (LinearLayout) v.findViewById(R.id
              .message_text_inner);
      // set the background to the correct bubble
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_left :
              R.drawable.bubble_right);
      // this is the textView showing the actual message
      EmojiconTextView chatText = (EmojiconTextView) v.findViewById(R.id.message_text_text);
      // make the emojicons the correct size$
      chatText.setExpandedSize(true);
      // set the actual text
      chatText.setText(msgObj.message);
//      // set the typeface
//      chatText.setTypeface(typeface);
      // set the timeStamp to the correct string representation
      TextView chatTime = (TextView) v.findViewById(R.id.message_text_timeStamp);
      chatTime.setText(String.format(getContext().getResources().getString(R
              .string.time), msgObj.time));
      // everything else differs on whether the message is on the left or
      // right side
      if (msgObj.left){
        // set the gravity
        layoutOuter.setGravity(Gravity.START);
        // we received the message, therefore, disable the status
        v.findViewById(R.id.message_text_status).setVisibility(View.GONE);
        // and make sure that the message is not half transparent because it
        // might be transparent when we are sending it and failed to do so
        layoutInner.setAlpha(1f);
        // set the correct values for the left and right margins
        v.findViewById(R.id.message_margin_right).setLayoutParams(
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 0.2f));
        v.findViewById(R.id.message_margin_left).setLayoutParams(
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 0f));
      }else{
        // set the gravity
        layoutOuter.setGravity(Gravity.END);
        // set the correct values for the left and right margins
        v.findViewById(R.id.message_margin_right).setLayoutParams(
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 0f));
        v.findViewById(R.id.message_margin_left).setLayoutParams(
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 0.2f));
        v.findViewById(R.id.message_text_status).setVisibility(View.VISIBLE);
        // switch the status for how to display the message
        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            // if waiting show the hourglass and make it somewhat transparent
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            layoutInner.setAlpha(0.7f);
            break;
          case MessageHistory.STATUS_SENT:
            // if sent successfully show one grey hook and make it non
            // transparent
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.single_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_RECEIVED:
            // if the message was received show two grey hooks and make it non
            // transparent
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.two_grey_hook);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_READ:
            // if the message was read show two blue hooks and make it non
            // transparent
            ((ImageView) v.findViewById(R.id.message_text_status))
                    .setImageResource(R.drawable.two_blue_hook);
            layoutInner.setAlpha(1f);
            break;
        }
      }
    }else if (obj instanceof ImageMessage){

      final ImageMessage msgObj = (ImageMessage) obj;
      // if the view is null, the view has not been inflated or has been
      // inflated wrong or if the descriptions do not match inflate a new layout
      if (v == null ||
              v.findViewById(R.id.message_image) == null){
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.message_image, parent, false);
      }

      // the outer layout wraps the whole width and contains the inner layout
      // but is necessary for moving the message to the left or right
      RelativeLayout layoutOuter = (RelativeLayout) v.findViewById(R.id
              .message_image);
      // this layout actually holds the message
      RelativeLayout layoutInner = (RelativeLayout) v.findViewById(R.id
              .message_image_inner);
      // set the correct background
      layoutInner.setBackgroundResource(msgObj.left ? R.drawable.bubble_left :
              R.drawable.bubble_right);
      EmojiconTextView description = (EmojiconTextView) v.findViewById(R.id
              .message_image_description);
      // make the emojicons the correct size
      description.setExpandedSize(true);
      // set the correct text
      description.setText(msgObj.description);
//      // set the typeface
//      description.setTypeface(typeface);

      // the imageView containing the image
      ImageView imageView = (ImageView) v.findViewById(R.id
              .message_image_image);

      // the timeStamp
      TextView chatTime = (TextView) v.findViewById(R.id.message_image_timeStamp);
      // set the correct string representation for the time
      chatTime.setText(String.format(getContext().getResources().getString(R
              .string.time), msgObj.time));

      // this is the file name of the tempFile I create for showing while
      // loading the real image.
      // this image is quite small and will be loaded instantly.
      String tmpFileName = new File(getContext().getFilesDir(), msgObj.chatId
              + "-" + msgObj._ID + ".jpg").getAbsolutePath();
      if (msgObj.left){
        // align the message to the left
        layoutOuter.setGravity(Gravity.START);
        // disable the status imageView
        v.findViewById(R.id.message_image_status).setVisibility(View.GONE);
        if (MessageHistory.STATUS_RECEIVING.equals(msgObj.status)){
          // if I am receiving the image show the progress bar and make it
          // transparent
          v.findViewById(R.id.message_image_progress).setVisibility(View.VISIBLE);
          layoutInner.setAlpha(0.7f);
        }else{
          // otherwise, disable the progress bar and make it opaque
          v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
          layoutInner.setAlpha(1f);
          // also if reloadImage is true load the image in the background
          if (reloadImage)
            try{
              loadBitmap(new File(msgObj.file), imageView, tmpFileName);
            }catch (Exception e){
              e.printStackTrace();
            }
        }
      }else{
        // if reloadImage is true load the image in the background
        // as I am sending the image I will always have one to load
        if (reloadImage)
          try{
            loadBitmap(new File(msgObj.file), imageView, tmpFileName);
          }catch (Exception e){
            e.printStackTrace();
          }
        // set the gravity to the right
        layoutOuter.setGravity(Gravity.END);
        // show the status imageView
        v.findViewById(R.id.message_image_status).setVisibility(View.VISIBLE);
        // this needs to be done before the switch because in the switch one
        // variable can only be defined once, though in different cases
        // separated by a break
        ProgressBar progressBar;

        switch (msgObj.status){
          case MessageHistory.STATUS_WAITING:
            // if the image is waiting to be sent, show the hourglass and
            // make the progressbar with a progress of 1 visible
            imageView = (ImageView) v.findViewById(R.id.message_image_status);
            imageView.setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(1);
            // also make the message transparent
            layoutInner.setAlpha(0.7f);
            break;
          case MessageHistory.STATUS_SENDING:
            // if sending there will also be the hourglass and the
            // progressBar but the progressBar will now show the progress of
            // the msgObj
            imageView = (ImageView) v.findViewById(R.id.message_image_status);
            imageView.setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            progressBar = (ProgressBar) v.findViewById(R.id
                    .message_image_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(msgObj.progress);
            // also make the message transparent
            layoutInner.setAlpha(0.7f);
            break;
          case MessageHistory.STATUS_SENT:
            // show one grey hook
            imageView = (ImageView) v.findViewById(R.id.message_image_status);
            imageView.setImageResource(R.drawable.single_grey_hook);
            // disable the progress bar and make the message opaque
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_RECEIVED:
            // show two grey hooks
            imageView = (ImageView) v.findViewById(R.id.message_image_status);
            imageView.setImageResource(R.drawable.two_grey_hook);
            // disable the progress bar and make the message opaque
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
          case MessageHistory.STATUS_READ:
            // show two blue hooks
            imageView = (ImageView) v.findViewById(R.id.message_image_status);
            imageView.setImageResource(R.drawable.two_blue_hook);
            // disable the progress bar and make the message opaque
            v.findViewById(R.id.message_image_progress).setVisibility(View.GONE);
            layoutInner.setAlpha(1f);
            break;
        }
      }
    }else if (obj instanceof Date){
      // I don't care about recycling here, just inflate the correct layout
      // and set the correct text
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_date, parent, false);

      TextView date = (TextView) v.findViewById(R.id.date);
      // set the correct string representation
      date.setText(String.format(getContext().getResources().getString(R
              .string.date), ((Date) obj).date));
    }else if (obj instanceof LoadMoreMessages){
      // I don't care about recycling here, just inflate the correct layout
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_load_more, parent, false);
    }else if (obj instanceof NewMessage){
      // I don't care about recycling here, just inflate the correct layout
      LayoutInflater inflater = (LayoutInflater) this.getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.message_new_messages, parent, false);
      // show the correct text
      TextView nm = (TextView) v.findViewById(R.id.new_messages);
      nm.setText(((NewMessage) obj).status);
    }
    return v;
  }

  /**
   * Removes the object at the specified location from this {@code List}.
   *
   * @param position the index of the object to remove.
   * @return the removed object.
   * @throws UnsupportedOperationException if removing from this {@code MessageArrayAdapter} is not
   *                                       supported.
   * @throws IndexOutOfBoundsException     if {@code location < 0 || location >= size()}
   */
  public MessageArrayContent remove(int position) throws UnsupportedOperationException,
          IndexOutOfBoundsException{
    return messageList.remove(position);
  }

  /**
   * set the async drawable to the imageView for it to be loaded in the
   * background, while showing either the specified tempFile or if it doesn't
   * exists the placeholder drawable
   *
   * @param file         the image to be loaded in the background
   * @param imageView    the imageView to show the image
   * @param tempFileName the file containing the small image
   * @throws Exception if e.g. the tempFile is no image or anything else goes
   *                   wrong
   */
  private void loadBitmap(File file, ImageView imageView, String tempFileName)
          throws Exception{
    // first cancel all potential work being done on the imageView
    if (AsyncDrawable.cancelPotentialWork(file, imageView)){
      // the asyncDrawable needs the imageView and its width and height as task
      final AsyncDrawable.BitmapWorkerTask task =
              new AsyncDrawable.BitmapWorkerTask(imageView,
                      imageView.getLayoutParams().width,
                      imageView.getLayoutParams().height, true);
      // set the drawable as the asyncDrawable
      imageView.setImageDrawable(new AsyncDrawable(
              getContext().getResources(),
              // if the tempFile exists show it as placeHolder, otherwise
              // show the placeHolder imageResource as placeHolder
              new File(tempFileName).isFile()
                      ? BitmapFactory.decodeFile(tempFileName)
                      : BitmapFactory.decodeResource(getContext().getResources(),
                      R.drawable.placeholder),
              task));
      // start the loading
      task.execute(file);

    }
  }

  @Override
  public Iterator<MessageArrayContent> iterator(){
    // make the messageArrayAdapter iterable
    // returns an iterator that keeps track of where I am currently at
    return new Iterator<MessageArrayContent>(){

      // the current index, which is the index of the next item
      private int currentIndex = 0;

      @Override
      public boolean hasNext(){
        // if the currentIndex is smaller than the size and if the current
        // item is not null
        return currentIndex < messageList.size() && messageList.get
                (currentIndex) != null;
      }

      @Override
      public MessageArrayContent next(){
        // return the current item and increment currentIndex
        return messageList.get(currentIndex++);
      }

      @Override
      public void remove(){
        // no
        throw new UnsupportedOperationException();
      }
    };
  }
}
