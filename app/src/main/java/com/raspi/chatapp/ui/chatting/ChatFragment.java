package com.raspi.chatapp.ui.chatting;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.Uri;
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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.message_array.Date;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

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
  private BroadcastReceiver MessageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      String intentBuddyId = extras.getString(ChatActivity.BUDDY_ID);
      int index = intentBuddyId.indexOf('@');
      if (index >= 0)
        intentBuddyId = intentBuddyId.substring(0, index);
      if (buddyId.equals(intentBuddyId)){
        MessageArrayContent mac = messageHistory.getLastMessage(buddyId, true);
        if (mac instanceof ImageMessage)
          downloadImage((ImageMessage) mac);
        maa.add(mac);

        abortBroadcast();
      }
    }
  };
  private BroadcastReceiver PresenceChangeReceiver = new BroadcastReceiver(){
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
  private BroadcastReceiver disconnectedReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      updateStatus(null);
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
    getContext().registerReceiver(MessageReceiver, filter);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance
            (getContext());
    LBmgr.registerReceiver(reconnectedReceiver, new IntentFilter
            (ChatActivity.RECONNECTED));
    LBmgr.registerReceiver(disconnectedReceiver, new IntentFilter
            (ChatActivity.DISCONNECTED));
    LBmgr.registerReceiver(PresenceChangeReceiver, new IntentFilter
            (ChatActivity.PRESENCE_CHANGED));
    uploadReceiver.register(getContext());
    initUI();
  }

  @Override
  public void onPause(){
    InputMethodManager mgr = (InputMethodManager) getContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE);
    mgr.hideSoftInputFromWindow(getView().findViewById(R.id.chat_in)
            .getWindowToken(), 0);
    getContext().unregisterReceiver(MessageReceiver);
    LocalBroadcastManager LBmgr = LocalBroadcastManager.getInstance(getContext());
    LBmgr.unregisterReceiver(PresenceChangeReceiver);
    LBmgr.unregisterReceiver(reconnectedReceiver);
    LBmgr.unregisterReceiver(disconnectedReceiver);
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
      case R.id.action_settings:
        return false;
      case R.id.action_attach:
        mListener.onAttachClicked();
        return true;
      case R.id.home:
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
    if (actionBar != null)
      actionBar.setTitle(chatName);
    maa = new MessageArrayAdapter(getContext(), R.layout.message_text);

    listView = (ListView) getView().findViewById(R.id.chat_listview);
    textIn = (EditText) getView().findViewById(R.id.chat_in);
    Button sendBtn = (Button) getView().findViewById(R.id.chat_sendBtn);

    sendBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        String message = textIn.getText().toString();
        String status = sendTextMessage(message)
                ? MessageHistory.STATUS_SENT
                : MessageHistory.STATUS_WAITING;
        messageHistory.addMessage(buddyId, getContext().getSharedPreferences
                (ChatActivity.PREFERENCES, 0).getString(ChatActivity
                .USERNAME, ""), MessageHistory.TYPE_TEXT, message, status);
        textIn.setText("");
        maa.add(new TextMessage(false, message, new GregorianCalendar()
                .getTimeInMillis(), status));
      }
    });

    listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
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
        }
      }
    });

    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener(){

      int count = 0;
      int wrongSelected = 0;
      Menu menu;

      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked){
        //TODO update number in CAB
        //this does not work perfectly, if you select an item below an date
        // or newMessage and afterwards select the date or newMessage it gets
        // confused... I don't really care atm
        MessageArrayContent mac = maa.getItem(position);
        if (mac instanceof Date || mac instanceof NewMessage){
          try{
            listView.setItemChecked(position + 1, checked);
          }catch (Exception e){
          }
          if (checked)
            wrongSelected++;
          else
            wrongSelected--;
        }
        if (checked)
          count++;
        else
          count--;
        MenuItem itemCopy = menu.findItem(R.id.action_copy);
        itemCopy.setVisible((count - wrongSelected) == 1);
        if (count == wrongSelected){
          mode.finish();
          wrongSelected = 0;
          count = 0;
        }
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
        return true;
      }

      @Override
      public boolean onActionItemClicked(final ActionMode mode, MenuItem item){
        boolean result = false;
        final int size;
        SparseBooleanArray checked;
        switch (item.getItemId()){
          case R.id.action_copy:
            size = listView.getCount();
            checked = listView.getCheckedItemPositions();
            for (int i = 0; i < size; i++)
              if (checked.get(i)){
                MessageArrayContent mac = (MessageArrayContent) listView
                        .getItemAtPosition(i);
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
                break;
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
                        SparseBooleanArray checked = listView
                                .getCheckedItemPositions();
                        ArrayList<Long> deletedRows = new ArrayList<>();
                        ArrayList<Integer> deletedIndices = new ArrayList<>();

                        int i = -1;
                        for (MessageArrayContent mac : maa){
                          if (checked.get(++i)){
                            if (mac instanceof TextMessage){
                              deletedRows.add(((TextMessage) mac)._ID);
                              deletedIndices.add(i);
                            }else if (mac instanceof ImageMessage){
                              deletedRows.add(((ImageMessage) mac)._ID);
                              deletedIndices.add(i);
                            }
                          }
                        }

                        for (int j : deletedIndices)
                          maa.remove(maa.getCount()-1);

                        long[] array = convertArray(deletedRows);
                        messageHistory.removeMessages(buddyId, array);

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

      private long[] convertArray(ArrayList<Long> list){
        long[] array = new long[list.size()];
        for (int i = 0; i < array.length; i++)
          array[i] = list.get(i);
        return array;
      }


      @Override
      public void onDestroyActionMode(ActionMode mode){
        count = 0;
      }
    });

    maa.registerDataSetObserver(new DataSetObserver(){
      @Override
      public void onChanged(){
        super.onChanged();
        listView.setSelection(maa.getCount() - 1);
      }
    });
    reloadMessages();
    String lastOnline = messageHistory.getOnline(buddyId);
    updateStatus(lastOnline);
  }

  private boolean sendTextMessage(String message){
    XmppManager xmppManager = XmppManager.getInstance(getContext());
    return (xmppManager.sendTextMessage(message, buddyId));
  }

  private void resendTextMessage(TextMessage msg){
    boolean status = sendTextMessage(msg.message);
    if (status){
      messageHistory.updateMessageStatus(buddyId, msg._ID, MessageHistory.STATUS_SENT);
      msg.status = MessageHistory.STATUS_SENT;
      int size = maa.getCount();
      for (int i = 0; i < size; i++){
        MessageArrayContent mac = maa.getItem(i);
        if (mac instanceof TextMessage && ((TextMessage) mac)._ID == msg._ID)
          updateMessage(i);
      }
    }
  }

  private void reloadMessages(){
    maa.clear();
    MessageArrayContent[] messages = messageHistory.getMessages(buddyId, MESSAGE_LIMIT);
    long oldDate = 0;
    final int c = 24 * 60 * 60 * 1000;
    NewMessage nm = null;
    for (MessageArrayContent message : messages){
      if (message instanceof TextMessage){
        TextMessage msg = (TextMessage) message;
        if ((msg.time - oldDate) / c > 0)
          maa.add(new Date(msg.time));
        oldDate = msg.time;
        if (msg.left){
          if (MessageHistory.STATUS_RECEIVED.equals(msg.status))
            if (nm == null){
              nm = new NewMessage(getResources().getString(R.string
                      .new_message));
              maa.add(nm);
            }else
              nm.status = getResources().getString(R.string.new_messages);
          messageHistory.updateMessageStatus(buddyId, 2, MessageHistory
                  .STATUS_READ);
        }else if (MessageHistory.STATUS_WAITING.equals(msg.status))
          resendTextMessage(msg);
        maa.add(msg);
      }else if (message instanceof ImageMessage){
        ImageMessage msg = (ImageMessage) message;
        if ((msg.time - oldDate) / c > 0)
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
        maa.add(msg);
      }
    }
  }

  private void updateStatus(String lastOnline){
    try{
      long time = Long.valueOf(lastOnline);
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
    }catch (NumberFormatException e){
      if (actionBar != null)
        if (lastOnline != null)
          actionBar.setSubtitle(Html
                  .fromHtml("<font " +
                          "color='#55AAFF'>" + lastOnline + "</font>"));
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
