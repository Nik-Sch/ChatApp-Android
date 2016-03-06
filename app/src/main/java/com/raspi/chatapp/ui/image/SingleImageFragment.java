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
package com.raspi.chatapp.ui.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.image.AsyncDrawable;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SingleImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SingleImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SingleImageFragment extends Fragment{

  private static final String VIEW_PAGE_ITEM = "com.raspi.chatapp.ui.util" +
          ".image.SingleImageFragment.currentItem";

  private int viewPagerPosition = -1;
  private boolean init = true;
  private boolean overlayActive = true;
  private ViewPager viewPager;

  private OnFragmentInteractionListener mListener;

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment SingleImageFragment.
   */
  public static SingleImageFragment newInstance(){
    SingleImageFragment fragment = new SingleImageFragment();
    fragment.setArguments(new Bundle());
    return fragment;
  }

  public SingleImageFragment(){
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null){
      viewPagerPosition = savedInstanceState.getInt(VIEW_PAGE_ITEM, 0);
    }
    ActionBar actionBar = ((AppCompatActivity) getActivity())
            .getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);
  }

  @Override
  public void onResume(){
    super.onResume();
    if (init)
      initUI();
    else if (viewPagerPosition != -1)
      viewPager.setCurrentItem(viewPagerPosition);
    init = false;
  }

  @Override
  public void onPause(){
    viewPagerPosition = viewPager.getCurrentItem();
    super.onPause();
  }

  private void initUI(){
    // set the actionBars background to transparent
    ActionBar actionBar = ((AppCompatActivity) getActivity())
            .getSupportActionBar();
    actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor
            (R.color.action_bar_transparent)));
    // set the statusBar color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getActivity().getWindow().setStatusBarColor(getResources().getColor(R
              .color.action_bar_transparent));
    TextView chatName = (TextView) getActivity().findViewById(R.id.chat_name);
    chatName.setText(mListener.getChatName());
    // get the imageView and load the image in the background
    viewPager = ((ViewPager) getActivity().findViewById(R.id.image));
    viewPager.setAdapter(new ImagePagerAdapter());
    viewPager.setPageTransformer(true, new DepthPageTransformer());
    if (viewPagerPosition == -1)
      viewPager.setCurrentItem(mListener.getCurrent());
    else
      viewPager.setCurrentItem(viewPagerPosition);
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
      }

      @Override
      public void onPageSelected(int position){
        updateInfo(position);
      }

      @Override
      public void onPageScrollStateChanged(int state){
      }
    });
    showOverlay(true);
    updateInfo(viewPager.getCurrentItem());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      case android.R.id.home:
        if (getActivity().getSupportFragmentManager()
                .getBackStackEntryCount() == 0)
          getActivity().finish();
        else
          getActivity().getSupportFragmentManager().popBackStack();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void updateInfo(int pos){
    // set the correct title
    ActionBar actionBar = ((AppCompatActivity) getActivity())
            .getSupportActionBar();
    actionBar.setTitle(String.format(getResources().getString(R.string
            .single_image_view_title), pos + 1, mListener.getCount()));
    // set the description
    TextView description = (TextView) getActivity().findViewById(R.id
            .description);
    description.setText(mListener.getImageAtIndex(pos).description);
    // make it gone if there is none for the layout to be smaller, and,
    // therefore, the transparent background
    if (description.getText() == null || description.getText().toString().isEmpty())
      description.setVisibility(View.GONE);
    else
      description.setVisibility(View.VISIBLE);
    // update the time of the message
    TextView dateTime = (TextView) getActivity().findViewById(R.id.date);
    dateTime.setText(String.format(getResources()
                    .getString(R.string.date_and_time),
            mListener.getImageAtIndex(mListener.getCurrent()).time));
  }

  private void showOverlay(boolean active){
    final View imageInfo = getActivity().findViewById(R.id.image_info);
    Animation anim;
    ActionBar actionBar = ((AppCompatActivity) getActivity())
            .getSupportActionBar();
    if (active){
      View decorView = getActivity().getWindow().getDecorView();
      int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      decorView.setSystemUiVisibility(uiOptions);
      actionBar.show();
      anim = AnimationUtils.loadAnimation(getContext(), R.anim
              .bottom_in);
      anim.setDuration(300);
      imageInfo.setVisibility(View.VISIBLE);
      imageInfo.startAnimation(anim);

    }else{
      View decorView = getActivity().getWindow().getDecorView();
      int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN |
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      decorView.setSystemUiVisibility(uiOptions);
      actionBar.hide();
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
    }
    overlayActive = active;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // set the status bar color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
      getActivity().getWindow().setStatusBarColor(Color.BLACK);
    }
    setHasOptionsMenu(true);
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_single_image, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
    menu.clear();
    inflater.inflate(R.menu.menu_single_image, menu);
  }

  @Override
  public void onSaveInstanceState(Bundle outState){
    super.onSaveInstanceState(outState);
    outState.putInt(VIEW_PAGE_ITEM, viewPager.getCurrentItem());
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

  private class DepthPageTransformer implements ViewPager.PageTransformer{
    private static final float MIN_SCALE = 0.5f;

    @Override
    public void transformPage(View page, float position){
      int width = page.getWidth();

      // offscreen to the left
      if (position < -1)
        page.setAlpha(0);
      else if (position <= 0){// moving to the left
        page.setAlpha(1);
        page.setTranslationX(0);
        page.setScaleX(1);
        page.setScaleY(1);
      }else if (position <= 1){// moving to the right
        page.setAlpha(1 - position);
        page.setTranslationX(width * -position);
        float scale = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
        page.setScaleX(scale);
        page.setScaleY(scale);
      }else// offscreen to the right
        page.setAlpha(0);
    }
  }

  private class ImagePagerAdapter extends PagerAdapter{

    @Override
    public int getCount(){
      return mListener.getCount();
    }

    @Override
    public boolean isViewFromObject(View view, Object object){
      return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position){
      ImageView imageView = new ImageView(getContext());
      imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
      // decode the file into a bitmap and show it afterwards
      ImageMessage msg = mListener.getImageAtIndex(position);
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(msg.file, options);

      DisplayMetrics metrics = new DisplayMetrics();
      getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
      // Calculate inSampleSize
      options.inSampleSize = AsyncDrawable.calculateInSampleSize(options,
              metrics.widthPixels, metrics.heightPixels, true);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      Bitmap bitmap = BitmapFactory.decodeFile(msg.file, options);
      Log.d("loadBitmap", "Dimensions: " + bitmap.getWidth() + ", " +
              bitmap.getHeight());
      imageView.setImageBitmap(bitmap);
      PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);
      attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener(){
        @Override
        public void onViewTap(View view, float x, float y){
          showOverlay(!overlayActive);
        }
      });
      container.addView(imageView, 0);
      return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
      container.removeView((ImageView) object);
    }
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
    int getCount();

    ImageMessage getImageAtIndex(int index);

    int getCurrent();

    String getChatName();
  }
}
