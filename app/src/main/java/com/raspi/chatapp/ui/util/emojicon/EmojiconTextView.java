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
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.raspi.chatapp.R;

public class EmojiconTextView extends TextView{
  private int emojiconSize;
  private int emojiconAlignment;
  private int emojiconTextSize;
  private int textStart = 0;
  private int textLength = -1;

  public EmojiconTextView(Context context){
    super(context);
    init(null);
  }

  public EmojiconTextView(Context context, AttributeSet attrs){
    super(context, attrs);
    init(attrs);
  }

  public EmojiconTextView(Context context, AttributeSet attrs, int defStyleAttr){
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(AttributeSet attrs){
    emojiconTextSize = (int) getTextSize();
    if (attrs == null)
      emojiconSize = emojiconTextSize;
    else{
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Emojicon);
      emojiconSize = (int) a.getDimension(R.styleable.Emojicon_emojiconSize, getTextSize());
      emojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment,
              DynamicDrawableSpan.ALIGN_BASELINE);
      textStart = a.getInteger(R.styleable.Emojicon_emojiconTextStart, 0);
      textLength = a.getInteger(R.styleable.Emojicon_emojiconTextLength, -1);
      a.recycle();
    }
    setText(getText());
  }

  @Override
  public void setText(CharSequence text, BufferType type){
    if (!TextUtils.isEmpty(text)){
      SpannableStringBuilder builder = new SpannableStringBuilder(text);
      EmojiconHandler.addEmojis(getContext(), builder, emojiconSize, emojiconAlignment,
              emojiconTextSize, textStart, textLength);
      text = builder;
    }
    super.setText(text, type);
  }

  /**
   * sets the size of emojicons in pixels.
   * @param pixels the size in pixels
   */
  public void setEmojiconSize(int pixels){
    emojiconSize = pixels;
    super.setText(getText());
  }
}
