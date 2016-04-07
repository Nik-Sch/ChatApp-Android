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
package com.raspi.chatapp.ui.util.emojicon;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.emojicon.EmojiconGridView.OnEmojiconClickedListener;
import com.raspi.chatapp.ui.util.emojicon.emoji.Emojicon;
import com.raspi.chatapp.ui.util.emojicon.emoji.Nature;
import com.raspi.chatapp.ui.util.emojicon.emoji.Objects;
import com.raspi.chatapp.ui.util.emojicon.emoji.People;
import com.raspi.chatapp.ui.util.emojicon.emoji.Places;
import com.raspi.chatapp.ui.util.emojicon.emoji.Symbols;

import java.util.Arrays;
import java.util.List;

public class EmojiconPopup extends PopupWindow implements ViewPager.OnPageChangeListener, EmojiconRecents{
  private View rootView;
  private int lastSelectedTab = -1;
  private View[] tabs;
  private PagerAdapter emojisAdapter;
  private EmojiconRecentsManager recentsManager;
  private ViewPager emojiconPager;
  private int keyboardHeight = 0;
  private boolean pendingOpen = false;
  private boolean isOpen = false;
  private Context context;
  private OnSoftKeyboardOpenCloseListener onSoftKeyboardOpenCloseListener;
  private OnEmojiconClickedListener onEmojiconClickedListener;
  private OnEmojiconBackspaceClickedListener onEmojiconBackspaceClickedListener;

  public EmojiconPopup(View rootView, Context context, OnEmojiconClickedListener onEmojiconClickedListener){
    super(context);
    this.context = context;
    this.rootView = rootView;
    this.onEmojiconClickedListener = onEmojiconClickedListener;
    setContentView(createInnerView());
    setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    setSize(LayoutParams.MATCH_PARENT, (int) context.getResources().
            getDimension(R.dimen.keyboard_height));
  }

  public void setOnSoftKeyboardOpenCloseListener(OnSoftKeyboardOpenCloseListener onSoftKeyboardOpenCloseListener){
    this.onSoftKeyboardOpenCloseListener = onSoftKeyboardOpenCloseListener;
  }

  public void setOnEmojiconBackspaceClickedListener(OnEmojiconBackspaceClickedListener onEmojiconBackspaceClickedListener){
    this.onEmojiconBackspaceClickedListener = onEmojiconBackspaceClickedListener;
  }

  /**
   * Shows the emojicon popup.<br/>
   * NOTE: If the softKeyboard has not been shown yet the size for the popup is not defined yet.
   * See {@link #showAtBottomPending} for further information
   */
  public void showAtBottom(){
    showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
  }

  /**
   * Shows the emojicon popup as soon as the soft keyboard is open.<br/>
   * As the size of the soft keyboard varies from phone to phone, it needs to opened at least
   * once before to measure the size.
   */
  public void showAtBottomPending(){
    if (isOpen)
      showAtBottom();
    else
      pendingOpen = true;
  }

  public boolean isKeyboardOpen(){
    return isOpen;
  }

