package com.raspi.chatapp.ui.chatting;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.FileUtils;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    args.putString(ChatActivity.IMAGE_URI, imageUri);
    args.putString(ChatActivity.BUDDY_ID, buddyId);
    args.putString(ChatActivity.CHAT_NAME, name);
    fragment.setArguments(args);
    return fragment;
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
      imageUri = Uri.parse(getArguments().getString(ChatActivity.IMAGE_URI));
      buddyId = getArguments().getString(ChatActivity.BUDDY_ID);
      name = getArguments().getString(ChatActivity.CHAT_NAME);
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
                sendImage();
                mListener.onReturnClick();
              }
            });
  }

  /*
  USER SPECIFIC FUNCTIONS
   */

  private void sendImage(){
    MyFileUtils mfu = new MyFileUtils();
    if (mfu.isExternalStorageWritable()){
      try{
        //creating the directory
       File file = mfu.getFileName();
        //creating the file
        file.createNewFile();
        //moving the given image into the file
        copyFile(FileUtils.getFile(getContext(), imageUri), file);
        //adding the image message to the messageHistory
        JSONArray contentJSON = createJSON(file.getAbsolutePath(), (
                (TextView) getView().findViewById(R.id
                        .send_image_description)).getText().toString());
        MessageHistory messageHistory = new MessageHistory(getContext());
        messageHistory.addMessage(
                buddyId,
                getActivity().getSharedPreferences(ChatActivity.PREFERENCES, 0)
                        .getString(ChatActivity.USERNAME, ""),
                MessageHistory.TYPE_IMAGE,
                contentJSON.toString(),
                MessageHistory.STATUS_WAITING, -1);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  public static JSONArray createJSON(String path, String desc){
    JSONArray contentJSON = new JSONArray();
    contentJSON.put(path);
    contentJSON.put(desc);
    return contentJSON;
  }

  private void copyFile(File sourceFile, File destFile) throws IOException{
    InputStream in = new FileInputStream(sourceFile);
    OutputStream out = new FileOutputStream(destFile);

    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0)
      out.write(buf, 0, len);
    in.close();
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
