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
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import com.raspi.chatapp.R;

public class EmojiconEditText extends EditText{
  private int emojiconSize;
  private int emojiconAlignment;
  private int emojiconTextSize;

  public EmojiconEditText(Context context){
    super(context);
    emojiconSize = (int) getTextSize();
    emojiconTextSize = emojiconSize;
  }

  public EmojiconEditText(Context context, AttributeSet attrs){
    super(context, attrs);
    init(attrs);
  }

  public EmojiconEditText(Context context, AttributeSet attrs, int defStyleAttr){
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(AttributeSet attrs){
    TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Emojicon);
    emojiconSize = (int) a.getDimension(R.styleable.Emojicon_emojiconSize, getTextSize());
    emojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan
            .ALIGN_BASELINE);
    a.recycle();
    emojiconTextSize = (int) getTextSize();
    setText(getText());
  }

  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter){
    updateText();
  }

  private void updateText(){
    EmojiconHandler.addEmojis(getContext(), getText(), emojiconSize, emojiconAlignment, emojiconTextSize);
  }

  /**
   * Set the size of emojicons in pixels.
   * @param pixels the size in pixels
   */
  public void setEmojiconSize(int pixels){
    emojiconSize = pixels;
    updateText();
  }
}
