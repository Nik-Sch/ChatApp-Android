package com.raspi.chatapp.ui.chatting;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.github.ankushsachdeva.emojicon.EmojiconEditText;
import com.github.ankushsachdeva.emojicon.EmojiconGridView;
import com.github.ankushsachdeva.emojicon.EmojiconsPopup;
import com.github.ankushsachdeva.emojicon.emoji.Emojicon;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.WallpaperImageView;
import com.raspi.chatapp.ui.util.message_array.Date;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.LoadMoreMessages;
import com.raspi.chatapp.ui.util.message_array.MessageArrayAdapter;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.ui.util.message_array.NewMessage;
import com.raspi.chatapp.ui.util.message_array.TextMessage;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.internet.http.MessageDownloadService;
import com.raspi.chatapp.util.internet.http.Upload;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnChatFragmentInteractionListener} interface
 * to handle interaction events.<br/>
 * This fragment contains the basic chat. It manages everything that has to
 * do with the ui of one single chat.
 */
public class ChatFragment extends Fragment{
  /**
   * the messageLimit describes how many messages are loaded at creating and
   * also how many more messages are loaded when clicking "loadMoreMessages".
   */
  private static final int MESSAGE_LIMIT = 30;

  /**
   * the current amount of loaded messages.
   */
  private int messageAmount = MESSAGE_LIMIT;

  /**
   * the buddyId of this instance of ChatFragment.
   */
  private String buddyId;
  /**
   * the chatName of this instance of ChatFragment.
   */
  private String chatName;

  /**
   * the array adapter for the listView containing the messages
   */
  private MessageArrayAdapter maa;
  /**
   * the listView containing the messages.
   */
  private ListView listView;
  /**
   * an instance of the messageHistory for saving/reading data to/from the db.
   */
  private MessageHistory messageHistory;
  /**
   * the uploadReceiver receives status updates from uploading. This includes
   * onCompleted and onError.
   */
  private final UploadServiceBroadcastReceiver uploadReceiver =
          new UploadServiceBroadcastReceiver(){
            @Override
            public void onProgress(String uploadId, int progress){
              //received a progress update
              Log.d("UPLOAD_DEBUG", "progress: " + progress);
              int index = uploadId.indexOf('|');
              String buddyID = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              //check if this is the correct chat
              if (buddyID.equals(buddyId)){
                int size = listView.getLastVisiblePosition();
                MessageArrayContent mac;
                //check for all visible messages
                for (int i = listView.getFirstVisiblePosition(); i <= size;
                     i++){
                  mac = maa.getItem(i);
                  //atm, only ImageMessages are able to receive a uploadEvent
                  if (mac instanceof ImageMessage){
                    ImageMessage im = (ImageMessage) mac;
                    //if the id fits the messageId update the messageView
                    if (im._ID == Long.parseLong(messageId)){
                      Log.d("UPLOAD_DEBUG", "progress: " + progress);
                      im.progress = progress;
                      updateMessage(i);
                    }
                  }
                }
              }
            }

            @Override
            public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage){
              //received a completed update
              int index = uploadId.indexOf('|');
              String buddyID = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              //check if this is the correct chat
              if (buddyID.equals(buddyId)){
                int size = maa.getCount();
                MessageArrayContent mac;
                //check for all loaded messages
                for (int i = 0; i < size; i++){
                  mac = maa.getItem(i);
                  //atm, only ImageMessages are able to receive a uploadEvent
                  if (mac instanceof ImageMessage){
                    ImageMessage im = (ImageMessage) mac;
                    //if the id fits the messageId update the messageView
                    if (im._ID == Long.parseLong(messageId)){
                      im.status = MessageHistory.STATUS_SENT;
                      updateMessage(i);
                    }
                  }
                }
              }
            }

