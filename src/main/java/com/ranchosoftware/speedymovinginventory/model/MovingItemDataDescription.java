package com.ranchosoftware.speedymovinginventory.model;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by rob on 11/3/16.
 */

public class MovingItemDataDescription extends Model {
  public  enum Room {Basement, Bedroom,
    Garage, DiningRoom, Den, Office, LivingRoom, Kitchen, Bathroom, Patio,
    Sunroom, Laundry, Nursery, Other}


  public enum BoxSize {Small, Medium, Large, XLarge, Wardrobe}

  private String room;
  private String itemName;
  private Float weightLbs;
  private Float cubicFeet;
  private Boolean isBox;
  private String boxSize;
  private String specialInstructions;


  // no-args constructor require for firebase
  public MovingItemDataDescription(){


  }

  public MovingItemDataDescription(
          String room,
          String itemName,
          Float weightLbs,
          Float cubicFeet,
          Boolean isBox,
          String boxSize,
          String specialInstructions){
    this.room = room;
    this.itemName = itemName;
    this.weightLbs = weightLbs;
    this.cubicFeet = cubicFeet;
    this.isBox = isBox;
    this.boxSize = boxSize;
    this.specialInstructions = specialInstructions;

  }

  public String getBoxSize() {
    return boxSize;
  }

  public Float getCubicFeet() {
    return cubicFeet;
  }

  public Boolean getIsBox() {
    return isBox;
  }

  public String getItemName() {
    return itemName;
  }

  public String getRoom() {
    return room;
  }

  public String getSpecialInstructions() {
    return specialInstructions;
  }

  public Float getWeightLbs() {
    return weightLbs;
  }

  public Room getRoomAsEnum(){
    Room room = Room.valueOf(this.room);
    return room;
  }
}
