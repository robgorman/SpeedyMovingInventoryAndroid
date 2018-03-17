package com.speedymovinginventory.speedyinventory.model;

/**
 * Created by rob on 7/14/16.
 */

public class User extends Model {


  public enum Role {CompanyAdmin, Foreman, AgentForeman, CrewMember, AgentCrewMember, Customer}
  public static String[] roleLabels= {"Company Admin", "Foreman", "Agent Foreman", "Crew Member", "Agent Crew Member", "Customer"};


  //@Deprecated
  //private String companyKey;
  private String firstName;

  //@Deprecated
  //private Boolean isDisabled;
  private String lastName;

  //@Deprecated
  //private String role;
  private String uid;
  private String emailAddress;

  //@Deprecated
  //private String customerJobKey;  // this is null unless the user's role is customer; if the
                                // role is customer this is the job they are limited to.


  //private Map<String, UserCompanyAssignment> companies;

  public User(String firstName, String lastName, String uid, String emailAddress){
    this.firstName = firstName;
    this.lastName = lastName;
    this.uid = uid;

    this.emailAddress = emailAddress;
  }

  // no args ctor required for firebase
  public User(){

  }


  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  //@Deprecated
  //@Exclude
  //public Role getRoleAsEnum(){
  //  for (int i = 0; i < roleLabels.length; i++){
  //    if (role.equals(Role.values()[i].toString())){
  //      return Role.values()[i];
  //    }
 //   }
  //  return Role.CrewMember;
 // }

  public String getUid() {
    return uid;
  }



  public String getEmailAddress() {
    return emailAddress;
  }



}
