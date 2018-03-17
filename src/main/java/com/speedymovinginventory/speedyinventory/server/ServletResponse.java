package com.speedymovinginventory.speedyinventory.server;

/**
 * Created by rob on 9/6/16.
 */


public class ServletResponse
{
  private boolean success;
  private String  errorMessage;

  public ServletResponse(boolean success)
  {
    this.success = success;
    this.errorMessage = "";
  }

  public ServletResponse(boolean success, String message)
  {
    this.success = success;
    this.errorMessage = message;
  }


  public ServletResponse(String message)
  {
    this.success = false;
    this.errorMessage = message;
  }

  public void setErrorMessage(String message)
  {
    this.success = false;
    this.errorMessage = message;
  }

  public boolean isSuccess()
  {
    return success;
  }

  public String getErrorMessage()
  {
    return errorMessage;
  }

}
