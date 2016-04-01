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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.chat_array.ChatArrayAdapter;
import com.raspi.chatapp.ui.util.chat_array.ChatEntry;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.rockerhieu.emojicon.EmojiconTextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatListFragment extends Fragment{
  private ActionBar actionBar;

  private OnFragmentInteractionListener mListener;
  private ChatArrayAdapter caa;
  private BroadcastReceiver messageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      // receiving a message will result in reinitializing the ui
      initUI();

      abortBroadcast();
    }
  };

  public ChatListFragment(){
    // require empty constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment ChatListFragment.
   */
  public static ChatListFragment newInstance(){
    ChatListFragment fragment = new ChatListFragment();
    fragment.setArguments(new Bundle());
    return fragment;
  }

  @Override
  public void onResume(){
    super.onResume();
    // init ui and register the receiver
    initUI();
    IntentFilter filter = new IntentFilter(Constants.MESSAGE_RECEIVED);
    filter.setPriority(1);
    getContext().registerReceiver(messageReceiver, filter);
  }

  @Override
  public void onPause(){
    super.onPause();
    // unregister the receiver
    getContext().unregisterReceiver(messageReceiver);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
    // clear the old and inflate the new optionsMenu
    menu.clear();
    inflater.inflate(R.menu.menu_chat_list, menu);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    // retrieve the actionBar
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_chat_list, container, false);

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
   * this function will make sure the ui looks right and reload everything.
   * Yeah, it is inefficient calling it every time a little detail has
   * changed but atm I don't care as this won't be as loaded as the
   * ChatFragment.
   */
  private void initUI(){
    // create the chatArrayAdapter
    caa = new ChatArrayAdapter(getContext(), R.layout.chat_list_entry);
    ListView lv = (ListView) getView().findViewById(R.id.main_listview);
    lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
    lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        // on click open the corresponding chat
        ChatEntry chatEntry = caa.getItem(position);
        mListener.onChatOpened(chatEntry.buddyId, chatEntry.name);
      }
    });
    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id){
        // on long click rename the chat --> look ChatFragment
        final EditText newName = new EditText(getActivity());
        newName.setText(caa.getItem(position).name);
        String title = getResources().getString(R.string.change_name_title) +
                " " + caa.getItem(position).name;
        // the dialog with corresponding title and message and with the
        // pre filled editText
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(R.string.change_name)
                .setView(newName)
                .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    // rename the chat and reinitialize the ui
                    MessageHistory messageHistory = new MessageHistory
                            (getContext());
                    String buddyId = caa.getItem(position).buddyId;
                    String name = newName.getText().toString();
                    messageHistory.renameChat(buddyId, name);
                    initUI();
                  }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    // there is a cancel button too...
                  }
                }).show();
        return true;
      }
    });
    // set the data
    lv.setAdapter(caa);
    MessageHistory messageHistory = new MessageHistory(getContext());
    // retrieve the chats and add them to the listView
    ChatEntry[] entries = messageHistory.getChats();
    for (ChatEntry entry : entries){
      if (entry != null)
        caa.add(entry);
    }

    // the swipe refresh layout
    final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) getActivity()
            .findViewById(R.id.swipe_refresh);
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
      @Override
      public void onRefresh(){
        // make everything related to refreshing in a background thread
        new Thread(new RefreshRunnable(new Handler(), swipeRefreshLayout)).start();
      }
    });

    // make sure that the no internet text displays the emojicon correctly
    EmojiconTextView textView = (EmojiconTextView) getActivity()
            .findViewById(R.id.no_internet_text);
    textView.setText(String.format(getActivity().getResources().getString(R
            .string.no_internet), "\uD83D\uDE28"));

    // set the title
    actionBar.setTitle("ChatApp");
    // make sure that there is no subtitle
    actionBar.setSubtitle(null);
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
    void onChatOpened(String buddyId, String name);
  }

  /**
   * this runnable does the refresh work
   */
  private class RefreshRunnable implements Runnable{
    Handler mHandler;
    SwipeRefreshLayout refreshLayout;

    /**
     * create the refreshRunnable.
     *
     * @param handler       an handler created in the ui thread for executing ui
     *                      operations.
     * @param refreshLayout the layout that is being refreshed.
     */
    public RefreshRunnable(Handler handler, SwipeRefreshLayout refreshLayout){
      mHandler = handler;
      this.refreshLayout = refreshLayout;
    }

    @Override
    public void run(){
      // enable refreshing
      mHandler.post(new Runnable(){
        @Override
        public void run(){
          refreshLayout.setRefreshing(true);
        }
      });
      // check for a connection
      XmppManager xmppManager = XmppManager.getInstance();
      boolean internet = true;
      if (!isReachable("raspi-server.ddns.net", 5222, 2000)){
        // if there is no internet
        internet = false;
        Log.d("INTERNET", "I don't have internet");
        // show the ui that there is no internet
        mHandler.post(new Runnable(){
          @Override
          public void run(){
            // make the view visible
            RelativeLayout noInternet = (RelativeLayout) getActivity()
                    .findViewById(R.id.no_internet);
            noInternet.setVisibility(View.VISIBLE);
            // get the in animation and set its duration
            Animation noInternetInAnimation = AnimationUtils.loadAnimation
                    (getContext(), R.anim.top_in);
            noInternetInAnimation.setDuration(500);
            // start the animation
            noInternet.startAnimation(noInternetInAnimation);
          }
        });
      }
      // disable refreshing
      mHandler.post(new Runnable(){
        @Override
        public void run(){
          refreshLayout.setRefreshing(false);
        }
      });
      if (!internet){
        // if I showed the overlay it needs to be hidden afterwards
        try{
          // wait for 2 seconds
          Thread.sleep(2000);
        }catch (Exception e){
          e.printStackTrace();
        }
        // start the hiding
        mHandler.post(new Runnable(){
          @Override
          public void run(){
            // the layout
            final RelativeLayout noInternet = (RelativeLayout) getActivity()
                    .findViewById(R.id.no_internet);
            if (noInternet != null){
              // if it still exists, create the animation, set its duration
              // and start it
              Animation noInternetOutAnimation = AnimationUtils.loadAnimation
                      (getContext(), R.anim.top_out);
              noInternetOutAnimation.setDuration(500);
              noInternetOutAnimation.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){
                }

                @Override
                public void onAnimationEnd(Animation animation){
                  // if it finished hide the view completely
                  noInternet.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation){
                }
              });
              noInternet.startAnimation(noInternetOutAnimation);
            }
          }
        });
      }
      // just try to connect no matter if I have internet or not, won't do
      // any harm
      try{
        xmppManager.getConnection().connect();
      }catch (Exception e){
        e.printStackTrace();
      }
    }

    /**
     * checks whether the given address is reachable at the specified port.
     * @param addr the address to be checked
     * @param port the port to which to connect
     * @param timeout the maximum time the connections will be tried to be
     *                established
     * @return true if a connection could be established
     */
    private boolean isReachable(String addr, int port, int timeout){
      try{
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(addr, port), timeout);
        socket.close();
        return true;
      }catch (IOException e){
        return false;
      }
    }
  }
}
