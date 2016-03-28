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
package com.raspi.chatapp.util.storage.file;

import android.os.Environment;

import com.raspi.chatapp.util.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyFileUtils{
  public static File getFileName(){
    String root = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_PICTURES).getAbsolutePath();
    File myDir = new File(root + "/" + Constants.IMAGE_DIR);
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

  public static boolean isExternalStorageWritable(){
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)){
      return true;
    }
    return false;
  }
}
