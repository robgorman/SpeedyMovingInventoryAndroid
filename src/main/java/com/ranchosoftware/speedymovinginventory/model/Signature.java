package com.ranchosoftware.speedymovinginventory.model;

import org.joda.time.DateTime;

/**
 * Created by rob on 8/1/16.
 */

public class Signature extends Model {
  private String name;
  private String imageUrl;
  private Long signOffDateTime;

  // necessary for firebase
  public Signature(){

  }

  public Signature(String name, String imageUrl, Long signOffDateTime){
    this.name = name;
    this.imageUrl = imageUrl;
    this.signOffDateTime = signOffDateTime;
  }

  public String getName() {
    return name;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  //
  public Long getSignOffDateTime(){
    if (signOffDateTime == null){
      // return the zero time
      return new DateTime(0).getMillis();

    }
    return signOffDateTime;
  }
}
