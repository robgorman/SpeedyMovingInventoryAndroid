package com.ranchosoftware.speedymovinginventory.model;

/**
 * Created by rob on 8/11/16.
 */

public class Company extends Model{
  private Boolean active;
  private Address address;
  private String calT;
  private String contactPerson;
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

  ///////////////////////////////////////////////////////////////
  // configurable UI Options
  // Default is false. When true the number of pads becomes a sortable
  // field in teh mobile apps. And on the printed inventory the
  // number of pads becomes a column on the inventory sheet.
  private Boolean showNumberOfPadsOnItems;


  // Default is true. When false, the basic idea is that
  // item values and summaries of value
  // are not shown on customer interfaces. These
  // interfaces include all sign off sheets, as well as
  // the web interface if a customer logs in.
  private Boolean exposeValueToCustomers;

  // Default is true. When false, the basic idea is that
  // item volume and volume summaries,
  // are not shown on customer interfaces. These
  // interfaces include all sign off sheets, as well as
  // the web interface if a customer logs in.
  private Boolean exposeVolumeToCustomers;

  private Boolean sendCustomerEmailAtJobCreation;   // defaults to false
  private String templateEmailAtJobCreation;

  private Boolean sendCustomerEmailAtJobPickup;     // defaults to true
  private String templateEmailAtJobPickup;

  private Boolean sendCustomerEmailAtJobDelivery;    // defaults to true
  private String templateEmailAtJobDelivery;

  private Boolean sendCustomerEmailEveryJobStatusChange; // defaults to false
  private String templateEmailEveryJobStatusChange;

  private String templateEmailForEmployees;

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

  public String getContactPerson(){
    if (contactPerson == null){
      return "";
    }
    return contactPerson;
  }

  public Boolean getShowNumberOfPadsOnItems(){
    if (showNumberOfPadsOnItems == null){
      return false;
    }
    return showNumberOfPadsOnItems;
  }

  public Boolean getExposeValueToCustomers(){
    if (exposeValueToCustomers == null){
      return true;
    }
    return exposeValueToCustomers;
  }

  public Boolean getExposeVolumeToCustomers(){
    if (exposeVolumeToCustomers == null){
      return true;
    }
    return exposeVolumeToCustomers;
  }

  public Boolean getSendCustomerEmailAtJobCreation() {
    return sendCustomerEmailAtJobCreation;
  }

  public String getTemplateEmailAtJobCreation() {
    if (templateEmailAtJobCreation == null || templateEmailAtJobCreation.length() == 0) {
      return "";
    }
    return templateEmailAtJobCreation;
  }

  public Boolean getSendCustomerEmailAtJobPickup() {
    return sendCustomerEmailAtJobPickup;
  }

  public String getTemplateEmailAtJobPickup() {
    if (templateEmailAtJobPickup == null || templateEmailAtJobPickup.length() == 0) {
      return "";
    }
    return templateEmailAtJobPickup;
  }

  public Boolean getSendCustomerEmailAtJobDelivery() {
    return sendCustomerEmailAtJobDelivery;
  }

  public String getTemplateEmailAtJobDelivery() {
    if (templateEmailAtJobDelivery == null || templateEmailAtJobDelivery.length() == 0) {
      return "";
    }
    return templateEmailAtJobDelivery;
  }

  public Boolean getSendCustomerEmailEveryJobStatusChange() {
    return sendCustomerEmailEveryJobStatusChange;
  }

  public String getTemplateEmailEveryJobStatusChange() {
    if (templateEmailEveryJobStatusChange == null || templateEmailEveryJobStatusChange.length() == 0) {
      return "";
    }
    return templateEmailEveryJobStatusChange;
  }

  public String getTemplateEmailForEmployees() {
    if (templateEmailForEmployees == null || templateEmailForEmployees.length() == 0) {
      return "";
    }
    return templateEmailForEmployees;
  }
}
