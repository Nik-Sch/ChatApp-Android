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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

import com.raspi.chatapp.R;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconRecents;
import com.rockerhieu.emojicon.EmojiconRecentsManager;
import com.rockerhieu.emojicon.emoji.Emojicon;

import java.util.List;

public class EmojiPopupWindow extends PopupWindow implements ViewPager.OnPageChangeListener,
        EmojiconRecents{
  private OnSoftKeyboardOpenCloseListener mOnSoftKeyboardOpenCloseListener;
  private int mEmojiTabLastSelectedIndex = -1;
  private View[] mEmojiTabs;
  private PagerAdapter mEmojisAdapter;
  private EmojiconRecentsManager mRecentsManager;
  private Context mContext;
  private View rootView;


  public EmojiPopupWindow(View rootView, Context mContext){
    super(mContext);
    this.mContext = mContext;
    this.rootView = rootView;
    setContentView(createView());
    setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    setSize((int) mContext.getResources().getDimension(R.dimen.keyboard_height),
            LayoutParams.MATCH_PARENT);
  }

  public void setOnSoftKeyboardOpenCloseListener(OnSoftKeyboardOpenCloseListener listener){
    mOnSoftKeyboardOpenCloseListener = listener;
  }

  private View createView(){
    LayoutInflater inflater = LayoutInflater.from(mContext);
    View view = inflater.inflate(R.layout.emojicons, null, false);
    final ViewPager emojisPager = (ViewPager) view.findViewById(R.id.emojis_pager);
    emojisPager.addOnPageChangeListener(this);
    EmojiconRecents recents = this;
    mEmojisAdapter = new EmojisPagerAdapter(mContext.get)
    return null;
  }

  private void setSize(int width, int height){
    setWidth(width);
    setHeight(height);
  }


  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){

  }

  @Override
  public void onPageSelected(int position){

  }

  @Override
  public void onPageScrollStateChanged(int state){

  }

  @Override
  public void addRecentEmoji(Context context, Emojicon emojicon){

  }

  public interface OnSoftKeyboardOpenCloseListener{
    void onKeyboardOpen(int keyBoardHeight);

    void onKeyboardClose();
  }

  private class EmojisPagerAdapter extends FragmentStatePagerAdapter{
    List<EmojiconGridFragment> fragments;

    public EmojisPagerAdapter(FragmentManager fm, List<EmojiconGridFragment> fragments){
      super(fm);
      this.fragments = fragments;
    }

    @Override
    public int getCount(){
      return fragments.size();
    }

    @Override
    public Fragment getItem(int position){
      return fragments.get(position);
    }
  }
}
