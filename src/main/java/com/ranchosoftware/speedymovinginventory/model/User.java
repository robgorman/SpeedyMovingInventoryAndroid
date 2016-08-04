package com.ranchosoftware.speedymovinginventory.model;

import com.google.firebase.database.Exclude;

/**
 * Created by rob on 7/14/16.
 */

public class User {


  public enum Role {ServiceAdmin, CompanyAdmin, Foreman, AgentForeman, CrewMember, AgentCrewMember, Customer}
  public static String[] roleLabels= {"Service Admin", "Company Admin", "Foreman", "Agent Foreman", "Crew Member", "Agent Crew Member", "Customer"};


  private String companyKey;
  private String firstName;
  private String lastName;
  private String role;
  private String uid;

  public User(String companyKey, String firstName, String lastName, String role, String uid){
    this.companyKey = companyKey;
    this.firstName = firstName;
    this.lastName = lastName;
    this.role = role;
    this.uid = uid;
  }

  // no args ctor required for firebase
  public User(){

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
}
