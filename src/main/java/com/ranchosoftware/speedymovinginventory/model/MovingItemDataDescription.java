package com.ranchosoftware.speedymovinginventory.model;

import com.google.firebase.database.Exclude;


public class MovingItemDataDescription extends Model {
  public  enum Room {Basement, Bedroom,
    Garage, DiningRoom, Den, Office, LivingRoom, Kitchen, Bathroom, Patio,
    Sunroom, Laundry, Nursery, Other}


  public enum BoxSize {Small, Medium, Large, XLarge, Wardrobe}

  private String room;
  private String itemName;
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
          Float cubicFeet,
          Boolean isBox,
          String boxSize,
          String specialInstructions){
    this.room = room;
    this.itemName = itemName;
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

  @Exclude
  public Room getRoomAsEnum(){
    Room room = Room.valueOf(this.room);
    return room;
  }

  @Exclude
  public void setRoomAsEnum(Room room){
    this.room = room.toString();
  }
}
