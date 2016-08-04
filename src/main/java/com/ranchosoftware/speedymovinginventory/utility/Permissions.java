package com.ranchosoftware.speedymovinginventory.utility;

/**
 * Created by rob on 7/17/16.
 */

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by rob on 1/17/16.
 */
public class Permissions {

  public static boolean hasPermission(Activity activity , String permission){
    return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
  }

  public static void requestPermission(Activity activity, String permission, int requestCode){
    ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
  }
}
