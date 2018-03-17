package com.speedymovinginventory.speedyinventory.utility;

import com.speedymovinginventory.speedyinventory.model.Address;

/**
 * Created by rob on 8/11/16.
 */

public class TextUtility {

  public static boolean isBlank(String s){
    if (s == null){
      return true;
    }

    String trimmed = s.trim();
    return trimmed.length() == 0;

  }

  public static String formMultiLineAddress(String address, String addressLine2, String city, String state, String zip){
    String result = address;
    if (!isBlank(addressLine2)){
      result = result + "\n" + addressLine2;
    }
    result = result + "\n" + city + ", " + state + " " + zip;
    return result;
  }

  public static String formSingleLineAddress(Address address){
    String line2 = ", ";
    if (!isBlank(address.getAddressLine2())){
      line2 = " " + address.getAddressLine2() + ", ";
    }
    String result = address.getStreet() + line2 + address.getCity() + ", " + address.getState() + " " + address.getZip();
    return result;
  }
}
