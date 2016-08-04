package com.ranchosoftware.speedymovinginventory.model;

import com.ranchosoftware.speedymovinginventory.BuildConfig;

import org.joda.time.DateTime;

/**
 * Created by rob on 7/13/16.
 */

public class Job {

  public enum Lifecycle {New, LoadedForStorage, InStorage, LoadedForDelivery, Delivered}
  private String jobNumber;
  private String companyKey;
  private Long createDateTime;
  private String customerEmail;
  private String customerFirstName;
  private String customerLastName;
  private String customerPhone;
  private Long deliveryEarliestDate;
  private Long deliveryLatestDate;
  private Address destinationAddress;
  private String lifecycle;
  private Address originAddress;
  private Long pickupDateTime;
  private String storageInTransit;

  private Signature signatureNew; // can be null
  private Signature signatureLoadedForStorage; // can be null
  private Signature signatureInStorage; // can be null
  private Signature signatureLoadedForDelivery; // can be null
  private Signature signatureDelivered; // can be null


  // no-args constructor require for firebase
  public Job(){

  }
  public Job(
          String jobNumber,
          String companyKey,
          Long createDateTime,
          String customerEmail,
          String customerFirstName,
          String customerLastName,
          String customerPhone,
          Long deliveryEarliestDate,
          Long deliveryLatestDate,
          Address destinationAddress,
          String lifecycle,
          Address originAddress,
          Long pickupDateTime,
          String storageInTransit
  ){
    this.jobNumber = jobNumber;
    this.companyKey = companyKey;
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
    this.signatureNew = null;
    this.signatureLoadedForStorage = null;
    this.signatureInStorage = null;
    this.signatureLoadedForDelivery = null;
    this.signatureDelivered = null;

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

  public String getStorageInTransit() {
    return storageInTransit;
  }

  public Signature getSignatureNew() {
    return signatureNew;
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
}
