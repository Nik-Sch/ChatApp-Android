package com.raspi.chatapp.ui.util.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * this drawable will contain a small image while loading the larger image.
 * If the large image has been loaded in the background the drawable
 * switches to it.
 */
public class AsyncDrawable extends BitmapDrawable{
  // keep reference to the workerTask
  private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

  /**
   * create an asyncDrawable
   *
   * @param res              the resources for the BitmapDrawable
   * @param bitmap           the small bitmap to be loaded instantly
   * @param bitmapWorkerTask the task that will load large bitmap in the
   *                         background
   */
  public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask
          bitmapWorkerTask){
    // create the bitmap with the small image
    super(res, bitmap);
    // create the reference to the workerTask
    bitmapWorkerTaskWeakReference = new WeakReference<>(bitmapWorkerTask);
  }

  public BitmapWorkerTask getBitmapWorkerTask(){
    // return what the reference holds
    return bitmapWorkerTaskWeakReference.get();
  }

  /**
   * this asyncTask will load the file in the background into a bitmap
   */
  public static class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap>{
    private final WeakReference<ImageView> imageViewWeakReference;
    private File data;
    private int width, height;

    /**
     * create a bitmapWorkerTask
     *
     * @param imageView the imageView which should show the image
     * @param width     the width of the imageView
     * @param height    the height of the imageView
     */
    public BitmapWorkerTask(ImageView imageView, int width, int height){
      // keep a weak reference to the imageView
      imageViewWeakReference = new WeakReference<>(imageView);
      // set width and height
      this.width = width;
      this.height = height;
    }

    @Override
    protected Bitmap doInBackground(File... params){
      if (!params[0].isFile())
        return null;
      data = params[0];

      // First decode with inJustDecodeBounds=true to check dimensions
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(data.getAbsolutePath(), options);

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, width, height, false);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      return BitmapFactory.decodeFile(data.getAbsolutePath(), options);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap){
      // if the task was cancelled set the bitmap to null to avoid half
      // loaded bitmaps
      if (isCancelled())
        bitmap = null;

      // if I still have reference to the imageView and if the bitmap exists
      if (imageViewWeakReference != null && bitmap != null){
        // get the bitmapWorkerTask and the imageView
        final ImageView imageView = imageViewWeakReference.get();
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask
                (imageView);
        // only if this is the correct workerTask and if the imageView still
        // exists set the bitmap
        if (this == bitmapWorkerTask && imageView != null)
          imageView.setImageBitmap(bitmap);
      }
    }
  }

  /**
   * if there is any work done by the bitmapWorkerTask of the imageView it is
   * interrupted.
   *
   * @param file      the file that should be loaded
   * @param imageView the imageView which should be checked for active tasks
   * @return true
   */
  public static boolean cancelPotentialWork(File file, ImageView imageView){
    // get the workerTask
    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
    // if there is a task
    if (bitmapWorkerTask != null){
      final File bitmapFile = bitmapWorkerTask.data;
      // if the file doesn't exist or is wrong cancel the task
      if (bitmapFile == null || bitmapFile != file)
        bitmapWorkerTask.cancel(true);
    }
    return true;
  }

  /**
   * returns the workerTask that is active on the imageViews bitmap
   *
   * @param imageView the imageView which bitmapWorkerTask should be returned
   * @return the bitmapWorkerTask
   */
  private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView){
    // there need to be an imageView
    if (imageView != null){
      // and the drawable needs to be async
      final Drawable drawable = imageView.getDrawable();
      if (drawable instanceof AsyncDrawable){
        // if this is the case return its workerTask
        final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
        return asyncDrawable.getBitmapWorkerTask();
      }
    }
    return null;
  }

  /**
   * calculates the sampleSize for the image to be loaded in.
   *
   * @param options   the current options for the bitmap
   * @param reqWidth  the required width of the imageView
   * @param reqHeight the required height of the imageView
   * @return the sampleSize
   */
  public static int calculateInSampleSize(
          BitmapFactory.Options options, int reqWidth, int reqHeight, boolean
          small){
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    // swap reqWidth and reqHeight if the orientation is different
    if ((height > width && reqWidth > reqHeight) ||
            (width > height && reqWidth < reqHeight)){
      int t = reqHeight;
      reqHeight = reqWidth;
      reqWidth = t;
    }

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
    if (small)
      return inSampleSize * 2;
    else
      return inSampleSize;
  }
}
