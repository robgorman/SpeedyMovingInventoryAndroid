package com.speedymovinginventory.speedyinventory.model;

import org.joda.time.DateTime;

/**
 * Created by rob on 2/1/17.
 */

public class ScanRecord extends Model{
  private Long scanDateTime;
  private Double latitude;
  private Double longitude;
  private String uidOfScanner;
  private Boolean isScanOverride;
  private String lifecycle;

  public ScanRecord(DateTime dateTime, Double latitude, Double longitude, String userUid, Boolean isScanOverride, Job.Lifecycle lifecycle){
    this.scanDateTime = dateTime.getMillis();
    this.latitude = latitude;
    this.longitude = longitude;
    this.uidOfScanner = userUid;
    this.isScanOverride = isScanOverride;
    this.lifecycle = lifecycle.toString();

  }

  public Boolean getIsScanOverride() {
    return isScanOverride;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public Long getScanDateTime() {
    return scanDateTime;
  }

  public String getUidOfScanner() {
    return uidOfScanner;
  }

  public String getLifecycle() {return lifecycle;}
}
