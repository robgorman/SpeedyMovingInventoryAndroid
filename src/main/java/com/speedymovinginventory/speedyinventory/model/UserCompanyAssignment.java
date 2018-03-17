package com.speedymovinginventory.speedyinventory.model;

import com.google.firebase.database.Exclude;

/**
 * Created by rob on 12/31/16.
 */

public class UserCompanyAssignment {


  private String uid;
  private String companyKey;  // this user can access this company
  // this might be null if the user doesn't yet have a company
  private String role;        // the users role for the company
  private Boolean isDisabled; // is user still active for the companhy

  // this is null unless a users role is customer
  private String customerJobKey;

  // no args constructor required for FirebaseUI
  public UserCompanyAssignment(){

  }

  public UserCompanyAssignment(String uid, String companyKey, String role, Boolean isDisabled, String customerJobKey) {
    this.companyKey = companyKey;
    this.role = role;
    this.isDisabled = isDisabled;
    this.customerJobKey = customerJobKey;
    this.uid = uid;
  }

  public String getCompanyKey() {
    return companyKey;
  }

  public String getCustomerJobKey() {
    return customerJobKey;
  }

  public Boolean getIsDisabled() {
    return isDisabled;
  }

  public String getRole() {
    return role;
  }

  @Exclude
  public User.Role getRoleAsEnum() {
    for (int i = 0; i < User.roleLabels.length; i++) {
      if (role.equals(User.Role.values()[i].toString())) {
        return User.Role.values()[i];
      }
    }
    return User.Role.CrewMember;

  }

  public String getUid() {
    return uid;
  }
}