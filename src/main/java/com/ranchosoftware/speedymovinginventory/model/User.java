package com.ranchosoftware.speedymovinginventory.model;

import com.google.firebase.database.Exclude;

/**
 * Created by rob on 7/14/16.
 */

public class User extends Model {


  public enum Role {ServiceAdmin, CompanyAdmin, Foreman, AgentForeman, CrewMember, AgentCrewMember, Customer}
  public static String[] roleLabels= {"Service Admin", "Company Admin", "Foreman", "Agent Foreman", "Crew Member", "Agent Crew Member", "Customer"};


  private String companyKey;
  private String firstName;
  private Boolean isDisabled;
  private String lastName;
  private String role;
  private String uid;
  private String emailAddress;
  private String customerJobKey;  // this is null unless the user's role is customer; if the
                                // role is customer this is the job they are limited to.

  public User(String companyKey, String firstName, String lastName, String role, String uid, String emailAddress, String customerJobKey){
    this.companyKey = companyKey;
    this.firstName = firstName;
    this.lastName = lastName;
    this.role = role;
    this.uid = uid;
    this.isDisabled = false;

    this.emailAddress = emailAddress;
    this.customerJobKey = customerJobKey;
  }

  // no args ctor required for firebase
  public User(){

    this.isDisabled = false;
  }

  public String getCompanyKey() {
    return companyKey;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getRole() {
    return role;
  }

  @Exclude
  public Role getRoleAsEnum(){
    for (int i = 0; i < roleLabels.length; i++){
      if (role.equals(Role.values()[i].toString())){
        return Role.values()[i];
      }
    }
    return Role.CrewMember;
  }

  public String getUid() {
    return uid;
  }

  public Boolean getDisabled() {
    return isDisabled;
  }

  public void setDisabled(Boolean disabled) {
    isDisabled = disabled;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getCustomerJobKey() {return customerJobKey;}
}
