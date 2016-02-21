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
import android.widget.ImageView;
import android.widget.ListView;

import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.message_array.Date;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.LoadMoreMessages;
import com.raspi.chatapp.ui.util.message_array.MessageArrayAdapter;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.ui.util.message_array.NewMessage;
import com.raspi.chatapp.ui.util.message_array.TextMessage;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.internet.http.DownloadService;
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
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment{
  private static final int MESSAGE_LIMIT = 30;

  private int messageAmount = MESSAGE_LIMIT;

  private String buddyId;
  private String chatName;

  private MessageArrayAdapter maa;
  private MessageHistory messageHistory;
  private ListView listView;
  private final UploadServiceBroadcastReceiver uploadReceiver =
          new UploadServiceBroadcastReceiver(){
            @Override
            public void onProgress(String uploadId, int progress){
              Log.d("UPLOAD_DEBUG", "progress: " + progress);
              int index = uploadId.indexOf('|');
              String buddyID = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              if (buddyID.equals(buddyId)){
                int size = listView.getLastVisiblePosition();
                MessageArrayContent mac;
                for (int i = listView.getFirstVisiblePosition(); i <= size;
                     i++){
                  mac = maa.getItem(i);
                  if (mac instanceof ImageMessage){
                    ImageMessage im = (ImageMessage) mac;
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
              int index = uploadId.indexOf('|');
              String buddyID = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              if (buddyID.equals(buddyId)){
                int size = listView.getLastVisiblePosition();
                MessageArrayContent mac;
                for (int i = listView.getFirstVisiblePosition(); i <= size;
                     i++){
                  mac = maa.getItem(i);
                  if (mac instanceof ImageMessage){
                    ImageMessage im = (ImageMessage) mac;
                    if (im._ID == Long.parseLong(messageId)){
                      im.status = MessageHistory.STATUS_SENT;
                      updateMessage(i);
                    }
                  }
                }
              }
            }
          };
  private EditText textIn;
  private ActionBar actionBar;
  private OnFragmentInteractionListener mListener;
  private BroadcastReceiver messageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      String intentBuddyId = extras.getString(ChatActivity.BUDDY_ID);
      int index = intentBuddyId.indexOf('@');
      if (index >= 0)
        intentBuddyId = intentBuddyId.substring(0, index);
      if (buddyId.equals(intentBuddyId)){
        int i = 0;
        for (MessageArrayContent mac : maa){
          if (mac instanceof NewMessage)
            maa.remove(i);
          i++;
        }
        MessageArrayContent mac = messageHistory.getLastMessage(buddyId, true);
        if (mac instanceof ImageMessage)
          downloadImage((ImageMessage) mac);
        maa.add(mac);
        listView.setSelection(maa.getCount() - 1);
        //also send the read acknowledgement
        try{
          XmppManager.getInstance(context).sendAcknowledgement(buddyId,
                  extras.getLong("id"), MessageHistory.STATUS_READ);
        }catch (Exception e){
        }
        abortBroadcast();
      }
    }
  };
  private BroadcastReceiver presenceChangeReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      if (extras != null && extras.containsKey(ChatActivity.BUDDY_ID) && extras.containsKey(ChatActivity.PRESENCE_STATUS)){
        if (buddyId.equals(extras.getString(ChatActivity.BUDDY_ID))){
          updateStatus(extras.getString(ChatActivity.PRESENCE_STATUS));
        }
      }
    }
  };
  private BroadcastReceiver messageStatusChangedReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      if (extras != null && extras.containsKey(ChatActivity.BUDDY_ID) &&
              extras.containsKey("id") && extras.containsKey("status")){
        String bId = extras.getString(ChatActivity.BUDDY_ID);
        int index = bId.indexOf('@');
        if (index >= 0){
          bId = bId.substring(0, index);
        }
        if (buddyId.equals(bId)){
          long id = extras.getLong("id");
          int i = 0;
          for (MessageArrayContent mac : maa){
            if (mac instanceof ImageMessage){
              ImageMessage msg = (ImageMessage) mac;
              if (msg._ID == id){
                msg.status = extras.getString("status");
                maa.getView(i, listView.getChildAt(i - listView
                        .getFirstVisiblePosition()), listView);
              }
            }else if (mac instanceof TextMessage){
              TextMessage msg = (TextMessage) mac;
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
  private BroadcastReceiver reconnectedReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      updateStatus(messageHistory.getOnline(buddyId));

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
  private AbsListView.MultiChoiceModeListener
          multiChoiceModeListener = new AbsListView.MultiChoiceModeListener(){
    Menu menu;
    Set<MessageArrayContent> selected;
    //as I am only using unsigned integer I add a sign in front of it, so the
    // treeset sorts it reverse. Because I am going to remove the items from
    // the arrayadapter I need the positions to be sorted reversed, because
    // fi removing one item all indices larger than the removed ones are
    // decremented
    Set<Integer> selectedPositions;

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked){
      //TODO update number in CAB
      MessageArrayContent mac = maa.getItem(position);
      if (mac instanceof Date || mac instanceof NewMessage || mac instanceof
              LoadMoreMessages){
        try{
          listView.setItemChecked(position + 1, true);
        }catch (Exception e){
          e.printStackTrace();
        }
      }
      if (checked){
        if (selected.add(mac))
          selectedPositions.add(-position);
      }else{
        if (selected.remove(mac)){
          Integer x = -position;
          selectedPositions.remove(x);
        }
      }

      MenuItem itemCopy = menu.findItem(R.id.action_copy);
      itemCopy.setVisible((count()) == 1);

      if (count() == 0)
        mode.finish();
    }

    private int count(){
      int result = 0;
      for (MessageArrayContent mac : selected)
        if (mac instanceof TextMessage || mac instanceof ImageMessage)
          result++;
      return result;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu){
      MenuInflater inflater = mode.getMenuInflater();
      inflater.inflate(R.menu.menu_message_select, menu);
      this.menu = menu;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        getActivity().getWindow().setStatusBarColor(getResources().getColor
                (R.color.colorPrimaryDark));
      }
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu){
      selected = new HashSet<>();
      selectedPositions = new TreeSet<>();
      return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item){
      boolean result = false;
      switch (item.getItemId()){
        case R.id.action_copy:
          MessageArrayContent mac = selected.toArray(new
                  MessageArrayContent[1])[0];
          String text = null;
          if (mac instanceof TextMessage)
            text = ((TextMessage) mac).message;
          else if (mac instanceof ImageMessage)
            text = ((ImageMessage) mac).description;

          if (text != null){
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("simple text", text);
            clipboard.setPrimaryClip(clipData);
          }
          mode.finish();
          result = true;
          break;
        case R.id.action_delete:
          new AlertDialog.Builder(getActivity())
                  .setMessage(listView.getCheckedItemCount() > 1 ? R.string
                          .delete_messages : R.string.delete_message)
                  .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                      for (MessageArrayContent mac : selected){
                        if (mac instanceof TextMessage || mac instanceof ImageMessage){
                          long _ID = (mac instanceof TextMessage)
                                  ? ((TextMessage) mac)._ID
                                  : ((ImageMessage) mac)._ID;
                          messageHistory.removeMessages(buddyId, _ID);
                        }
                      }
                      for (int i : selectedPositions){
                        maa.remove(-i);
                      }
                      mode.finish();
                      dialog.dismiss();
                    }
                  })
                  .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which){
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
    args.putString(ChatActivity.BUDDY_ID, buddyId);
    args.putString(ChatActivity.CHAT_NAME, chatName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    messageAmount = MESSAGE_LIMIT;
    if (getArguments() != null){
      buddyId = getArguments().getString(ChatActivity.BUDDY_ID);
      chatName = getArguments().getString(ChatActivity.CHAT_NAME);
    }else
      return;
    messageHistory = new MessageHistory(getContext());
  }

  @Override
  public void onResume(){
    super.onResume();
    IntentFilter filter = new IntentFilter(ChatActivity.RECEIVE_MESSAGE);
    filter.setPriority(1);
    getContext().registerReceiver(messageReceiver, filter);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance
            (getContext());
    LBmgr.registerReceiver(reconnectedReceiver, new IntentFilter
            (ChatActivity.RECONNECTED));
    LBmgr.registerReceiver(presenceChangeReceiver, new IntentFilter
            (ChatActivity.PRESENCE_CHANGED));
    LBmgr.registerReceiver(messageStatusChangedReceiver, new IntentFilter
            (ChatActivity.MESSAGE_STATUS_CHANGED));
    uploadReceiver.register(getContext());
    initUI();
  }

  @Override
  public void onPause(){
    InputMethodManager mgr = (InputMethodManager) getContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE);
    mgr.hideSoftInputFromWindow(getView().findViewById(R.id.chat_in)
            .getWindowToken(), 0);
    getContext().unregisterReceiver(messageReceiver);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance(getContext());
    LBmgr.unregisterReceiver(presenceChangeReceiver);
    LBmgr.unregisterReceiver(reconnectedReceiver);
    LBmgr.registerReceiver(messageStatusChangedReceiver, new IntentFilter
            (ChatActivity.MESSAGE_STATUS_CHANGED));
    uploadReceiver.unregister(getContext());
    super.onPause();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_chat, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
    menu.clear();
    menuInflater.inflate(R.menu.menu_chat, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      case R.id.action_attach:
        mListener.onAttachClicked();
        return true;
      case R.id.action_rename:
        final EditText newName = new EditText(getActivity());
        newName.setText(chatName);
        String title = getResources().getString(R.string.change_name_title) +
                " " + chatName;
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(R.string.change_name)
                .setView(newName)
                .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    MessageHistory messageHistory = new MessageHistory
                            (getContext());
                    String name = newName.getText().toString();
                    messageHistory.renameChat(buddyId, name);
                    chatName = name;
                    actionBar.setTitle(chatName);
                  }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){

                  }
                }).show();
        return true;
      case R.id.action_settings:
        break;
      case R.id.home:
        break;
      default:
        break;
    }
    return false;
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
    //load wallpaper
    loadWallPaper();
    if (actionBar != null)
      actionBar.setTitle(chatName);
    maa = new MessageArrayAdapter(getContext(), R.layout.message_text);

    listView = (ListView) getView().findViewById(R.id.chat_listview);
    textIn = (EditText) getView().findViewById(R.id.chat_in);
    Button sendBtn = (Button) getView().findViewById(R.id.chat_sendBtn);

    sendBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        String message = textIn.getText().toString().trim();
        if (!message.isEmpty()){
          long id = messageHistory.addMessage(buddyId, getContext()
                  .getSharedPreferences(ChatActivity.PREFERENCES, 0).getString
                          (ChatActivity.USERNAME, ""), MessageHistory
                  .TYPE_TEXT, message, MessageHistory.STATUS_WAITING, -1);
          maa.add(new TextMessage(false, message, new GregorianCalendar()
                  .getTimeInMillis(), MessageHistory.STATUS_WAITING, id, -1));
          sendTextMessage(message, id);
          textIn.setText("");
          int i = 0;
          for (MessageArrayContent mac : maa){
            if (mac instanceof NewMessage)
              maa.remove(i);
            i++;
          }
          listView.setSelection(maa.getCount() - 1);
        }
      }
    });

    listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
    listView.setAdapter(maa);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        MessageArrayContent mac = maa.getItem(position);
        if (mac instanceof ImageMessage){
          ImageMessage im = (ImageMessage) mac;
          Intent pickIntent = new Intent(Intent.ACTION_VIEW);
          pickIntent.setDataAndType(Uri.fromFile(new File(im.file)),
                  "image/*");
          startActivity(pickIntent);
        }else if (mac instanceof LoadMoreMessages){
          MessageArrayContent[] macs = messageHistory.getMessages(buddyId,
                  MESSAGE_LIMIT, messageAmount, true);
          messageAmount += MESSAGE_LIMIT;
          //save position to not scroll
          int index = listView.getFirstVisiblePosition() + 2;
          View v = listView.getChildAt(2);
          int top = (v == null) ? 0 : v.getTop();
          //remove the date
          maa.remove(1);
          final long c = 24 * 60 * 60 * 1000;
          MessageArrayContent macT = maa.getItem(1);
          long oldDate = (macT instanceof TextMessage) ? ((TextMessage) macT)
                  .time : ((ImageMessage) macT).time;
          int count = macs.length;
          for (MessageArrayContent macTemp : macs){
            if (macTemp instanceof TextMessage){
              TextMessage msg = (TextMessage) macTemp;
              if (msg.time / c < oldDate / c){
                maa.insert(new Date(msg.time), 1);
                count++;
              }
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
          maa.insert(new Date(oldDate), 1);
          listView.setSelectionFromTop(index + count, top);
          if (messageAmount >= messageHistory.getMessageAmount(buddyId)){
            maa.remove(0);
            count--;
          }
          listView.setSelectionFromTop(index + count, top);
        }
      }
    });

    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(multiChoiceModeListener);
    reloadMessages();
    String lastOnline = messageHistory.getOnline(buddyId);
    updateStatus(lastOnline);
    //scroll down for notification click
    listView.setSelection(maa.getCount() - 1);
  }

  private void loadWallPaper(){
    final File file = new File(getActivity().getFilesDir(), ChatActivity
            .WALLPAPER_NAME);
    if (file.exists()){
      final ImageView imageView = (ImageView) getView().findViewById(R.id
              .chat_wallpaper);
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

  private void reloadMessages(){
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
          }else if (!MessageHistory.STATUS_RECEIVING.equals(msg.status))
            messageHistory.updateMessageStatus(buddyId, msg._ID, MessageHistory
                    .STATUS_READ);
        }else if (MessageHistory.STATUS_WAITING.equals(msg.status)){
          Upload.Task task = new Upload.Task(new File(msg.file), msg.chatId, msg
                  ._ID);
          new Upload().uploadFile(getContext(), task);
          msg.status = MessageHistory.STATUS_SENDING;
        }
        maa.add(msg);//send the read acknowledgement
        try{
          XmppManager.getInstance().sendAcknowledgement(buddyId,
                  msg.othersId, MessageHistory.STATUS_READ);
        }catch (Exception e){
          e.printStackTrace();
        }
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
        messageHistory.updateMessageStatus(chatName, msg._ID,
                MessageHistory.STATUS_RECEIVING);
        msg.status = MessageHistory.STATUS_RECEIVING;
        Intent intent = new Intent(getContext(), DownloadService.class);
        intent.setAction(DownloadService.DOWNLOAD_ACTION);
        intent.putExtra(DownloadService.PARAM_URL, msg.url);
        intent.putExtra(DownloadService.PARAM_RECEIVER, new
                DownloadReceiver(new Handler()));
        intent.putExtra(DownloadService.PARAM_FILE, msg.file);
        intent.putExtra(DownloadService.PARAM_MESSAGE_ID, msg._ID);
        getContext().startService(intent);
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
      if (resultCode == DownloadService.UPDATE_PROGRESS){
        Long messageId = resultData.getLong(DownloadService.PARAM_MESSAGE_ID);
        int progress = resultData.getInt(DownloadService.PARAM_PROGRESS);
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
                messageHistory.updateMessageStatus(chatName, messageId,
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
