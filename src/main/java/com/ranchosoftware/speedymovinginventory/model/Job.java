package com.ranchosoftware.speedymovinginventory.model;

import com.ranchosoftware.speedymovinginventory.BuildConfig;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;


public class Job extends Model {

  public enum Lifecycle {New, LoadedForStorage, InStorage, LoadedForDelivery, Delivered}

  private String companyKey;
  private String jobCompanyName;
  private Long createDateTime;
  private String customerEmail;
  private String customerFirstName;
  private String customerLastName;
  private String customerPhone;
  private Long deliveryEarliestDate;
  private String deliveryInstructions;
  private Long deliveryLatestDate;
  private Address destinationAddress;
  private String jobNumber;
  private String lifecycle;
  private Address originAddress;
  private Long pickupDateTime;
  private String pickupInstructions;
  private Signature signatureDelivered; // can be null
  private Signature signatureInStorage; // can be null
  private Signature signatureLoadedForDelivery; // can be null
  private Signature signatureLoadedForStorage; // can be null
  private Boolean storageInTransit;
  private Map<String, UserIdMapEntry> users; // can be null
  private Boolean isCancelled;

  // no-args constructor require for firebase
  public Job(){

  }
  public Job(
          String jobNumber,
          String companyKey,
          String jobCompanyName,
          Long createDateTime,
          String customerEmail,
          String customerFirstName,
          String customerLastName,
          String companyName,
          String customerPhone,
          Long deliveryEarliestDate,
          Long deliveryLatestDate,
          Address destinationAddress,
          String lifecycle,
          Address originAddress,
          Long pickupDateTime,
          Boolean storageInTransit,
          Boolean isCancelled
  ){
    this.jobNumber = jobNumber;
    this.companyKey = companyKey;
    this.jobCompanyName = jobCompanyName;
    this.createDateTime  = createDateTime;
    this.customerEmail  = customerEmail;
    this.customerFirstName  = customerFirstName;
    this.customerLastName  = customerLastName;
    this.customerPhone  = customerPhone;
    this.deliveryEarliestDate  = deliveryEarliestDate;
    this.deliveryLatestDate  = deliveryLatestDate;
    this.destinationAddress  = destinationAddress;
    this.lifecycle  = lifecycle;
    this.originAddress  = originAddress;
    this.pickupDateTime  = pickupDateTime;
    this.storageInTransit = storageInTransit;
    this.signatureLoadedForStorage = null;
    this.signatureInStorage = null;
    this.signatureLoadedForDelivery = null;
    this.signatureDelivered = null;
    this.users = new HashMap<>();
    this.deliveryInstructions = "";
    this.pickupInstructions = "";
    this.isCancelled = isCancelled;

  }

  public String getCompanyKey() {
    return companyKey;
  }

  public DateTime getCreateDateTime() {

    return new DateTime( createDateTime);
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public String getCustomerFirstName() {
    return customerFirstName;
  }

  public String getCustomerLastName() {
    return customerLastName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public DateTime getDeliveryEarliestDate() {
    return new DateTime(deliveryEarliestDate);
  }

  public DateTime getDeliveryLatestDate() {
    return new DateTime(deliveryLatestDate);
  }

  public Address getDestinationAddress() {
    return destinationAddress;
  }

  public Lifecycle getLifecycle() {

    Lifecycle l = Lifecycle.valueOf(lifecycle);
    if (BuildConfig.DEBUG) {if (l == null) throw new AssertionError("assert failed");}
    return l;
  }

  public Address getOriginAddress() {
    return originAddress;
  }

  public DateTime getPickupDateTime() {
    return  new DateTime(pickupDateTime);
  }

  public Boolean getStorageInTransit() {
    return storageInTransit;
  }

  public Signature getSignatureLoadedForStorage() {
    return signatureLoadedForStorage;
  }

  public Signature getSignatureInStorage() {
    return signatureInStorage;
  }

  public Signature getSignatureLoadedForDelivery() {
    return signatureLoadedForDelivery;
  }

  public Signature getSignatureDelivered() {
    return signatureDelivered;
  }

  public String getJobNumber() {
    return jobNumber;
  }

  public Map<String, UserIdMapEntry> getUsers() {
    if (users == null){
      users = new HashMap<>();
    }
    return users;
  }

  public String getDeliveryInstructions() {
    if (deliveryInstructions == null){
      deliveryInstructions = "";
    }
    return deliveryInstructions;
  }

  public String getPickupInstructions() {
    if (pickupInstructions == null){
      pickupInstructions = "";
    }
    return pickupInstructions;
  }

  private String getJobCompanyName(){
    if (jobCompanyName == null){
      jobCompanyName = "";
    }
    return jobCompanyName;
  }

  public boolean getIsCancelled(){
    if (isCancelled == null){
      isCancelled = false;
    }
    return isCancelled;
  }
}
