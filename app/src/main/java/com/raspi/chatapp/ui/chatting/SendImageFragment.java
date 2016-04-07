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
package com.raspi.chatapp.ui.chatting;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.ortiz.touch.TouchImageView;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.emojicon.EmojiconEditText;
import com.raspi.chatapp.ui.util.emojicon.EmojiconGridView;
import com.raspi.chatapp.ui.util.emojicon.EmojiconPopup;
import com.raspi.chatapp.ui.util.emojicon.emoji.Emojicon;
import com.raspi.chatapp.ui.util.image.AsyncDrawable;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.FileUtils;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SendImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SendImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendImageFragment extends Fragment{

  private static final int ADD_PHOTO_CLICKED = 542;
  private ArrayList<Message> images;
  private ViewPager viewPager;
  private String buddyId;
  private String name;
  private ActionBar actionBar;
  private int current = 0;
  private boolean keyboardShown = false;

  private OnFragmentInteractionListener mListener;

  public SendImageFragment(){
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param buddyId   the buddyId to whom the image should be sent
   * @param name      the name of the chat
   * @param imageUris the imageUris representing the images that are added
   * @return A new instance of fragment SendImageFragment.
   */
  public static SendImageFragment newInstance(String buddyId,
                                              String name,
                                              Parcelable... imageUris){
    SendImageFragment fragment = new SendImageFragment();
    Bundle args = new Bundle();
    args.putParcelableArray(Constants.IMAGE_URI, imageUris);
    args.putString(Constants.BUDDY_ID, buddyId);
    args.putString(Constants.CHAT_NAME, name);
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * creates a JSON String with the path as first item and the description as
   * second.
   *
   * @param path the first parameter to put into the JSON
   * @param desc the second parameter to put into the JSON
   * @return
   */
  public static JSONArray createJSON(String path, String desc){
    JSONArray contentJSON = new JSONArray();
    contentJSON.put(path);
    contentJSON.put(desc);
    return contentJSON;
  }

  @Override
  public void onResume(){
    super.onResume();
    // get the actionBar and initialize the ui
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    initUI();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    // retrieve the important data
    if (getArguments() != null){
      // the image to be sent
      images = parcelableToMessageArrayList(Arrays.asList(getArguments()
              .getParcelableArray(Constants.IMAGE_URI)));
      // the buddyId to whom to send the image
      buddyId = getArguments().getString(Constants.BUDDY_ID);
      // and the name of the chat for showing in the actionBar
      name = getArguments().getString(Constants.CHAT_NAME);
    }
  }

  private ArrayList<Message> parcelableToMessageArrayList(List<Parcelable>
                                                                  parcelables){
    ArrayList<Message> result = new ArrayList<>();
    for (Parcelable p : parcelables)
      result.add(new Message((Uri) p));
    return result;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    // if the user chose a image to be sent to the current buddy
    if (requestCode == ADD_PHOTO_CLICKED && resultCode ==
            Activity.RESULT_OK){
      if (data.getData() != null){
        // one image was selected
        Message msg = new Message(data.getData());
        images.add(msg);
        current = images.size() - 1;
      }else if (data.getClipData() != null){
        // multiple images were selected
        ClipData clipData = data.getClipData();
        for (int i = 0; i < clipData.getItemCount(); i++)
          images.add(new Message(clipData.getItemAt(i).getUri()));
        current = images.size() - 1;
      }
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_send_image, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
    menu.clear();
    inflater.inflate(R.menu.menu_send_image, menu);
  }

  @Override
  public void onAttach(Context context){
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener){
      mListener = (OnFragmentInteractionListener) context;
    }else{
      throw new RuntimeException(context.toString()
              + " must implement OnChatFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach(){
    super.onDetach();
    mListener = null;
  }

  @Override
  public void onPause(){
    super.onPause();
    actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor
            (R.color.colorPrimary)));
    // set the statusBar color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getActivity().getWindow().setStatusBarColor(getResources().getColor(R
              .color.colorPrimaryDark));
    getActivity().getWindow().getDecorView().setSystemUiVisibility(View
            .SYSTEM_UI_FLAG_VISIBLE);
  }

  /**
   * this function will initialize the ui showing the current image and reload
   * everything to make sure it is shown correctly
   */
  private void initUI(){
    // set the actionBar title and subtitle
    if (actionBar != null){
      actionBar.setTitle(R.string.send_image);
      actionBar.setSubtitle(name);
//      actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor
//              (R.color.action_bar_transparent)));
//      // set the statusBar color
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//        getActivity().getWindow().setStatusBarColor(getResources().getColor(R
//                .color.action_bar_transparent));
    }

    // instantiate the ViewPager
    viewPager = (ViewPager) getActivity().findViewById(R.id
            .send_image_view_pager);
    viewPager.setAdapter(new MyPagerAdapter());
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
      }

      @Override
      public void onPageSelected(int position){
        changePage(position, false, false);
      }

      @Override
      public void onPageScrollStateChanged(int state){
      }
    });

    //Cancel button pressed
    getActivity().findViewById(R.id.send_image_cancel)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                // just return to the chatFragment
                mListener.onReturnClick();
              }
            });
    //Send button pressed
    getActivity().findViewById(R.id.send_image_send)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                // create the progressDialog that is to be shown while saving
                // the image
                ProgressDialog progressDialog = new ProgressDialog
                        (getContext());
                if (images.size() > 1)
                  progressDialog.setTitle(String.format(getResources().getString(R.string
                          .sending_images), images.size()));
                else
                  progressDialog.setTitle(R.string.sending_image);
                // run the sendImage in a new thread because I am saving the
                // image and this should be done in a new thread
                new Thread(new SendImagesRunnable(new Handler(), getContext(),
                        progressDialog)).start();
              }
            });


    // generate the overview only if there are at least 2 images
    if (images.size() > 1){
      // the layoutParams for the imageView which has the following attributes:
      // width = height = 65dp
      // margin = 5dp
      getActivity().findViewById(R.id.send_image_overview).setVisibility(View.VISIBLE);
      int a = Constants.dipToPixel(getContext(), 65);
      RelativeLayout.LayoutParams imageViewParams = new RelativeLayout.LayoutParams
              (a, a);
      int b = Constants.dipToPixel(getContext(), 5);
      imageViewParams.setMargins(b, b, b, b);
      // the layoutParams for the backgroundView which has the following
      // attributes:
      // width = height = 71dp
      // margin = 2dp
      int c = Constants.dipToPixel(getContext(), 71);
      RelativeLayout.LayoutParams backgroundParams = new RelativeLayout
              .LayoutParams(c, c);
      int d = Constants.dipToPixel(getContext(), 2);
      backgroundParams.setMargins(d, d, d, d);
      // the layoutParams for the relativeLayout containing the image and
      // the background which has the following attributes:
      // width = height = wrap_content
      LinearLayout.LayoutParams relativeLayoutParams = new LinearLayout
              .LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id
              .send_image_overview_content);
      linearLayout.removeAllViewsInLayout();
      int i = 0;
      for (Message msg : images){
        // set up the imageView
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(imageViewParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // load the bitmap async
        AsyncDrawable.BitmapWorkerTask bitmapWorker = new AsyncDrawable
                .BitmapWorkerTask(imageView, a, a, true);
        imageView.setImageDrawable(new AsyncDrawable(getResources(),
                null, bitmapWorker));
        imageView.setOnClickListener(new overviewSelectListener(i++));
        bitmapWorker.execute(FileUtils.getFile(getContext(), msg.getImageUri
                ()));
        // set up the background
        View background = new View(getContext());
        background.setLayoutParams(backgroundParams);
        background.setBackgroundColor(getActivity().getResources().getColor(R.color
                .colorPrimaryDark));
        // make it invisible in the beginning
        background.setVisibility(View.GONE);

        // create the relativeLayout containing them
        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setLayoutParams(relativeLayoutParams);
        relativeLayout.addView(background);
        relativeLayout.addView(imageView);
        // combination of Message and overviewViews
        msg.setLayout(relativeLayout);
        msg.setBackground(background);
        linearLayout.addView(relativeLayout);
      }
    }else
      getActivity().findViewById(R.id.send_image_overview).setVisibility(View.GONE);
    changePage(current, true, true);
    showSystemUI();
  }

  /**
   * changes the page.
   *
   * @param position the page to go to.
   * @param clicked  if true, the viewPager is set to the position
   */
  private void changePage(final int position, boolean clicked, boolean force){
    if (images.size() > 1 && (force || current != position)){
      Log.d("SEND_IMAGE", "page changed");
      images.get(current).getBackground().setVisibility(View.GONE);
      images.get(position).getBackground().setVisibility(View.VISIBLE);
      current = position;
      // scroll to correct position
      HorizontalScrollView scrollView = (HorizontalScrollView) getActivity()
              .findViewById(R.id.send_image_overview);
      View view = images.get(current).getLayout();
      int vLeft = view.getLeft();
      int vRight = view.getRight();
      int sWidth = scrollView.getWidth();
      if (!isViewVisible(scrollView, view))
        scrollView.smoothScrollTo((vLeft + vRight - sWidth) / 2, 0);
      if (clicked)
        // setting the viewPagers item is posted via a handler, so the gui
        // thread finishes the scrollView related task before switching the
        // viewPager. That way it seems smoother because the new image is
        // selected instantly.
        new Handler().post(new Runnable(){
          @Override
          public void run(){
            viewPager.setCurrentItem(position);
          }
        });
    }
  }

  private boolean isViewVisible(HorizontalScrollView scrollView, View view){
    Rect scrollBounds = new Rect();
    scrollView.getDrawingRect(scrollBounds);
    float vLeft = view.getLeft();
    float vRight = view.getWidth() + vLeft;
    return scrollBounds.left < vLeft && scrollBounds.right > vRight;
  }

  /**
   * initialize the emojiconKeyboard
   */
  private void initEmoji(View view){
// save the views I will use
    final EmojiconEditText emojiconEditText = (EmojiconEditText) view.findViewById(R.id
            .send_image_description);
    final ImageButton emojiBtn = (ImageButton) view.findViewById(R
            .id.send_image_emoti_switch);
    final EmojiconPopup popup = new EmojiconPopup(getActivity().findViewById(R.id.root_view),
            getContext(), new EmojiconGridView.OnEmojiconClickedListener(){
      @Override
      public void OnEmojiconClicked(Emojicon emojicon){
        if (emojiconEditText == null || emojicon == null)
          return;
        int start = emojiconEditText.getSelectionStart();
        int end = emojiconEditText.getSelectionEnd();
        if (start < 0)
          emojiconEditText.append(emojicon.getEmoji());
        else
          emojiconEditText.getText().replace(Math.min(start, end),
                  Math.max(start, end),
                  emojicon.getEmoji(),
                  0,
                  emojicon.getEmoji().length());
      }
    });
    popup.setSoftKeyboardSize();

    popup.setOnSoftKeyboardOpenCloseListener(new EmojiconPopup.OnSoftKeyboardOpenCloseListener(){
      @Override
      public void onKeyboardOpen(int keyboardHeight){
        keyboardOpened();
      }

      @Override
      public void onKeyboardClose(){
        if (popup.isShowing())
          popup.dismiss();
        keyboardClosed();
      }
    });
    // open/close the emojicon keyboard when pressing the button
    emojiBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        if (!popup.isShowing()){
          if (popup.isKeyboardOpen())
            popup.showAtBottom();
          else{
            emojiconEditText.setFocusableInTouchMode(true);
            emojiconEditText.requestFocus();
            popup.showAtBottomPending();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            imm.showSoftInput(emojiconEditText, InputMethodManager.SHOW_IMPLICIT);
          }
        }else
          popup.dismiss();
      }
    });

    popup.setOnEmojiconBackspaceClickedListener(new EmojiconPopup.OnEmojiconBackspaceClickedListener(){
      @Override
      public void onEmojiconBackspaceClicked(View view){
        emojiconEditText.dispatchKeyEvent(
                new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL));
      }
    });
  }

  private void keyboardClosed(){
    if (keyboardShown){
      try{
        showSystemUI();
        View buttons = getActivity().findViewById(R.id.send_image_buttons);
        View viewPager = getActivity().findViewById(R.id
                .send_image_view_pager);
        RelativeLayout.LayoutParams viewPagerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        viewPagerParams.addRule(RelativeLayout.ABOVE, R.id.send_image_overview);
        viewPagerParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        viewPagerParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        viewPager.setLayoutParams(viewPagerParams);
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        images.get(current).getImageLayout().setLayoutParams(imageParams);
        if (images.size() > 1)
          getActivity().findViewById(R.id.send_image_overview).setVisibility(View
                  .VISIBLE);
        buttons.setVisibility(View.VISIBLE);
      }catch (Exception e){
        e.printStackTrace();
      }
      keyboardShown = false;
    }
  }

  private void keyboardOpened(){
    if (!keyboardShown){
      try{
        View buttons = getActivity().findViewById(R.id.send_image_buttons);
        View viewPager = getActivity().findViewById(R.id
                .send_image_view_pager);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        viewPager.setLayoutParams(params);
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        imageParams.addRule(RelativeLayout.ABOVE, R.id.send_image_description_layout);
        images.get(current).getImageLayout().setLayoutParams(imageParams);
        if (images.size() > 1)
          getActivity().findViewById(R.id.send_image_overview).setVisibility
                  (View.GONE);
        buttons.setVisibility(View.GONE);
        hideSystemUI();
      }catch (Exception e){
        e.printStackTrace();
      }
      keyboardShown = true;
    }
  }

  private void showSystemUI(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getActivity().getWindow().setStatusBarColor(getResources().getColor(R
              .color.colorPrimaryDark));
    actionBar.show();
  }

  private void hideSystemUI(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getActivity().getWindow().setStatusBarColor(getResources().getColor
              (android.R.color.black));
    actionBar.hide();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      case android.R.id.home:
        mListener.onReturnClick();
        return true;
      case R.id.add_image:
        addImage();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void addImage(){

    // when clicking attack the user should at first select an application to
    // choose the image with and then choose an image.
    // this intent is for getting the image
    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
    getIntent.setType("image/*");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
      getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

    // and this for getting the application to get the image with
    Intent pickIntent = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    pickIntent.setType("image/*");

    // and this finally is for opening the chooserIntent for opening the
    // getIntent for returning the image uri. Yep, thanks android
    Intent chooserIntent = Intent.createChooser(getIntent, getResources()
            .getString(R.string.select_image));
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new
            Intent[]{pickIntent});
    startActivityForResult(chooserIntent, ADD_PHOTO_CLICKED);
    // nope I don't want to be asked for a pwd when selected the image
    getActivity().getSharedPreferences(Constants.PREFERENCES, 0).edit()
            .putBoolean(Constants.PWD_REQUEST, false).apply();
  }

  /**
   * saves the image in own image folder
   *
   * @param sourcePath the path of the image the user selected
   * @param destFile   the file where it should be saved
   * @throws IOException
   */
  private void saveImage(String sourcePath, File destFile) throws IOException{
    // get the output stream
    OutputStream out = new FileOutputStream(destFile);
    // create the options
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDensity = 96;
    Bitmap image = BitmapFactory.decodeFile(sourcePath, options);
    // and compress the image into the file
    image.compress(Bitmap.CompressFormat.JPEG, 42, out);
    out.close();
    getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            Uri.parse("file://" + destFile.getAbsolutePath())));
  }

  /**
   * saves a copy in the internal storage. This may be used for displaying when
   * rendering the real image in background. As this is image is really low
   * quality it is loaded instantly (size is a couple bytes) you can load
   * this in the main thread while the background thread will do the heavy
   * loading task.
   *
   * @param context      the context used to get the localStorage
   * @param fileLocation the location of the file that is to be saved
   * @param id           the messageId of the imageMessage, for the file name
   * @param chatId       the chatId of the chat the image is from, for the file name
   */
  private void saveImageCopy(Context context, String fileLocation, Long id,
                             String chatId){
    try{
      //old images bitmap
      Bitmap oldImg = BitmapFactory.decodeFile(fileLocation);
      // sample the height to 50 and maintain the aspect ratio
      float height = 50;
      float x = oldImg.getHeight() / height;
      float width = oldImg.getWidth() / x;

      // generate the destination file
      File destFile = new File(context.getFilesDir(), chatId + "-" +
              id + ".jpg");
      OutputStream out = new FileOutputStream(destFile);
      // generate the bitmap to compress to file
      Bitmap image = Bitmap.createScaledBitmap(oldImg, (int) width, (int)
              height, true);
      // compress the bitmap to file
      image.compress(Bitmap.CompressFormat.JPEG, 20, out);
      out.close();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * This interface must be implemented by ui that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other chatting contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener{
    void onReturnClick();
  }

  /**
   * this runnable sends the image, shows while saving the image a
   * progressDialog and afterwards clicks return
   */
  private class SendImagesRunnable implements Runnable{

    Handler mHandler;
    Context context;
    ProgressDialog progressDialog;

    /**
     * creates a runnable that saves the image
     *
     * @param mHandler       the handler to perform operation with the progressDialog
     * @param context        the context
     * @param progressDialog the progressDialog to show when saving the image
     */
    public SendImagesRunnable(Handler mHandler, Context context,
                              ProgressDialog progressDialog){
      this.mHandler = mHandler;
      this.context = context;
      this.progressDialog = progressDialog;
    }

    @Override
    public void run(){
      // show the progressDialog
      mHandler.post(new Runnable(){
        @Override
        public void run(){
          progressDialog.show();
        }
      });
      // can only send the image if I can save it locally in the external Storage
      if (MyFileUtils.isExternalStorageWritable()){
        try{
          for (Message msg : images){
            //creating the directory
            File file = MyFileUtils.getFileName();
            //creating the file
            file.createNewFile();
            //save the given image into a new file
            saveImage(FileUtils.getPath(context, msg.getImageUri()), file);
            //adding the image message to the messageHistory
            JSONArray contentJSON = createJSON(file.getAbsolutePath(),
                    msg.getDescription());
            MessageHistory messageHistory = new MessageHistory(getContext());
            long id = messageHistory.addMessage(
                    buddyId,
                    // I send it by myself --> get my username
                    getActivity().getSharedPreferences(Constants.PREFERENCES, 0)
                            .getString(Constants.USERNAME, ""),
                    MessageHistory.TYPE_IMAGE,
                    contentJSON.toString(),
                    MessageHistory.STATUS_WAITING,
                    -1);
            // also make sure to save a down sampled copy of the image in
            // localStorage for fast rendering in the chatFragment
            saveImageCopy(getContext(), file.getAbsolutePath(), id, buddyId);
          }
        }catch (Exception e){
          e.printStackTrace();
        }finally {
          // dismiss the dialog
          mHandler.post(new Runnable(){
            @Override
            public void run(){
              progressDialog.dismiss();
            }
          });
          // return to the chatFragment
          mListener.onReturnClick();
        }
      }
    }
  }

  private class overviewSelectListener implements View.OnClickListener{
    final int i;

    public overviewSelectListener(int i){
      this.i = i;
    }

    @Override
    public void onClick(View v){
      changePage(i, true, false);
    }
  }

  private class Message{
    /**
     * the uri of the image to be sent
     */
    private Uri imageUri = null;
    /**
     * the layout containing the imageView
     */
    private LinearLayout imageLayout = null;
    /**
     * the description of the image
     */
    private String description = "";
    /**
     * the layout containing the preview image in the overview
     */
    private RelativeLayout layout = null;
    /**
     * the background image of the preview image in the overview (the active
     * images backgroun is visible and blue, showing the blue border)
     */
    private View background = null;

    public Message(){
    }

    public Message(Uri imageUri){
      this.imageUri = imageUri;
    }

    public Message(String description){
      this.description = description;
    }

    public Uri getImageUri(){
      return imageUri;
    }

    public void setImageUri(Uri imageUri){
      this.imageUri = imageUri;
    }

    public String getDescription(){
      return description;
    }

    public void setDescription(String description){
      this.description = description;
    }

    public View getBackground(){
      return background;
    }

    public void setBackground(View background){
      this.background = background;
    }

    public RelativeLayout getLayout(){
      return layout;
    }

    public void setLayout(RelativeLayout layout){
      this.layout = layout;
    }

    public LinearLayout getImageLayout(){
      return imageLayout;
    }

    public void setImageLayout(LinearLayout imageLayout){
      this.imageLayout = imageLayout;
    }
  }

  private class MyPagerAdapter extends PagerAdapter{
    @Override
    public int getCount(){
      return images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object){
      return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
      container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position){
      final Message msg = images.get(position);

      // inflate the layout
      LayoutInflater inflater = LayoutInflater.from(getContext());
      View view = inflater.inflate(R.layout.send_image_view_pager_content, container,
              false);
      // init the emoji Button
      initEmoji(view);

      // add a textChangedListener to the editText
      EditText editText = (EditText) view.findViewById(R.id
              .send_image_description);
      editText.addTextChangedListener(new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after){
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){
        }

        @Override
        public void afterTextChanged(Editable s){
          msg.setDescription(s.toString());
        }
      });
      // nope this doesn't work with xml attributes because. It is just for the
      // editText to scroll vertically instead of horizontally
      editText.setHorizontallyScrolling(false);
      editText.setMaxLines(3);
      // set the current text if there is one
      editText.setText(msg.getDescription());

      // work with the image
      String imagePath = FileUtils.getPath(getContext(), msg.getImageUri());
      TouchImageView imageView = (TouchImageView) view.findViewById(R.id
              .send_image_image);
      // decode the image properly
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(imagePath, options);

      DisplayMetrics metrics = new DisplayMetrics();
      getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
      // Calculate inSampleSize
      options.inSampleSize = AsyncDrawable.calculateInSampleSize(options,
              metrics.widthPixels, metrics.heightPixels, true);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
      if (bitmap == null)
        return null;
      Log.d("loadBitmap", "Dimensions: " + bitmap.getWidth() + ", " +
              bitmap.getHeight());
      imageView.setImageBitmap(bitmap);

      // set the imageViewLayout
      msg.setImageLayout((LinearLayout) view.findViewById(R.id
              .send_image_image_layout));

      // finally add the view
      container.addView(view, 0);
      return view;
    }
  }

  private int getUsableScreenHeight(View rootView){
    try{
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        DisplayMetrics metrics = new DisplayMetrics();

        WindowManager windowManager = (WindowManager)
                getActivity().getSystemService(Context
                        .WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        return metrics.heightPixels;

      }else
        return rootView.getRootView().getHeight();
    }catch (Exception e){
      e.printStackTrace();
    }
    return rootView.getRootView().getHeight();
  }
}