            @Override
            public void onError(String uploadId, Exception exception){
              //received an error update
              int index = uploadId.indexOf('|');
              String buddyID = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              //check if this is the correct chat
              if (buddyID.equals(buddyId)){
                int size = maa.getCount();
                MessageArrayContent mac;
                //check for all loaded messages
                for (int i = 0; i <= size; i++){
                  mac = maa.getItem(i);
                  //atm, only ImageMessages are able to receive a uploadEvent
                  if (mac instanceof ImageMessage){
                    ImageMessage im = (ImageMessage) mac;
                    //if the id fits the messageId update the messageView
                    if (im._ID == Long.parseLong(messageId)){
                      im.status = MessageHistory.STATUS_WAITING;
                      updateMessage(i);
                    }
                  }
                }
              }
            }
          };
  /**
   * the editText where the message is typed. You are able to use emojicon.
   */
  private EmojiconEditText textIn;
  /**
   * this is the actionBar visible at the top.
   */
  private ActionBar actionBar;
  /**
   * the listener that is implemented by the ChatActivity.
   */
  private OnChatFragmentInteractionListener mListener;
  /**
   * this receiver should be called if we received a new message and we need
   * to load this
   */
  private BroadcastReceiver messageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      String intentBuddyId = extras.getString(Constants.BUDDY_ID);
      int index = intentBuddyId.indexOf('@');
      if (index >= 0)
        intentBuddyId = intentBuddyId.substring(0, index);
      // check whether this is the correct chat
      if (buddyId.equals(intentBuddyId)){
        int i = 0;
        // if there was the NewMessage item anywhere in the maa, delete it
        for (MessageArrayContent mac : maa){
          if (mac instanceof NewMessage)
            maa.remove(i);
          i++;
        }
        // I retrieve the lastMessage as I suppose there is only one message
        // added.
        // TODO: pass a messageId to this receiver and get the specific message
        MessageArrayContent mac = messageHistory.getLastMessage(buddyId, true);
        // if this is an image download it. the download task will take care
        // of message status and acknowledgements
        if (mac instanceof ImageMessage)
          downloadImage((ImageMessage) mac);
        else
          // this is a TextMessage -> directly send the read acknowledgement
          // and update the messageStatus
          try{
            // careful with my id and the others id as they may differ
            messageHistory.updateMessageStatus(buddyId, ((TextMessage) mac)._ID,
                    MessageHistory.STATUS_READ);
            long othersId = extras.getLong("id");
            XmppManager.getInstance().sendAcknowledgement(buddyId,
                    othersId, MessageHistory.STATUS_READ);
          }catch (Exception e){
            e.printStackTrace();
          }
        // finally add the message to the listView and select it in order to
        // scroll down
        maa.add(mac);
        listView.setSelection(maa.getCount() - 1);
        // in case this was an orderedBroadcast abort it.
        abortBroadcast();
      }
    }
  };

  /**
   * this receiver receives an event if the presence of anyone in my roster
   * changed.
   */
  private BroadcastReceiver presenceChangeReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      // if the intent is correct
      if (extras != null && extras.containsKey(Constants.BUDDY_ID) && extras.containsKey(Constants.PRESENCE_STATUS)){
        // if this is the correct chat
        if (buddyId.equals(extras.getString(Constants.BUDDY_ID))){
          //update the status to the new one.
          updateStatus(extras.getString(Constants.PRESENCE_STATUS));
        }
      }
    }
  };

  /**
   * this receiver receives an event if a messageStatus changed (e.g.
   * received an acknowledgement.
   */
  private BroadcastReceiver messageStatusChangedReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      // if the intent is correct
      if (extras != null && extras.containsKey(Constants.BUDDY_ID) &&
              extras.containsKey("id") && extras.containsKey("status")){
        String bId = extras.getString(Constants.BUDDY_ID);
        int index = bId.indexOf('@');
        if (index >= 0){
          bId = bId.substring(0, index);
        }
        // if this is the correct chat
        if (buddyId.equals(bId)){
          long id = extras.getLong("id");
          int i = 0;
          // loop through all messages
          for (MessageArrayContent mac : maa){
            // need to separate into imageMessage and textMessage because of
            // casting
            if (mac instanceof ImageMessage){
              ImageMessage msg = (ImageMessage) mac;
              // if the id is correct update the status and get the view in
              // order to update the view.. makes sense so far...
              if (msg._ID == id){
                msg.status = extras.getString("status");
                maa.getView(i, listView.getChildAt(i - listView
                        .getFirstVisiblePosition()), listView);
              }
            }else if (mac instanceof TextMessage){
              TextMessage msg = (TextMessage) mac;
              // do the same with TextMessage
              if (msg._ID == id){
                msg.status = extras.getString("status");
                maa.getView(i, listView.getChildAt(i - listView
                        .getFirstVisiblePosition()), listView);
              }
            }
            i++;
          }
        }
      }
    }
  };

  /**
   * this receiver receives an event if the xmppManager reconnected to the
   * server.
   */
  private BroadcastReceiver reconnectedReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      // make the status visible
      updateStatus(messageHistory.getOnline(buddyId));

      // resend all messages that have not been sent.
      int size = listView.getCount();
      for (int i = 0; i < size; i++){
        MessageArrayContent mac = maa.getItem(i);
        if (mac instanceof TextMessage){
          TextMessage msg = (TextMessage) mac;
          if (MessageHistory.STATUS_WAITING.equals(msg.status))
            resendTextMessage(msg);
        }
      }
    }
  };

  /**
   * this listener manages the selection of messages
   */
  private AbsListView.MultiChoiceModeListener
          multiChoiceModeListener = new AbsListView.MultiChoiceModeListener(){
    Menu menu;
    // this is the set over all selected messages. This will also include
    // "unselectable" items like DateMessage as I am filtering these when
    // executing an action.
    Set<MessageArrayContent> selected;
    // This Set saves all positions in the maa of the selected items.
    // Due to the way removing multiple elements of an ArrayAdapter works I
    // need to remove the elements in reversed order (from bottom to top).
    // Also, as the treeSet sorts integers from low to high, I want to
    // reverse the indices.
    Set<Integer> selectedPositions;

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked){
      // TODO update number in CAB
      MessageArrayContent mac = maa.getItem(position);
      // if the selection is invalid try to select the item below it.
      if (mac instanceof Date || mac instanceof NewMessage || mac instanceof
              LoadMoreMessages){
        try{
          listView.setItemChecked(position + 1, true);
        }catch (Exception e){
          e.printStackTrace();
        }
      }
      // if I checked it add this mac to the selected Set and if it really
      // was added, also add it the selectedPositions Set (with a negative
      // sign, see above)
      if (checked){
        if (selected.add(mac))
          selectedPositions.add(-position);
        // otherwise remove it
      }else{
        if (selected.remove(mac)){
          // yep casting an int into an Integer is necessary because the
          // remove function is overloaded: it may take the object to remove
          // OR the index (int) of the item to remove.
          Integer x = -position;
          selectedPositions.remove(x);
        }
      }

      // if there is exactly one item selected show the copy actionItem.
      MenuItem itemCopy = menu.findItem(R.id.action_copy);
      // be careful, use the count function and not the count of one of the
      // Sets because the sets may also include invalid selections.
      itemCopy.setVisible((count()) == 1);

      // if I deselected the last valid item finish the actionMode
      if (count() == 0)
        mode.finish();
    }

    /**
     * calculates the amount of valid selections. That means every selected
     * TextMessage or ImageMessage
     * @return the count of valid selections made
     */
    private int count(){
      int result = 0;
      // loop through the selected Set and increment the result if it is a
      // Text- or ImageMessage
      for (MessageArrayContent mac : selected)
        if (mac instanceof TextMessage || mac instanceof ImageMessage)
          result++;
      return result;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu){
      // inflate the menu
      MenuInflater inflater = mode.getMenuInflater();
      inflater.inflate(R.menu.menu_message_select, menu);
      this.menu = menu;
      // if I am over LOLLIPOP also set the color of the statusBar to the
      // primary color as it would, otherwise, be black or something like that.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        // yeah getColor is deprecated but I am actually targeting API 16 not
        // 23...
        getActivity().getWindow().setStatusBarColor(getResources().getColor
                (R.color.colorPrimaryDark));
      }
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu){
      // initialize the Sets
      selected = new HashSet<>();
      // this is a tree set because a treeSet sorts the element (binary
      // search tree implementation I suppose)
      selectedPositions = new TreeSet<>();
      return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item){
      //the result is only true if I really caught the event ( I should always)
      boolean result = false;
      switch (item.getItemId()){
        // if the user want to copy the content
        case R.id.action_copy:
          // the selected Set should only contain one item, so just get it
          // via array conversion
          MessageArrayContent mac = selected.toArray(new
                  MessageArrayContent[1])[0];
          String text = null;
          // if it is a textMessage, obviously, select the text.
          if (mac instanceof TextMessage)
            text = ((TextMessage) mac).message;
            // if it is an imageMessage, however, select the description.
          else if (mac instanceof ImageMessage)
            text = ((ImageMessage) mac).description;

          if (text != null){
            // if I selected something (should always happen) copy it as
            // simple text to the clipboard
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("simple text", text);
            clipboard.setPrimaryClip(clipData);
          }
          // finish the action mode
          mode.finish();
          result = true;
          break;
        case R.id.action_delete:
          // the delete action is not that straight forward. I will ask the
          // user if he is sure to delete the items.
          new AlertDialog.Builder(getActivity())
                  // delete message vs. delete messages...
                  .setMessage(listView.getCheckedItemCount() > 1 ? R.string
                          .delete_messages : R.string.delete_message)
                  .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                      // if he agreed loop through all selected messages and
                      // delete them
                      for (MessageArrayContent mac : selected){
                        // remember to only use the text and imageMessages
                        if (mac instanceof TextMessage || mac instanceof ImageMessage){
                          // typecasting ftw
                          long _ID = (mac instanceof TextMessage)
                                  ? ((TextMessage) mac)._ID
                                  : ((ImageMessage) mac)._ID;
                          // remove them from the db
                          messageHistory.removeMessages(buddyId, _ID);
                        }
                      }
                      // remove them from the ui
                      for (int i : selectedPositions){
                        // remember the minus I added for the treeSet to
                        // order in the correct order.
                        maa.remove(-i);
                      }
                      // also finish the action mode and dismiss the dialog
                      mode.finish();
                      dialog.dismiss();
                    }
                  })
                  .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                      // only dismiss the dialog but don't finish the actionMode
                      dialog.dismiss();
                    }
                  }).create().show();
          result = true;
          break;
      }
      return result;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode){
      // if the actionMode gets destroyed (back click or similar) set the
      // Sets to null to be sure that there are no selections from the last time
      selected = null;
      selectedPositions = null;
    }
  };

  public ChatFragment(){
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param buddyId  TRhe buddyId of the chat partner
   * @param chatName The name of the chat
   * @return A new instance of fragment ChatFragment.
   */
  public static ChatFragment newInstance(String buddyId, String chatName){
    ChatFragment fragment = new ChatFragment();
    Bundle args = new Bundle();
    args.putString(Constants.BUDDY_ID, buddyId);
    args.putString(Constants.CHAT_NAME, chatName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    // get the actionBar
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    // just making sure
    messageAmount = MESSAGE_LIMIT;
    // if the arguments are correct set the buddyId and chatName, otherwise,
    // throw an error
    try{
      buddyId = getArguments().getString(Constants.BUDDY_ID);
      chatName = getArguments().getString(Constants.CHAT_NAME);
    }catch (Exception e){
      throw new IllegalArgumentException("There must be a buddyId and " +
              "chatName provided in order to create this fragment.");
    }
    // create the instance of messageHistory
    messageHistory = new MessageHistory(getContext());
  }

  @Override
  public void onResume(){
    super.onResume();
    // start the different receiver and init the ui
    IntentFilter filter = new IntentFilter(Constants.MESSAGE_RECEIVED);
    filter.setPriority(1);
    // messageReceiver. this is for reasons not on the localBroadcastManager...
    getContext().registerReceiver(messageReceiver, filter);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance
            (getContext());
    // the reconnected receiver
    LBmgr.registerReceiver(reconnectedReceiver, new IntentFilter
            (Constants.RECONNECTED));
    // the presence changed receiver
    LBmgr.registerReceiver(presenceChangeReceiver, new IntentFilter
            (Constants.PRESENCE_CHANGED));
    // the messageStatus changed receiver
    LBmgr.registerReceiver(messageStatusChangedReceiver, new IntentFilter
            (Constants.MESSAGE_STATUS_CHANGED));
    uploadReceiver.register(getContext());
    // also init the ui
    initUI();
  }

  @Override
  public void onPause(){
    // unregister the different reciever (see onResume)
    InputMethodManager mgr = (InputMethodManager) getContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE);
    mgr.hideSoftInputFromWindow(getView().findViewById(R.id.chat_in)
            .getWindowToken(), 0);
    getContext().unregisterReceiver(messageReceiver);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance(getContext());
    LBmgr.unregisterReceiver(presenceChangeReceiver);
    LBmgr.unregisterReceiver(reconnectedReceiver);
    LBmgr.registerReceiver(messageStatusChangedReceiver, new IntentFilter
            (Constants.MESSAGE_STATUS_CHANGED));
    uploadReceiver.unregister(getContext());
    super.onPause();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment and make sure that there is an
    // optionsMenu showing
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_chat, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
    // clear the menu to make sure the old entries are no longer contained
    menu.clear();
    // inflate the menu for this fragment
    menuInflater.inflate(R.menu.menu_chat, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      // if clicked attach perform the onAttackClicked. Easy.
      case R.id.action_attach:
        mListener.onAttachClicked();
        return true;
      // if clicked rename open the dialog where the user can rename this chat
      case R.id.action_rename:
        // this will be the shown editText (without emojicons, I might wanna
        // change this in the future, let's see)
        final EditText newName = new EditText(getActivity());
        // prefix the EditText with the current name
        newName.setText(chatName);
        // the title comes from the resources and will include the current
        // chatName
        String title = getResources().getString(R.string.change_name_title) +
                " " + chatName;
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(R.string.change_name)
                .setView(newName)
                .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    // when clicking rename retrieve the messageHistory
                    MessageHistory messageHistory = new MessageHistory
                            (getContext());
                    String name = newName.getText().toString();
                    // update the db
                    messageHistory.renameChat(buddyId, name);
                    // set the current chatName
                    chatName = name;
                    actionBar.setTitle(chatName);
                  }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    // yeah there also is a negative button...
                  }
                }).show();
        return true;
    }
    // otherwise return false, I have not caught the event
    return false;
  }

  @Override
  public void onAttach(Context context){
    // not to be confused with onAttackClicked. This a method overridden from
    // the Fragment to signal that this fragment is somehow active
    super.onAttach(context);
    if (context instanceof OnChatFragmentInteractionListener){
      mListener = (OnChatFragmentInteractionListener) context;
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

  private void initUI(){
    // load wallpaper
    loadWallPaper();
    // enable the emojicon-keyboard
    createEmoji();
    // set the actionBar title
    if (actionBar != null)
      actionBar.setTitle(chatName);
    // create the messageArrayAdapter
    maa = new MessageArrayAdapter(getContext(), R.layout.message_text);

    listView = (ListView) getView().findViewById(R.id.chat_listview);
    textIn = (EmojiconEditText) getView().findViewById(R.id.chat_in);
    Button sendBtn = (Button) getView().findViewById(R.id.chat_sendBtn);

    sendBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        // clicking the sendButton will send the message, uhh.
        // trim the message. Spaces are a waste of resources!
        String message = textIn.getText().toString().trim();
        // and ofc only send if the message has something in it.
        if (!message.isEmpty()){
          // add the message to the messageHistory
          long id = messageHistory.addMessage(buddyId,
                  getContext().getSharedPreferences(Constants.PREFERENCES, 0)
                          .getString(Constants.USERNAME, ""),
                  // this will be a textMessage for sure
                  MessageHistory.TYPE_TEXT,
                  message,
                  MessageHistory.STATUS_WAITING,
                  -1);
          // also add the message to the ui
          maa.add(new TextMessage(false, message, new GregorianCalendar()
                  .getTimeInMillis(), MessageHistory.STATUS_WAITING, id, -1));
          // and probably the buddy also wants the message, so send it
          sendTextMessage(message, id);
          // revert the editText
          textIn.setText("");
          int i = 0;
          // remove the NewMessage mac if it exists.
          for (MessageArrayContent mac : maa){
            if (mac instanceof NewMessage)
              maa.remove(i);
            i++;
          }
          // select the last item to scroll down.
          listView.setSelection(maa.getCount() - 1);
        }
      }
    });

    // this fucking bitch cost me a lot of time. Yep normal seems quite good,
    // doesn't it. Then why the actual fuck is "NORMAL" not the default?
    // Android?
    listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
    // set the corresponding adapter
    listView.setAdapter(maa);
    // also clicking on items will do sometimes something.
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        MessageArrayContent mac = maa.getItem(position);
        // if it is an imageMessage show the image
        if (mac instanceof ImageMessage){
          ImageMessage im = (ImageMessage) mac;
          // yeah pretty straight forward, just view the image with the
          // default application. I probably should make my own imageViewer
          // which is able to scroll through the images of one chat etc.
          Intent viewIntent = new Intent(Intent.ACTION_VIEW);
          viewIntent.setDataAndType(Uri.fromFile(new File(im.file)),
                  "image/*");
          startActivity(viewIntent);

          // don't ask for a pwd as I am just viewing an image
          getContext().getSharedPreferences(Constants.PREFERENCES, 0).edit()
                  .putBoolean(Constants.PWD_REQUEST, false).apply();
        }else if (mac instanceof LoadMoreMessages){
          // clicking on loadMoreMessage should load more messages shouldn't it?
          // FIXME: fix bugs xD
          // 1. need further investigation, sometimes if there is only one more
          // message to be loaded it is not loaded but only the dateMessage
          // 2. sometimes the recycling doesn't quite work as I need to
          // scroll down and up again to view the correct messages.

          // retrieve the messages from db
          MessageArrayContent[] macs = messageHistory.getMessages(buddyId,
                  MESSAGE_LIMIT, messageAmount, true);
          messageAmount += macs.length;
          //save position in order not to scroll
          int index = listView.getFirstVisiblePosition() + 2;
          View v = listView.getChildAt(2);
          int top = (v == null) ? 0 : v.getTop();
          //remove the date
          maa.remove(1);
          // calculating whether we need a new DatMessage works with
          // converting the time in millis in days and the compare the days.
          // Therefore, I need this constant.
          final long c = 24 * 60 * 60 * 1000;
          // get the oldDate which is the date of the first message that is
          // currently loaded, so the oldest currently loaded message
          MessageArrayContent macT = maa.getItem(1);
          long oldDate = (macT instanceof TextMessage) ? ((TextMessage) macT)
                  .time : ((ImageMessage) macT).time;
          // this is needed for restoring the position after adding messages
          int count = macs.length;
          // loop through all new messages and add them, increase the count
          // and eventually insert a DateMessage
          for (MessageArrayContent macTemp : macs){
            if (macTemp instanceof TextMessage){
              TextMessage msg = (TextMessage) macTemp;
              if (msg.time / c < oldDate / c){
                maa.insert(new Date(msg.time), 1);
                count++;
              }
              // reset the old date
              oldDate = msg.time;
            }else if (macTemp instanceof ImageMessage){
              ImageMessage msg = (ImageMessage) macTemp;
              if (msg.time / c < oldDate / c){
                maa.insert(new Date(msg.time), 1);
                count++;
              }
              oldDate = msg.time;
            }
            maa.insert(macTemp, 1);
          }
          // insert the date at the end
          maa.insert(new Date(oldDate), 1);
          // reset the selection (aka scroll height
          listView.setSelectionFromTop(index + count, top);
          // if there are no more messages to be loaded than I currently have
          // loaded remove the loadMoreMessages item
          if (messageAmount >= messageHistory.getMessageAmount(buddyId)){
            maa.remove(0);
            count--;
          }
          // hmh reselect the new scrollHeight?! why not
          listView.setSelectionFromTop(index + count, top);
        }
      }
    });

    // activate the multiSelectionOption
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(multiChoiceModeListener);

    // yeah loading messages, seems to be a good idea
    loadAllMessages();
    // set the status in the actionBar
    String lastOnline = messageHistory.getOnline(buddyId);
    updateStatus(lastOnline);
    //scroll down. Due to "I don't know why" clicking a notification is
    // different from clicking a chat in the ChatListFragment and therefore I
    // need to select the last message.
    listView.setSelection(maa.getCount() - 1);
  }

  private void createEmoji(){
    // this method is based on the example for the emojicons I use
    // (https://github.com/ankushsachdeva/emojicon)
    final EmojiconEditText emojiconEditText = (EmojiconEditText) getActivity
            ().findViewById(R.id.chat_in);
    final View root = getActivity().findViewById(R.id.root_view);
    final EmojiconsPopup popup = new EmojiconsPopup(root, getActivity());
    final ImageButton emojiBtn = (ImageButton) getActivity().findViewById(R
            .id.emoti_switch);
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

  private void loadWallPaper(){
    final File file = new File(getActivity().getFilesDir(), Constants
            .WALLPAPER_NAME);
    if (file.exists()){
      final WallpaperImageView imageView = (WallpaperImageView) getView()
              .findViewById(R.id.chat_wallpaper);
      ViewTreeObserver vto = imageView.getViewTreeObserver();
      vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
        @Override
        public boolean onPreDraw(){
          imageView.getViewTreeObserver().removeOnPreDrawListener(this);
          int width = imageView.getMeasuredWidth();
          int height = imageView.getMeasuredHeight();
          new WallpaperWorkerTask(imageView, width, height).execute(file);
          return true;
        }
      });
    }
  }

  private void sendTextMessage(String message, long id){
    try{
      XmppManager xmppManager = XmppManager.getInstance(getContext());
      if (xmppManager.sendTextMessage(message, buddyId, id)){
        messageHistory.updateMessageStatus(buddyId, id, MessageHistory.STATUS_SENT);
        int i = 0;
        for (MessageArrayContent mac : maa){
          if (mac instanceof TextMessage){
            TextMessage msg = (TextMessage) mac;
            if (msg._ID == id){
              msg.status = MessageHistory.STATUS_SENT;
              maa.getView(i, listView.getChildAt(i - listView
                      .getFirstVisiblePosition()), listView);
            }
          }
          i++;
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  private void resendTextMessage(TextMessage msg){
    sendTextMessage(msg.message, msg._ID);
  }

  private void loadAllMessages(){
    maa.clear();
    MessageArrayContent[] messages;
    try{
      messages = messageHistory.getMessages(buddyId, messageAmount);
    }catch (Exception e){
      messages = new MessageArrayContent[0];
    }
    long oldDate = 0;
    final long c = 24 * 60 * 60 * 1000;
    NewMessage nm = null;

    if (messageAmount < messageHistory.getMessageAmount(buddyId))
      maa.add(new LoadMoreMessages());

    for (MessageArrayContent message : messages){
      if (message instanceof TextMessage){
        TextMessage msg = (TextMessage) message;
        if (msg.time / c > oldDate / c)
          maa.add(new Date(msg.time));
        oldDate = msg.time;
        if (msg.left){
          if (MessageHistory.STATUS_RECEIVED.equals(msg.status)){
            if (nm == null){
              nm = new NewMessage(getResources().getString(R.string
                      .new_message));
              maa.add(nm);
            }else
              nm.status = getResources().getString(R.string.new_messages);
            //send the read acknowledgement
            try{
              messageHistory.updateMessageStatus(buddyId, msg._ID,
                      MessageHistory.STATUS_READ);
              XmppManager.getInstance().sendAcknowledgement(buddyId,
                      msg.othersId, MessageHistory.STATUS_READ);
            }catch (Exception e){
              e.printStackTrace();
            }
          }
          messageHistory.updateMessageStatus(buddyId, 2, MessageHistory
                  .STATUS_READ);
        }else if (MessageHistory.STATUS_WAITING.equals(msg.status))
          resendTextMessage(msg);
        maa.add(msg);
      }else if (message instanceof ImageMessage){
        ImageMessage msg = (ImageMessage) message;
        if (msg.time / c > oldDate / c)
          maa.add(new Date(msg.time));
        oldDate = msg.time;
        if (msg.left){
          if (msg.status.equals(MessageHistory.STATUS_RECEIVED)){
            if (nm == null){
              nm = new NewMessage(getResources().getString(R.string
                      .new_message));
              maa.add(nm);
            }else
              nm.status = getResources().getString(R.string.new_messages);
          }else if (MessageHistory.STATUS_WAITING.equals(msg.status)){
            downloadImage(msg);
          }
        }else if (MessageHistory.STATUS_WAITING.equals(msg.status)){
          Upload.Task task = new Upload.Task(new File(msg.file), msg.chatId, msg
                  ._ID);
          new Upload().uploadFile(getContext(), task);
          msg.status = MessageHistory.STATUS_SENDING;
        }
        maa.add(msg);
      }
    }
    listView.setSelection(maa.getCount() - 1);
  }

  private void updateStatus(String lastOnline){
    try{
      if (lastOnline != null){
        long time = Long.valueOf(lastOnline);
        if (time > 0){
          Calendar startOfDay = Calendar.getInstance();
          startOfDay.set(Calendar.HOUR_OF_DAY, 0);
          startOfDay.set(Calendar.MINUTE, 0);
          startOfDay.set(Calendar.SECOND, 0);
          startOfDay.set(Calendar.MILLISECOND, 0);
          long diff = startOfDay.getTimeInMillis() - time;
          if (diff <= 0)
            lastOnline = getResources().getString(R.string.last_online_today) + " ";
          else if (diff > 1000 * 60 * 60 * 24)
            lastOnline = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format
                    (time) + " " + getResources().getString(R.string
                    .last_online_at) + " ";
          else
            lastOnline = getResources().getString(R.string.last_online_yesterday)
                    + " ";
          lastOnline += new SimpleDateFormat("HH:mm", Locale.GERMANY)
                  .format(time);
          if (actionBar != null)
            actionBar.setSubtitle(lastOnline);
        }else{
          if (actionBar != null)
            actionBar.setSubtitle(Html
                    .fromHtml("<font " +
                            "color='#55AAFF'>" + getResources().getString(R
                            .string.online) + "</font>"));
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  private void updateMessage(int index){
    updateMessage(index, false);
  }

  private void updateMessage(int index, boolean reloadImage){
    try{
      View v = listView.getChildAt(index - listView.getFirstVisiblePosition());
      if (v != null){
        maa.getView(index, v, listView, reloadImage);
      }
    }catch (Exception e){
    }
  }

  private void downloadImage(ImageMessage msg){
    if (msg.left)
      try{
        MyFileUtils mfu = new MyFileUtils();
        if (!mfu.isExternalStorageWritable())
          throw new Exception("ext storage not writable. Cannot save " +
                  "image");
        messageHistory.updateMessageStatus(buddyId, msg._ID,
                MessageHistory.STATUS_RECEIVING);
        msg.status = MessageHistory.STATUS_RECEIVING;
        Intent intent = new Intent(getContext(), MessageDownloadService.class);
        intent.setAction(MessageDownloadService.DOWNLOAD_ACTION);
        intent.putExtra(MessageDownloadService.PARAM_URL, msg.url);
        intent.putExtra(MessageDownloadService.PARAM_RECEIVER, new
                DownloadReceiver(new Handler()));
        intent.putExtra(MessageDownloadService.PARAM_FILE, msg.file);
        intent.putExtra(MessageDownloadService.PARAM_MESSAGE_ID, msg._ID);
        intent.putExtra(MessageDownloadService.PARAM_OTHERS_MSG_ID, msg.othersId);
        intent.putExtra(MessageDownloadService.PARAM_CHAT_ID, buddyId);
        getContext().startService(intent);
      }catch (Exception e){
        e.printStackTrace();
      }
  }

  //TODO: this is kinda unnecessary isn't it? I should either to everything
  // with this listener or nothing...

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
  public interface OnChatFragmentInteractionListener{
    void onAttachClicked();
  }

  class WallpaperWorkerTask extends AsyncTask<File, Void, Bitmap>{
    private final WeakReference<ImageView> imageViewWeakReference;
    private File data;
    private int width, height;

    public WallpaperWorkerTask(ImageView imageView, int width, int height){
      imageViewWeakReference = new WeakReference<>(imageView);
      this.width = width;
      this.height = height;
    }

    @Override
    protected Bitmap doInBackground(File... params){
      if (!params[0].isFile())
        return null;
      data = params[0];

      // First decode with inJustDecodeBounds=true to check dimensions
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(data.getAbsolutePath(), options);

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, width, height);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      return BitmapFactory.decodeFile(data.getAbsolutePath(), options);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap){
      if (bitmap != null){
        final ImageView imageView = imageViewWeakReference.get();
        if (imageView != null)
          imageView.setImageBitmap(bitmap);
      }
    }
  }

  //see MAA
  private int calculateInSampleSize(
          BitmapFactory.Options options, int reqWidth, int reqHeight){
    if (reqHeight <= 0 && reqWidth <= 0)
      throw new IllegalArgumentException("reqWidth and reqHeigth must be " +
              "positive.");
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth){

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;
      while ((halfHeight / inSampleSize) > reqHeight
              && (halfWidth / inSampleSize) > reqWidth){
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }

  public class DownloadReceiver extends ResultReceiver{

    public DownloadReceiver(Handler handler){
      super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData){
      super.onReceiveResult(resultCode, resultData);
      if (resultCode == MessageDownloadService.UPDATE_PROGRESS){
        Long messageId = resultData.getLong(MessageDownloadService.PARAM_MESSAGE_ID);
        int progress = resultData.getInt(MessageDownloadService.PARAM_PROGRESS);
        int size = listView.getLastVisiblePosition();
        MessageArrayContent mac;
        for (int i = listView.getFirstVisiblePosition(); i <= size; i++){
          mac = maa.getItem(i);
          if (mac instanceof ImageMessage){
            ImageMessage im = (ImageMessage) mac;
            if (im._ID == messageId){
              Log.d("DEBUG DOWNLOAD", "progress: " + progress);
              if (progress < 100){
                ((ImageMessage) mac).progress = progress;
//                updateProgressBar(i, progress);
                updateMessage(i);
              }else{
                im.status = MessageHistory.STATUS_READ;
                updateMessage(i);
                messageHistory.updateMessageStatus(buddyId, messageId,
                        MessageHistory.STATUS_READ);
                updateMessage(i, true);
              }
            }
          }
        }
      }
    }
  }
}
