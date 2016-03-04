package com.raspi.chatapp.ui.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

import com.raspi.chatapp.util.storage.file.FileUtils;

import java.io.File;


public class LoadImageRunnable implements Runnable{
  Handler mHandler;
  ImageView imageView;
  Context context;
  Uri imageUri;
  File imageFile;

  public LoadImageRunnable(ImageView imageView, Handler mHandler, Context
          context, Uri imageUri){
    this.mHandler = mHandler;
    this.imageView = imageView;
    this.context = context;
    this.imageUri = imageUri;
  }

  public LoadImageRunnable(ImageView imageView, Handler mHandler, Context
          context, File imageFile){
    this.mHandler = mHandler;
    this.imageView = imageView;
    this.context = context;
    this.imageFile = imageFile;
  }

  @Override
  public void run(){
    final String imagePath = (imageUri != null)
            ? FileUtils.getPath(context, imageUri)
            : imageFile.getAbsolutePath();

    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imagePath, options);

    // Calculate inSampleSize
    DisplayMetrics d = new DisplayMetrics();
    ((WindowManager) context.getSystemService(Context
            .WINDOW_SERVICE)).getDefaultDisplay().getMetrics(d);
    options.inSampleSize = calculateInSampleSize(options, d.widthPixels / 2, d
            .heightPixels / 2);

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false;
    mHandler.post(new Runnable(){
      @Override
      public void run(){
        imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath, options));
      }
    });
  }

  /**
   * calculates the sampleSize of the image
   *
   * @param options   the options of the Bitmap
   * @param reqWidth  the width you want the image in
   * @param reqHeight the height you want the image in
   * @return the sample size which is preferred for loading the image for the
   * given width and height
   */
  private int calculateInSampleSize(
          BitmapFactory.Options options, int reqWidth, int reqHeight){
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    // if I actually want to sample more
    if (height > reqHeight || width > reqWidth){

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight
              && (halfWidth / inSampleSize) > reqWidth){
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }
}
