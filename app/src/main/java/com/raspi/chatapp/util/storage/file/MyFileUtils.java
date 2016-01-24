package com.raspi.chatapp.util.storage.file;

import android.os.Environment;

import com.raspi.chatapp.ui.chatting.ChatActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyFileUtils{
  public File getFileName() throws IOException{
    String root = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_PICTURES).getAbsolutePath();
    File myDir = new File(root + "/" + ChatActivity.IMAGE_DIR);
    myDir.mkdirs();
    String filePath = new File(myDir, "IMG_" + new SimpleDateFormat
            ("yyyy-MM-dd_HH-mm-ss").format(new Date())).getAbsolutePath();
    int i = 0;
    File file = new File(filePath + ".jpg");
    while (file.exists()){
      file.renameTo(new File(filePath + "_" + i + ".jpg"));
      i++;
    }
    return file;
  }

  public boolean isExternalStorageWritable(){
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)){
      return true;
    }
    return false;
  }
}
