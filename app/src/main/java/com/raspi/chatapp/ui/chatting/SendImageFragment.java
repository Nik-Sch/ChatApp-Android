package com.raspi.chatapp.ui.chatting;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.github.ankushsachdeva.emojicon.EmojiconEditText;
import com.github.ankushsachdeva.emojicon.EmojiconGridView;
import com.github.ankushsachdeva.emojicon.EmojiconsPopup;
import com.github.ankushsachdeva.emojicon.emoji.Emojicon;
import com.raspi.chatapp.R;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.FileUtils;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SendImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SendImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendImageFragment extends Fragment{

  private Uri imageUri;
  private String buddyId;
  private String name;
  private ActionBar actionBar;

  private OnFragmentInteractionListener mListener;

  public SendImageFragment(){
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param imageUri the uri of the image that should be sent.
   * @param buddyId  the buddyId to whom the image should be sent
   * @return A new instance of fragment SendImageFragment.
   */
  public static SendImageFragment newInstance(String imageUri, String
          buddyId, String name){
    SendImageFragment fragment = new SendImageFragment();
    Bundle args = new Bundle();
    args.putString(Constants.IMAGE_URI, imageUri);
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
      imageUri = Uri.parse(getArguments().getString(Constants.IMAGE_URI));
      // the buddyId to whom to send the image
      buddyId = getArguments().getString(Constants.BUDDY_ID);
      // and the name of the chat for showing in the actionBar
      name = getArguments().getString(Constants.CHAT_NAME);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_send_image, container, false);
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

  /**
   * this function will initialize the ui and reload everything to make sure
   * it is shown correctly
   */
  private void initUI(){
    // set the actionBar title and subtitle
    if (actionBar != null){
      actionBar.setTitle(R.string.send_image);
      actionBar.setSubtitle(name);
    }
    initEmoji();

    // nope this doesn't work with xml attributes because. It is just for the
    // editText to scroll vertically instead of horizontally
    EditText et = (EditText) getView().findViewById(R.id
            .send_image_description);
    et.setHorizontallyScrolling(false);
    et.setMaxLines(3);

    // load the image in the background
    ImageView imageView = ((ImageView) getView().findViewById(R.id
            .send_image_image));
    new Thread(new LoadImageRunnable(imageView, new Handler())).start();

    //Cancel button pressed
    getView().findViewById(R.id.send_image_cancel)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                // just return to the chatFragment
                mListener.onReturnClick();
              }
            });
    //Send button pressed
    getView().findViewById(R.id.send_image_send)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                // create the progressDialog that is to be shown while saving
                // the image
                ProgressDialog progressDialog = new ProgressDialog
                        (getContext());
                progressDialog.setTitle(R.string.sending_image);
                // run the sendImage in a new thread because I am saving the
                // image and this should be done in a new thread
                new Thread(new SendImageRunnable(new Handler(), getContext(),
                        progressDialog)).start();
              }
            });
  }

  /**
   * initialize the emojiconKeyboard
   */
  private void initEmoji(){
    // this is the same as in ChatFragment --> look there for documentation
    final EmojiconEditText emojiconEditText = (EmojiconEditText) getActivity
            ().findViewById(R.id.send_image_description);
    final View root = getActivity().findViewById(R.id.root_view);
    final EmojiconsPopup popup = new EmojiconsPopup(root, getActivity());
    final ImageButton emojiBtn = (ImageButton) getActivity().findViewById(R
            .id.send_image_emoti_switch);
    popup.setSizeForSoftKeyboard();
    popup.setOnDismissListener(new PopupWindow.OnDismissListener(){
      @Override
      public void onDismiss(){
//        changeKeyboardIcon();
      }
    });
    popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener(){
      @Override
      public void onKeyboardOpen(int keyBoardHeight){
      }

      @Override
      public void onKeyboardClose(){
        if (popup.isShowing())
          popup.dismiss();
      }
    });
    popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener(){
      @Override
      public void onEmojiconClicked(Emojicon emojicon){
        if (emojiconEditText == null || emojicon == null)
          return;
        int start = emojiconEditText.getSelectionStart();
        int end = emojiconEditText.getSelectionEnd();
        if (start < 0)
          emojiconEditText.append(emojicon.getEmoji());
        else
          emojiconEditText.getText().replace(
                  Math.min(start, end),
                  Math.max(start, end),
                  emojicon.getEmoji(),
                  0,
                  emojicon.getEmoji().length());
      }
    });
    popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener(){
      @Override
      public void onEmojiconBackspaceClicked(View v){
        KeyEvent event = new KeyEvent(
                0, 0, 0,
                KeyEvent.KEYCODE_DEL,
                0, 0, 0, 0,
                KeyEvent.KEYCODE_ENDCALL
        );
        emojiconEditText.dispatchKeyEvent(event);
      }
    });
    emojiBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        if (!popup.isShowing()){
          if (popup.isKeyBoardOpen())
            popup.showAtBottom();
          else{
            emojiconEditText.setFocusableInTouchMode(true);
            emojiconEditText.requestFocus();
            popup.showAtBottomPending();
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getActivity().getSystemService(Context
                            .INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(emojiconEditText,
                    InputMethodManager.SHOW_IMPLICIT);
          }
        }else
          popup.dismiss();
      }
    });
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
   * calculates the sampleSize of the image
   *
   * @param options   the options of the Bitmap
   * @param reqWidth  the width you want the image in
   * @param reqHeight the height you want the image in
   * @return the sample size which is preferred for loading the image for the
   * given width and height
   */
  private int calculateInSampleSize(
          BitmapFactory.Options options, int reqWidth, int reqHeight){
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    // if I actually want to sample more
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

  private class LoadImageRunnable implements Runnable{
    Handler mHandler;
    ImageView imageView;

    public LoadImageRunnable(ImageView imageView, Handler mHandler){
      this.mHandler = mHandler;
      this.imageView = imageView;
    }

    @Override
    public void run(){
      final String imagePath = FileUtils.getPath(getContext(), imageUri);

      // First decode with inJustDecodeBounds=true to check dimensions
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(imagePath, options);

      // Calculate inSampleSize
      DisplayMetrics d = new DisplayMetrics();
      ((WindowManager) getContext().getSystemService(Context
              .WINDOW_SERVICE)).getDefaultDisplay().getMetrics(d);
      options.inSampleSize = calculateInSampleSize(options, d.widthPixels / 2, d
              .heightPixels / 2);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      mHandler.post(new Runnable(){
        @Override
        public void run(){
          imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath, options));
        }
      });
    }
  }

  /**
   * this runnable sends the image, shows while saving the image a
   * progressDialog and afterwards clicks return
   */
  private class SendImageRunnable implements Runnable{

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
    public SendImageRunnable(Handler mHandler, Context context,
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
      MyFileUtils mfu = new MyFileUtils();
      if (mfu.isExternalStorageWritable()){
        try{
          //creating the directory
          File file = mfu.getFileName();
          //creating the file
          file.createNewFile();
          //save the given image into a new file
          saveImage(FileUtils.getPath(context, imageUri), file);
          //adding the image message to the messageHistory
          JSONArray contentJSON = createJSON(file.getAbsolutePath(), (
                  (TextView) getView().findViewById(R.id
                          .send_image_description)).getText().toString());
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
        }catch (Exception e){
          e.printStackTrace();
        }finally{
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
}
