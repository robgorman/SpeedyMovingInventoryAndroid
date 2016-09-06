package com.ranchosoftware.speedymovinginventory.model;

/**
 * Created by rob on 8/1/16.
 */

public class Signature extends Model {
  private String name;
  private String imageUrl;

  // necessary for firebase
  public Signature(){

  }

  public Signature(String name, String imageUrl){
    this.name = name;
    this.imageUrl = imageUrl;
  }

  public String getName() {
    return name;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
