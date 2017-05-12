package com.ranchosoftware.speedymovinginventory.utility;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.ranchosoftware.speedymovinginventory.R;
import com.ranchosoftware.speedymovinginventory.model.Job;

import java.util.UUID;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


public class Utility {

  // question for test. Will this wwork with null
  public static void popupError(Context context, String title, String message, DialogInterface.OnClickListener listener){
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title)
            .setMessage(message);
    if (listener != null){
      builder.setPositiveButton("Ok", listener);
    }
    builder.show();

  }


  public static void infoMessage(View view, Context context, String message){

    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }

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
      bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }
    return bitmap;
  }

  public static Drawable imageForLifecycle(Context context, Job.Lifecycle lifecycle, boolean active){

    int resource = 0;

      switch (lifecycle){
        case New:
          resource = active ? R.drawable.new_active : R.drawable.new_;
          break;
        case LoadedForStorage:
          resource = active ? R.drawable.loaded_for_storage_active : R.drawable.loaded_for_storage;
          break;
        case InStorage:
          resource = active ? R.drawable.in_storage_active : R.drawable.in_storage;
          break;
        case LoadedForDelivery:
          resource = active ? R.drawable.loaded_for_delivery_active : R.drawable.loaded_for_delivery;
          break;
        case Delivered:
          resource = active ? R.drawable.delivered_active : R.drawable.delivered;
          break;

    }
    return ResourcesCompat.getDrawable(context.getResources(), resource, null);
    //return context.getResources().getDrawable(resource);
  }

  public static boolean isValidEmailAddress(String email){
    boolean result = true;
    try {
      InternetAddress emailAddress = new InternetAddress(email);
      emailAddress.validate();

    } catch (AddressException ex){
      result = false;
    }
    return result;
  }
}
