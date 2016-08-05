package com.ranchosoftware.speedymovinginventory.model;

import com.google.firebase.database.Exclude;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by rob on 7/15/16.
 */

public class Item {

  public static class Defaults{
    public static Integer monetaryValue(){return 20;}
    public static Integer numberOfPads(){return 0;}
    public static Integer weightLbs(){return 5;}
    public static Integer volume(){return 1;}
    public static String packedBy(){ return "Owner";}
  }

  public enum Category {Bedroom1, Bedroom2, Bedroom3, Bedroom4, Bedroom5,
                        Garage, DiningRoom, Den, LivingRoom, Kitchen, Bathroom, Patio,
                        Yard, BonusRoom, Other}
  public enum PackedBy{Owner, Mover, ThirdParty}

  public enum Insurance { Released, Company, ThirdParty};

  // note xxxInverse fields are just for sorting in reverse order in the
  // web api. We can reverse sort in java using recylerview.

  private String category;
  private String claimNumber;
  private String damageDescription;
  private String description;
  private Boolean hasClaim;
  private Boolean hasClaimInverse;
  private Map<String, String> imageReferences;  // first string is timestamp second url
  private String insurance;
  private Boolean isBox;
  private Boolean isClaimActive;
  private Boolean isClaimActiveInverse;
  private Boolean isScanned;
  private Boolean isScannedInverse;
  private String jobKey;
  private Integer monetaryValue;
  private Integer monetaryValueInverse;
  private Integer numberOfPads;
  private Integer numberOfPadsInverse;
  private String packedBy;
  private String specialHandling;
  private String uidOfCreator;
  private Integer volume;
  private Integer volumeInverse;
  private Integer weightLbs;
  private Integer weightLbsInverse;




  public Item(){
    this.imageReferences = new TreeMap<>();
  }

  public Item(Category category, Integer numberOfPads,
              String uidOfCreator,
              String description,
              Integer monetaryValue,
              Integer weightLbs,
              Integer volume,
              String specialHandling,
              String jobKey,
              String packedBy,
              String insurance,
              Boolean isBox){
    this.category = category.toString();
    this.numberOfPads = numberOfPads;
    this.numberOfPadsInverse = -numberOfPads;
    this.uidOfCreator = uidOfCreator;

    this.description = description ;
    this.monetaryValue = monetaryValue;
    this.monetaryValueInverse = -this.monetaryValue;
    this.weightLbs = weightLbs;
    this.weightLbsInverse = -this.weightLbs;
    this.volume = volume;
    this.volumeInverse = - this.volume;
    this.specialHandling = specialHandling;
    this.jobKey = jobKey;
    this.packedBy = packedBy;
    this.imageReferences = new TreeMap<>();
    this.claimNumber = "";
    this.hasClaim = false;
    this.hasClaimInverse = !this.hasClaim;
    this.isClaimActive = false;
    this.isClaimActiveInverse = !this.isClaimActive;
    this.insurance = insurance;
    this.isBox = isBox ;
    this.isScanned = false;
    this.isScannedInverse = !this.isScanned;
    this.damageDescription = "";
  }


  public String getSpecialHandling() {

    return specialHandling;
  }
  public String getCategory(){
    return category;
  }
  @Exclude
  public Category getCategoryEnum() {
    Category c = Category.valueOf(category);

    if (c == null){
      return Category.Other;
    }
    return c;
  }

  public String getPackedBy(){
    return packedBy;
  }

  @Exclude
  public PackedBy getPackedByEnum(){
    PackedBy by = PackedBy.valueOf(packedBy);
    if (by == null){
      return PackedBy.Mover;
    }
    return by;
  }

  @Exclude
  public Insurance getInsuranceEnum(){
    Insurance ins = Insurance.valueOf(insurance);
    if (ins == null){
      return Insurance.Released;
    }
    return ins;
  }

  public Integer getNumberOfPads() {
    return numberOfPads;
  }

  public String getUidOfCreator() {
    return uidOfCreator;
  }

  public String getDescription() {
    return description;
  }

  public Integer getMonetaryValue() {
    return monetaryValue;
  }

  public Integer getWeightLbs() {
    return weightLbs;
  }

  public Integer getVolume() {
    return volume;
  }


  public String getJobKey() {return jobKey;}

  public Map<String, String> getImageReferences() {
    return imageReferences;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setPackedBy(String packedBy) {
    this.packedBy = packedBy;
  }

  public void setNumberOfPads(Integer numberOfPads) {
    this.numberOfPads = numberOfPads;
    this.numberOfPadsInverse = -this.numberOfPads;
  }

  public void setUidOfCreator(String uidOfCreator) {
    this.uidOfCreator = uidOfCreator;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setMonetaryValue(Integer monetaryValue) {

    this.monetaryValue = monetaryValue;
    this.monetaryValueInverse = -this.monetaryValue;
  }

  public void setWeightLbs(Integer weightLbs) {
    this.weightLbs = weightLbs;
    this.weightLbsInverse = -this.weightLbs;
  }

  public void setVolume(Integer volume) {
    this.volume = volume;
    this.volumeInverse = -this.volume;

  }

  public void setSpecialHandling(String specialHandling) {
    this.specialHandling = specialHandling;
  }

  public void setJobKey(String jobKey) {
    this.jobKey = jobKey;
  }

  public void setImageReferences(Map<String, String> imageReferences) {
    this.imageReferences = imageReferences;
  }

  public String getClaimNumber() {
    return claimNumber;
  }

  public void setClaimNumber(String claimNumber) {
    this.claimNumber = claimNumber;
  }

  public Boolean getIsClaimActive() {
    return isClaimActive;

  }

  public void setIsClaimActive(Boolean claimActive) {
    isClaimActive = claimActive;
    this.isClaimActiveInverse = !this.isClaimActive;
  }

  public Boolean getHasClaim() {
    return hasClaim;
  }

  public void setHasClaim(Boolean hasClaim) {
    this.hasClaim = hasClaim;
    this.hasClaimInverse = !this.hasClaim;
  }

  public String getInsurance() {
    return insurance;
  }

  public void setInsurance(String insurance) {
    this.insurance = insurance;
  }

  public void setIsBox(Boolean isBox){this.isBox = isBox;}

  public  Boolean getIsBox(){ return this.isBox;}

  public Boolean getIsScanned() {
    return isScanned;
  }

  public void setIsScanned(Boolean scanned) {
    this.isScanned = scanned;
    this.isScannedInverse = !this.isScanned;
  }

  public String getDamageDescription() {
    return damageDescription;
  }

  public void setDamageDescription(String damageDescription) {
    this.damageDescription = damageDescription;
  }

  public Boolean getHasClaimInverse() {
    return hasClaimInverse;
  }

  public Boolean getClaimActiveInverse() {
    return isClaimActiveInverse;
  }

  public Integer getMonetaryValueInverse() {
    return monetaryValueInverse;
  }

  public Integer getNumberOfPadsInverse() {
    return numberOfPadsInverse;
  }

  public Integer getVolumeInverse() {
    return volumeInverse;
  }

  public Integer getWeightLbsInverse() {
    return weightLbsInverse;
  }
}

