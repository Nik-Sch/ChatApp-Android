package com.raspi.chatapp.ui.image;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.raspi.chatapp.R;
import com.raspi.chatapp.util.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OverviewImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OverviewImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewImageFragment extends Fragment{

  private String chatId;

  private OnFragmentInteractionListener mListener;

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param chatId the chatId of the overView
   * @return A new instance of fragment OverviewImageFragment.
   */
  public static OverviewImageFragment newInstance(String chatId){
    OverviewImageFragment fragment = new OverviewImageFragment();
    Bundle args = new Bundle();
    args.putString(Constants.BUDDY_ID, chatId);
    fragment.setArguments(args);
    return fragment;
  }

  public OverviewImageFragment(){
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (getArguments() != null){
      chatId = getArguments().getString(Constants.BUDDY_ID);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_overview_image, container, false);
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
