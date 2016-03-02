package com.raspi.chatapp.ui.util;

import android.content.Context;
import android.graphics.Matrix;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * this is basically a standard ImageView but the scaleType differs. It
 * scales the image pretty much the same as CROP_CENTER but doesn't crop to
 * the center but to the top. <br/>
 * That means it will scale the image so that the imageView will be filled
 * and if the images height is greater than the imageViews height, the bottom
 * of the image is cut off.
 */
public class WallpaperImageView extends AppCompatImageView{
  public WallpaperImageView(Context context){
    super(context);
    // set the scaleType to matrix for the matrix scaling to be applied
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  public WallpaperImageView(Context context, AttributeSet attrs, int defStyleAttr){
    super(context, attrs, defStyleAttr);
    // set the scaleType to matrix for the matrix scaling to be applied
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  public WallpaperImageView(Context context, AttributeSet attrs){
    super(context, attrs);
    // set the scaleType to matrix for the matrix scaling to be applied
    setScaleType(AppCompatImageView.ScaleType.MATRIX);
  }

  @Override
  protected boolean setFrame(int l, int t, int r, int b){
    try{
      // get the imageMatrix and calculate the scaleFactor
      Matrix matrix = getImageMatrix();
      float scaleFactor = r / (float) getDrawable()
              .getIntrinsicWidth();
      // scale the matrix and set the images matrix
      matrix.setScale(scaleFactor, scaleFactor, 0, 0);
      setImageMatrix(matrix);
    }catch (NullPointerException e){
      e.printStackTrace();
    }
    return super.setFrame(l, t, r, b);
  }
}
