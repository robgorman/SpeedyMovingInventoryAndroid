package com.ranchosoftware.speedymovinginventory.app;

/**
 * Created by rob on 9/6/16.
 */

public class Configuration {

  public enum Environment {Production, Development, EmailServerDevelopmentOnly, FirebaseServerDevelopmentOnly}

  ////////////////////////////////////////////////////////////////
  /////////////////////////////////////// Configure Here!!!
  public static Environment environment = Environment.Production;
  private static Configuration instance;
  public static Configuration getInstance(){
    if (instance == null){
      instance = new Configuration();
    }
    return instance;
  }

  private String mailServerHost;
  private String firebaseHost;
  private String inviteSignupUrl;

  private Configuration(){


    if (environment == Environment.Production){
      firebaseHost =  "https://speedymovinginventory.firebaseapp.com";
      mailServerHost = "https://speedymovinginventory.appspot.com";
      inviteSignupUrl = firebaseHost + "/user-signup";
    } else if (environment == Environment.Development){
      firebaseHost = "http://localhost:4200";
      mailServerHost = "http://localhost:8080";
      inviteSignupUrl = firebaseHost + "/user-signup";
    } else if (environment == Environment.EmailServerDevelopmentOnly){
      firebaseHost =  "https://speedymovinginventory.firebaseapp.com";
      mailServerHost = "http://localhost:8080";
      inviteSignupUrl = firebaseHost + "/user-signup";

    } else if (environment == Environment.FirebaseServerDevelopmentOnly){
      firebaseHost = "http://localhost:4200";
      mailServerHost = "https://speedymovinginventory.appspot.com";
      inviteSignupUrl = firebaseHost + "/user-signup";
    }
  }

  public String getInviteSignupUrl() {
    return inviteSignupUrl;
  }

  public String getMailServerHost() {
    return mailServerHost;
  }

  public String getFirebaseHost() {
    return firebaseHost;
  }
}
