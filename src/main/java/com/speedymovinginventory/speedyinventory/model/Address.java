package com.speedymovinginventory.speedyinventory.model;

/**
 * Created by rob on 7/13/16.
 */

public class Address extends  Model {
  private String street;
  private String addressLine2; // may be null
  private String city;
  private String state;
  private String zip;

  // no-args constructor require for firebase
  public Address(){

  }

  public Address(String street, String addressLine2, String city, String state, String zip){
    this.street = street;
    this.addressLine2 = addressLine2;
    this.city = city;
    this.state = state;
    this.zip = zip;
  }

  public String getStreet() {
    return street;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getZip() {
    return zip;
  }
}