  /**
   * set the size automatically to the softKeyboard size (if one exists)
   */
  public void setSoftKeyboardSize(){
    rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
      @Override
      public void onGlobalLayout(){
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);

        int screenHeight = getUsableScreenHeight();
        int heightDiff = screenHeight - (r.bottom - r.top);
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0)
          heightDiff -= context.getResources().getDimensionPixelSize(resourceId);
        if (heightDiff > 100){
          keyboardHeight = heightDiff;
          setSize(LayoutParams.MATCH_PARENT, keyboardHeight);
          if (!isOpen && onSoftKeyboardOpenCloseListener != null)
            onSoftKeyboardOpenCloseListener.onKeyboardOpen(keyboardHeight);
          isOpen = true;
          if (pendingOpen){
            showAtBottom();
            pendingOpen = false;
          }
        }else{
          isOpen = false;
          if (onSoftKeyboardOpenCloseListener != null)
            onSoftKeyboardOpenCloseListener.onKeyboardClose();
        }
      }

      private int getUsableScreenHeight(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
          DisplayMetrics metrics = new DisplayMetrics();

          WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
          manager.getDefaultDisplay().getMetrics(metrics);
          return metrics.heightPixels;
        }
        return rootView.getRootView().getHeight();
      }
    });
  }

  /**
   * manually set the size of the popup
   *
   * @param width  width
   * @param height height
   */
  public void setSize(int width, int height){
    setWidth(width);
    setHeight(height);
  }

  private View createInnerView(){
    LayoutInflater inflater = LayoutInflater.from(context);
    View v = inflater.inflate(R.layout.emojicon_popup, null, false);
    emojiconPager = (ViewPager) v.findViewById(R.id.emojicon_pager);
    emojiconPager.addOnPageChangeListener(this);
    emojisAdapter = new EmojiconPagerAdapter(Arrays.asList(
            new EmojiconRecentsGridView(context, onEmojiconClickedListener),
            new EmojiconGridView(context, People.DATA, this, onEmojiconClickedListener),
            new EmojiconGridView(context, Nature.DATA, this, onEmojiconClickedListener),
            new EmojiconGridView(context, Objects.DATA, this, onEmojiconClickedListener),
            new EmojiconGridView(context, Places.DATA, this, onEmojiconClickedListener),
            new EmojiconGridView(context, Symbols.DATA, this, onEmojiconClickedListener)
    ));
    emojiconPager.setAdapter(emojisAdapter);
    tabs = new View[6];
    tabs[0] = v.findViewById(R.id.emojis_tab_0_recents);
    tabs[1] = v.findViewById(R.id.emojis_tab_1_people);
    tabs[2] = v.findViewById(R.id.emojis_tab_2_nature);
    tabs[3] = v.findViewById(R.id.emojis_tab_3_objects);
    tabs[4] = v.findViewById(R.id.emojis_tab_4_cars);
    tabs[5] = v.findViewById(R.id.emojis_tab_5_punctuation);
    for (int i = 0; i < tabs.length; i++){
      final int p = i;
      tabs[p].setOnClickListener(new View.OnClickListener(){
        @Override
        public void onClick(View v){
          emojiconPager.setCurrentItem(p);
        }
      });
    }

    v.findViewById(R.id.emojis_backspace).setOnTouchListener(new RepeatListener(1000, 50, new
            View.OnClickListener(){

              @Override
              public void onClick(View v){
                if (onEmojiconBackspaceClickedListener != null)
                  onEmojiconBackspaceClickedListener.onEmojiconBackspaceClicked(v);
              }
            }));

    recentsManager = EmojiconRecentsManager.getInstance(context);
    int page = recentsManager.getRecentPage();
    // if the recent page is the recents but there are no recents go to first page
    if (page == 0 && recentsManager.size() == 0)
      page = 1;
    if (page == 0)
      onPageSelected(0);
    else
      emojiconPager.setCurrentItem(page, true);
    return v;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
  }

  @Override
  public void onPageSelected(int position){
    if (lastSelectedTab == position)
      return;
    if (position < 6){
      if (lastSelectedTab >= 0 && lastSelectedTab < tabs.length)
        tabs[lastSelectedTab].setSelected(false);
      tabs[position].setSelected(true);
      lastSelectedTab = position;
      recentsManager.setRecentPage(position);
    }
  }

  @Override
  public void onPageScrollStateChanged(int state){
  }

  @Override
  public void addRecentEmoji(Context context, Emojicon emojicon){
    ((EmojiconPagerAdapter) emojiconPager.getAdapter()).getRecentView()
            .addRecentEmoji(context, emojicon);
  }

  private static class EmojiconPagerAdapter extends PagerAdapter{

    private List<EmojiconGridView> views;

    public EmojiconPagerAdapter(List<EmojiconGridView> emojiconGridViews){
      super();
      views = emojiconGridViews;
    }

    @Override
    public int getCount(){
      return views.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position){
      View v = views.get(position).rootView;
      container.addView(v, 0);
      return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
      container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object){
      return view == object;
    }

    public EmojiconRecentsGridView getRecentView(){
      for (EmojiconGridView v : views)
        if (v instanceof EmojiconRecentsGridView)
          return (EmojiconRecentsGridView) v;
      return null;
    }
  }

  private class RepeatListener implements View.OnTouchListener{

    private Handler handler = new Handler();

    private final int initInterval;
    private final int normalInterval;
    private final View.OnClickListener onClickListener;

    private View downView;

    public RepeatListener(int initInterval, int normalInterval, View.OnClickListener onClickListener){
      if (onClickListener == null)
        throw new IllegalArgumentException("onClickListener must not be null");
      if (initInterval < 0 || normalInterval < 0)
        throw new IllegalArgumentException("interval must not be negative");
      this.initInterval = initInterval;
      this.normalInterval = normalInterval;
      this.onClickListener = onClickListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
      switch (event.getAction()){
        case MotionEvent.ACTION_DOWN:
          downView = v;
          handler.removeCallbacks(handlerRunnable);
          handler.postAtTime(handlerRunnable, downView, SystemClock.uptimeMillis() + initInterval);
          onClickListener.onClick(v);
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_OUTSIDE:
          handler.removeCallbacksAndMessages(downView);
          downView = null;
          return true;
        default:
          return false;
      }
    }

    private Runnable handlerRunnable = new Runnable(){
      @Override
      public void run(){
        if (downView == null)
          return;
        handler.removeCallbacksAndMessages(downView);
        handler.postAtTime(this, downView, SystemClock.uptimeMillis() + normalInterval);
        onClickListener.onClick(downView);
      }
    };
  }

  public interface OnSoftKeyboardOpenCloseListener{
    void onKeyboardOpen(int keyboardHeight);

    void onKeyboardClose();
  }

  public interface OnEmojiconBackspaceClickedListener{
    void onEmojiconBackspaceClicked(View view);
  }
}
