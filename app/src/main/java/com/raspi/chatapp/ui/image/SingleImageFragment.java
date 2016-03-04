package com.raspi.chatapp.ui.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.image.AsyncDrawable;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;

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
    // activate the back button
    getActivity().findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        if (getActivity().getSupportFragmentManager()
                .getBackStackEntryCount() == 0)
          getActivity().finish();
        else
          getActivity().getSupportFragmentManager().popBackStack();
      }
    });
    TextView chatName = (TextView) getActivity().findViewById(R.id.chat_name);
    chatName.setText(mListener.getChatName());
    // get the imageView and load the image in the background
    viewPager = ((ViewPager) getActivity().findViewById(R.id.image));
    viewPager.setAdapter(new ImagePagerAdapter());
    viewPager.setPageTransformer(true, new DepthPageTransformer());
    if (viewPagerPosition == -1)
      viewPager.setCurrentItem(mListener.getCurrent() - 1);
    else
      viewPager.setCurrentItem(viewPagerPosition);
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
        updateInfo(position);
      }

      @Override
      public void onPageSelected(int position){
      }

      @Override
      public void onPageScrollStateChanged(int state){
      }
    });
  }

  private void updateInfo(int pos){
    // set the correct title
    TextView title = (TextView) getActivity().findViewById(R.id.image_index);
    title.setText(String.format(getResources().getString(R.string
            .single_image_view_title), pos + 1, mListener.getCount()));
    TextView description = (TextView) getActivity().findViewById(R.id
            .description);
    description.setText(mListener.getImageAtIndex(pos).description);
    if (description.getText() == null || description.getText().toString().isEmpty())
      description.setVisibility(View.GONE);
    else
      description.setVisibility(View.VISIBLE);
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
      imageView.setOnClickListener(new View.OnClickListener(){
        @Override
        public void onClick(View v){
          toggleOverlay();
        }
      });
      ImageMessage msg = mListener.getImageAtIndex(position);
//      new Thread(new LoadImageRunnable(
//              imageView, new Handler(), getContext(), new File(msg.file))).start();
      File file = new File(msg.file);
//      if (AsyncDrawable.cancelPotentialWork(file, imageView)){
//        String tmpFileName = new File(getContext().getFilesDir(), msg.chatId
//                + "-" + msg._ID + ".jpg").getAbsolutePath();
//        DisplayMetrics metrics = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        Log.d("loadBitmap", "Dimensions: " + metrics.widthPixels + ", " +
//                metrics.heightPixels);
//        final AsyncDrawable.BitmapWorkerTask task =
//                new AsyncDrawable.BitmapWorkerTask(imageView,
//                        metrics.widthPixels,
//                        metrics.heightPixels);
//        imageView.setImageDrawable(new AsyncDrawable(
//                getContext().getResources(),
//                new File(tmpFileName).isFile()
//                        ? BitmapFactory.decodeFile(tmpFileName)
//                        : BitmapFactory.decodeResource(getContext().getResources(),
//                        R.drawable.placeholder),
//                task));
//      }
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
