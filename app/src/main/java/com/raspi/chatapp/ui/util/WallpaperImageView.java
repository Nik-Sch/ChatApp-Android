package com.raspi.chatapp.ui.util;

import android.content.Context;
import android.graphics.Matrix;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class WallpaperImageView extends AppCompatImageView{
  public WallpaperImageView(Context context){
    super(context);
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  public WallpaperImageView(Context context, AttributeSet attrs, int defStyleAttr){
    super(context, attrs, defStyleAttr);
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  public WallpaperImageView(Context context, AttributeSet attrs){
    super(context, attrs);
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  @Override
  protected boolean setFrame(int l, int t, int r, int b){
    try{
      Matrix matrix = getImageMatrix();
      float scaleFactor = r / (float) getDrawable()
              .getIntrinsicWidth();
      matrix.setScale(scaleFactor, scaleFactor, 0, 0);
      setImageMatrix(matrix);
    }catch (NullPointerException e){
      e.printStackTrace();
    }
    return super.setFrame(l, t, r, b);
  }
}
