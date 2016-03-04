package com.raspi.chatapp.ui.image;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.raspi.chatapp.R;
import com.raspi.chatapp.util.Constants;

public class ImageViewActivity extends AppCompatActivity implements
        SingleImageFragment.OnFragmentInteractionListener,
        OverviewImageFragment.OnFragmentInteractionListener{

  private String chatId;
  private long messageId;


  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_view);

    if (savedInstanceState == null){
      Bundle extras = getIntent().getExtras();
      if (extras != null && extras.containsKey(Constants.BUDDY_ID) && extras
              .containsKey(Constants.MESSAGE_ID)){
        chatId = extras.getString(Constants.BUDDY_ID);
        messageId = extras.getLong(Constants.MESSAGE_ID);
        getSupportFragmentManager().beginTransaction().add(
                R.id.fragment_container,
                SingleImageFragment.newInstance(chatId, messageId)).commit();
      }else if (extras != null && extras.containsKey(Constants.BUDDY_ID)){
        chatId = extras.getString(Constants.BUDDY_ID);
        messageId = -1;
        getSupportFragmentManager().beginTransaction().add(
                R.id.fragment_container,
                OverviewImageFragment.newInstance(chatId)).commit();
      }
    }
  }
}
