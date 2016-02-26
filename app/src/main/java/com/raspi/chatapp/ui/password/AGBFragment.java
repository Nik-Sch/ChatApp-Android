package com.raspi.chatapp.ui.password;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.raspi.chatapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AGBFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AGBFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AGBFragment extends Fragment{
  private static final String agb = "<b>The MIT License (MIT)</b><br/>" +
          "Copyright (c) 2016 MusicToGo-Coop<br/>" +
          "<br/>" +
          "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:<br/>" +
          "<br/>" +
          "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.<br/>" +
          "<br/>" +
          "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";

  private OnFragmentInteractionListener mListener;

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment AGBFragment.
   */
  public static AGBFragment newInstance(){
    AGBFragment fragment = new AGBFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume(){
    super.onResume();
    TextView tv = ((TextView) getActivity().findViewById(R.id.agb));
    tv.setText(Html.fromHtml(agb));
    ((ImageButton) getActivity().findViewById(R.id.agb_accept))
            .setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v){
                mListener.agbAccepted();
              }
            });
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_agb, container, false);
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
    void agbAccepted();
  }
}
