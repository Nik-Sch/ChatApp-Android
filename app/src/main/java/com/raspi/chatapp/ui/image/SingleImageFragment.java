package com.raspi.chatapp.ui.image;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.LoadImageRunnable;
import com.raspi.chatapp.ui.util.OnSwipeTouchListener;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SingleImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SingleImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SingleImageFragment extends Fragment{

  private String chatId;
  private long messageId;
  private boolean overlayActive = true;

  private OnFragmentInteractionListener mListener;

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param chatId    the chatId of the chat the image belongs to
   * @param messageId the messageId of message in the chat
   * @return A new instance of fragment SingleImageFragment.
   */
  public static SingleImageFragment newInstance(String chatId, long messageId){
    SingleImageFragment fragment = new SingleImageFragment();
    Bundle args = new Bundle();
    args.putString(Constants.BUDDY_ID, chatId);
    args.putLong(Constants.MESSAGE_ID, messageId);
    fragment.setArguments(args);
    return fragment;
  }

  public SingleImageFragment(){
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (getArguments() != null){
      chatId = getArguments().getString(Constants.BUDDY_ID);
      messageId = getArguments().getLong(Constants.MESSAGE_ID);
    }
  }

  @Override
  public void onResume(){
    super.onResume();
    initUI();
  }

  private void initUI(){

    // get the imageMessage from messageHistory
    MessageHistory messageHistory = new MessageHistory(getContext());
    MessageArrayContent mac = messageHistory.getMessage(chatId, messageId);
    if (!(mac instanceof ImageMessage)){
      getActivity().getSupportFragmentManager().popBackStack();
      return;
    }
    ImageMessage msg = (ImageMessage) mac;
    // get the imageView and load the image in the background
    ImageView imageView = ((ImageView) getView().findViewById(R.id
            .image));
    imageView.setOnTouchListener(new OnSwipeTouchListener(getContext()){
      @Override
      public void onSwipeDown(){
      }

      @Override
      public void onSwipeUp(){
      }

      @Override
      public void onSwipeRight(){
        previousImage();
      }

      @Override
      public void onSwipeLeft(){
        nextImage();
      }
    });
    imageView.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        toggleOverlay();
      }
    });
    new Thread(new LoadImageRunnable(
            imageView, new Handler(), getContext(), new File(msg.file))).start();
  }

  private void nextImage(){

  }

  private void previousImage(){

  }

  private void toggleOverlay(){
    final View customActionBar = getActivity().findViewById(R.id.custom_action_bar);
    final View imageInfo = getActivity().findViewById(R.id.image_info);
    Animation anim;
    if (overlayActive){
      getActivity().getWindow().addFlags(WindowManager.LayoutParams
              .FLAG_FULLSCREEN);
      anim = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_out);
      anim.setDuration(300);
      anim.setAnimationListener(new Animation.AnimationListener(){
        @Override
        public void onAnimationStart(Animation animation){
        }

        @Override
        public void onAnimationEnd(Animation animation){
          imageInfo.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation){
        }
      });
      imageInfo.startAnimation(anim);
      anim = AnimationUtils.loadAnimation(getContext(), R.anim
              .top_out);
      anim.setDuration(300);
      anim.setAnimationListener(new Animation.AnimationListener(){
        @Override
        public void onAnimationStart(Animation animation){
        }

        @Override
        public void onAnimationEnd(Animation animation){
          customActionBar.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation){
        }
      });
      customActionBar.startAnimation(anim);
    }else{
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams
              .FLAG_FULLSCREEN);
      anim = AnimationUtils.loadAnimation(getContext(), R.anim
              .bottom_in);
      anim.setDuration(300);
      imageInfo.setVisibility(View.VISIBLE);
      imageInfo.startAnimation(anim);
      anim = AnimationUtils.loadAnimation(getContext(), R.anim.top_in);
      anim.setDuration(300);
      customActionBar.setVisibility(View.VISIBLE);
      customActionBar.startAnimation(anim);
    }
    overlayActive = !overlayActive;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // set the status bar color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
      getActivity().getWindow().setStatusBarColor(Color.BLACK);
    }
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_single_image, container, false);
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
//    void onFragmentInteraction(Uri uri);
  }
}
