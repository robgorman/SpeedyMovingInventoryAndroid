package com.ranchosoftware.speedymovinginventory.model;

/**
 * Created by rob on 8/11/16.
 */

public class Company extends Model{
  private Boolean active;
  private Address address;
  private String calT;
  private String contact;
  private Long dateCreated;
  private Long dateDeactivated;
  private String iccMc;
  // skip the jobkeys
  private String logoUrl;
  private String name;
  private String phoneNumber;
  private String poundsPerCubicFoot;
  private String usDot;
  private String website;

  // required no args ctor
  public Company(){}

  public Boolean getActive() {
    return active;
  }

  public Address getAddress() {
    return address;
  }

  public String getCalT() {
    return calT;
  }

  public Long getDateCreated() {
    return dateCreated;
  }

  public Long getDateDeactivated() {
    return dateDeactivated;
  }

  public String getIccMc() {
    return iccMc;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public String getName() {
    return name;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getUsDot() {
    return usDot;
  }

  public String getWebsite() {
    return website;
  }

  public String getPoundsPerCubicFoot(){
    return poundsPerCubicFoot;
  }

  public String getContact(){
    if (contact == null){
      return "";
    }
    return contact;
  }
}
