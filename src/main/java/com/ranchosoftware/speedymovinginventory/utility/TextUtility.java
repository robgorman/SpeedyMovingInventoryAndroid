package com.ranchosoftware.speedymovinginventory.utility;

/**
 * Created by rob on 8/11/16.
 */

public class TextUtility {

  public static String synthesizeAddress(String address, String addressLine2, String city, String state, String zip){
    String result = address;
    if (addressLine2 != null && addressLine2.length() > 0){
      result = result + "\n" + addressLine2;
    }
    result = result + "\n" + city + ", " + state + " " + zip;
    return result;
  }
}
