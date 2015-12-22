package com.raspi.chatapp.activities.fragments;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.activities.MainActivity;
import com.raspi.chatapp.sqlite.MessageHistory;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

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
  public static SendImageFragment newInstance(String imageUri, String buddyId){
    SendImageFragment fragment = new SendImageFragment();
    Bundle args = new Bundle();
    args.putString(MainActivity.IMAGE_URI, imageUri);
    args.putString(MainActivity.BUDDY_ID, buddyId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onResume(){
    super.onResume();
    initUI();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (getArguments() != null){
      imageUri = Uri.parse(getArguments().getString(MainActivity.IMAGE_URI));
      buddyId = getArguments().getString(MainActivity.BUDDY_ID);
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
    ((ImageView) getView().findViewById(R.id
            .send_image_image)).setImageURI(imageUri);
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
    if (isExternalStorageWritable()){
      String root = Environment.getExternalStoragePublicDirectory
              (Environment.DIRECTORY_PICTURES).toString();
      File myDir = new File(root + "/" + MainActivity.SENT_FILE_DIR);
      myDir.mkdirs();
      File file = new File(myDir, getFileName());
      try{
        if (file.exists()) file.delete();
        file.createNewFile();
        copyFile(new File(getRealPathFromURI(imageUri)), file);
        JSONArray contentJSON = new JSONArray();
        contentJSON.put(file.getAbsolutePath());
        contentJSON.put(((TextView) getView().findViewById(R.id
                .message_image_description)).getText());
        contentJSON.put(0d);//progress
        MessageHistory messageHistory = new MessageHistory(getContext());
        messageHistory.addMessage(
                buddyId,
                getActivity().getSharedPreferences(MainActivity.PREFERENCES, 0)
                        .getString(MainActivity.USERNAME, ""),
                MessageHistory.TYPE_IMAGE,
                contentJSON.toString(),
                MessageHistory.STATUS_WAITING);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  private void copyFile(File sourceFile, File destFile) throws IOException{
    if (!sourceFile.exists()) return;
    FileChannel source = new FileInputStream(sourceFile).getChannel();
    FileChannel dest = new FileInputStream(destFile).getChannel();
    if (source != null && dest != null)
      dest.transferFrom(source, 0, source.size());
    if (source != null) source.close();
    if (dest != null) dest.close();
  }

  private String getRealPathFromURI(Uri contentUri){
    String[] proj = {MediaStore.Images.Media.DATA};
    CursorLoader loader = new CursorLoader(getContext(), contentUri, proj,
            null, null, null);
    Cursor cursor = loader.loadInBackground();
    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media
            .DATA);
    cursor.moveToFirst();
    String result = cursor.getString(columnIndex);
    cursor.close();
    return result;
  }

  private String getFileName(){
    return "IMG_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date
            ()) + ".jpg";
  }

  public boolean isExternalStorageWritable(){
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)){
      return true;
    }
    return false;
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
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
