package com.raspi.chatapp.ui.chatting;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.raspi.chatapp.R;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;

/**
 * this all probably shouldn't exist and be implemented as popup inside the
 * chatFragment, let's see
 */
public class ChatEntryActionListFragment extends DialogFragment{

  private String buddyId;

  public ChatEntryActionListFragment(){
    super();
    Bundle arguments = getArguments();
    if (arguments != null && arguments.containsKey(Constants.BUDDY_ID))
      buddyId = arguments.getString(Constants.BUDDY_ID);
  }

  private final DialogInterface.OnClickListener onClickListener = new
          DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which){
              //The array is the following:
              //{rename, delete}
              MessageHistory messageHistory = new MessageHistory(getContext());
            }
          };

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState){
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setItems(R.array.chatlist_options_array, onClickListener);
    return super.onCreateDialog(savedInstanceState);
  }
}
