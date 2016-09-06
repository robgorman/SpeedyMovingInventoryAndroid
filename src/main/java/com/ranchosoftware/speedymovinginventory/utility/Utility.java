package com.ranchosoftware.speedymovinginventory.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.UUID;

/**
 * Created by rob on 7/15/16.
 */

public class Utility {

  public static void error(View view, Context context, String message){

    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }

  public static void error(View view, Context context, int resourceId){
    String message = context.getResources().getString(resourceId);
    if (message == null){
      message = "Message lookup failed";
    }
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }

  public static boolean isQrcCodeValid(String code){
    // valid QRC codes are UUIDS 216687b2-3c9b-4b71-8cb8-75a775af43b8

    // use the UUID class to validate
    try {
      UUID uid = UUID.fromString(code);
      return true;
    } catch (Exception e){
      return false;
    }

  }

  public static Bitmap rotateImage(Bitmap source, float angle){
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }
  public static  Bitmap scaleBitmapIfNecessary(Bitmap bitmap){
    int maxSize = Math.max(bitmap.getWidth(), bitmap.getHeight());
    // bitmap to be at most 800
    int newWidth = 0;
    int newHeight = 0;
    if (maxSize > 800){
      if (bitmap.getWidth() > bitmap.getHeight()){
        newWidth = 800;
        newHeight = (int) (bitmap.getHeight() * (800.0/ (double)bitmap.getWidth()));
      } else {
        newHeight = 800;
        newWidth = (int) (bitmap.getWidth() * (800.0/(double) bitmap.getHeight()));
      }
      bitmap = bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }
    return bitmap;
  }
}
