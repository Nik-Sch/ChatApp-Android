package com.raspi.chatapp.ui.chatting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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

  public static JSONArray createJSON(String path, String desc){
    JSONArray contentJSON = new JSONArray();
    contentJSON.put(path);
    contentJSON.put(desc);
    return contentJSON;
  }

  @Override
  public void onResume(){
    super.onResume();
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    initUI();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (getArguments() != null){
      imageUri = Uri.parse(getArguments().getString(Constants.IMAGE_URI));
      buddyId = getArguments().getString(Constants.BUDDY_ID);
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
              + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach(){
    super.onDetach();
    mListener = null;
  }

  private void initUI(){
    if (actionBar != null){
      actionBar.setTitle(R.string.send_image);
      actionBar.setSubtitle(name);
    }
    createEmoji();

    EditText et = (EditText) getView().findViewById(R.id
            .send_image_description);
    et.setHorizontallyScrolling(false);
    et.setMaxLines(3);

    ImageView imageView = ((ImageView) getView().findViewById(R.id
            .send_image_image));
    String imagePath = FileUtils.getPath(getContext(), imageUri);

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
    imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath, options));

    //Cancel button pressed
    getView().findViewById(R.id.send_image_cancel)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                mListener.onReturnClick();
              }
            });
    //Send button pressed
    getView().findViewById(R.id.send_image_send)
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                // this should probably be done in a seperate thread but I
                // don't care atm
                sendImage();
                mListener.onReturnClick();
              }
            });
  }

  private void createEmoji(){
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


  private void sendImage(){
    MyFileUtils mfu = new MyFileUtils();
    if (mfu.isExternalStorageWritable()){
      try{
        //creating the directory
        File file = mfu.getFileName();
        //creating the file
        file.createNewFile();
        //moving the given image into the file
        copyImage(FileUtils.getPath(getContext(), imageUri), file);
        //adding the image message to the messageHistory
        JSONArray contentJSON = createJSON(file.getAbsolutePath(), (
                (TextView) getView().findViewById(R.id
                        .send_image_description)).getText().toString());
        MessageHistory messageHistory = new MessageHistory(getContext());
        messageHistory.addMessage(
                buddyId,
                getActivity().getSharedPreferences(Constants.PREFERENCES, 0)
                        .getString(Constants.USERNAME, ""),
                MessageHistory.TYPE_IMAGE,
                contentJSON.toString(),
                MessageHistory.STATUS_WAITING,
                -1);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  private void copyImage(String sourcePath, File destFile) throws IOException{
    OutputStream out = new FileOutputStream(destFile);
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDensity = 96;
    Bitmap image = BitmapFactory.decodeFile(sourcePath, options);
    image.compress(Bitmap.CompressFormat.JPEG, 42, out);
    out.close();
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
}
