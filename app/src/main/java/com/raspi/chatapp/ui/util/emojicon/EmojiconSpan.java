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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import java.lang.ref.WeakReference;

class EmojiconSpan extends DynamicDrawableSpan{
  private final Context context;
  private final int resourceId;
  private final int size;
  private final int textSize;
  private int height;
  private int width;
  private int top;
  private Drawable drawable;
  private WeakReference<Drawable> drawableReference;

  public EmojiconSpan(Context context, int resourceId, int size, int alignment, int textSize){
    super(alignment);
    this.context = context;
    this.resourceId = resourceId;
    this.width = this.height = this.size = size;
    this.textSize = textSize;
  }

  @Override
  public Drawable getDrawable(){
    if (drawable == null){
      try{
        drawable = context.getResources().getDrawable(resourceId);
        height = size;
        width = height * drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
        top = (textSize - height) / 2;
        drawable.setBounds(0, top, width, top + height);
      }catch (Exception e){
      }
    }
    return drawable;
  }

  @Override
  public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint){
    Drawable d = getCachedDrawable();
    canvas.save();

    int transY = bottom - d.getBounds().bottom;
    if (mVerticalAlignment == ALIGN_BASELINE)
      transY = top + ((bottom - top) / 2) - ((d.getBounds().bottom - d.getBounds().top) / 2) - top;
    canvas.translate(x, transY);
    d.draw(canvas);
    canvas.restore();
  }

  public Drawable getCachedDrawable(){
    if (drawableReference == null || drawableReference.get() == null)
      drawableReference = new WeakReference<>(getDrawable());
    return drawableReference.get();
  }
}
