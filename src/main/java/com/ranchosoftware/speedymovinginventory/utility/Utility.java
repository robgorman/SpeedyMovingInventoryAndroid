package com.ranchosoftware.speedymovinginventory.utility;

import android.content.Context;
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
}
