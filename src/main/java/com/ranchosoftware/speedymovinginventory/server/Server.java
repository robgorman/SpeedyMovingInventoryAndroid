package com.ranchosoftware.speedymovinginventory.server;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.gson.Gson;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rob on 9/6/16.
 */

public class Server {


  private static String TAG = Server.class.getSimpleName();

  public String serverUrl;
  public Server (String baseUrl){
    serverUrl = baseUrl;
  }


  public static interface StandardCallback{
    public void success();
    public void failure(String message);
  }


  public static interface EmailCallback {
    public void success(String message);

    public void failure(String message);
  }


  private String makeUrlParams(final Map<String, String> params){
    StringBuilder builder = new StringBuilder();
    builder.append("?");
    int paramNumber = 0;
    for (String key : params.keySet()){
      String value = params.get(key);
      if (paramNumber > 0){
        builder.append("&");
      }
      try {
        builder.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
      } catch (Exception e) {
        e.printStackTrace();
      }
      paramNumber++;
    }
    return builder.toString();
  }
  public void sendNewSignoffEmailMessage(final String recipients,
                                         final String companyName,
                                         final String linkUrl,
                                         final String customername,
                                         final String lifecycle,
                                         final String jobNumber,
                                         final String companyPhone,
                                         final EmailCallback callback)
  {
    Map<String, String> params = new HashMap<String, String>();
    params.put("recipients", recipients);
    params.put("companyname", companyName);
    params.put("linkurl", linkUrl);
    params.put("customername", customername);
    params.put("lifecycle", lifecycle);
    params.put("jobnumber", jobNumber);
    params.put("companyphone", companyPhone);

    sendNewSignoffEmailMessageRequest(params, callback);
  }


  private void sendNewSignoffEmailMessageRequest(final Map<String, String> params, final EmailCallback callback){

    RequestQueue queue = MyVolley.getRequestQueue();
    String url = serverUrl+ "/sendsignoffemail" + makeUrlParams(params);
    StringRequest request = new StringRequest(Request.Method.GET,url,
            new Response.Listener<String>(){
              @Override
              public void onResponse(String s) {

                Gson gson = new Gson();

                ServletResponse response = gson.fromJson(s, ServletResponse.class);
                if (response.isSuccess()){
                  callback.success(response.getErrorMessage());
                } else {
                  callback.failure(response.getErrorMessage());
                }

              }
            },
            new Response.ErrorListener(){
              @Override
              public void onErrorResponse(VolleyError volleyError) {
                // turn the error to string
                callback.failure(volleyError.getMessage());
              }
            }){

      protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {

        return params;
      };
    };
    queue.add(request);
  }


  public void sendEmailMessage(final String recipients,
                                         final String subject,
                                         final String messageBody,
                                         final String fromEmailAddress,
                                         final EmailCallback callback)
  {
    Map<String, String> params = new HashMap<String, String>();
    params.put("recipients", recipients);
    params.put("subject", subject);
    params.put("body", messageBody);
    params.put("fromemailaddress", fromEmailAddress);


    sendEmailMessage(params, callback);
  }

  private void sendEmailMessage(final Map<String, String> params, final EmailCallback callback){

    RequestQueue queue = MyVolley.getRequestQueue();
    String url = serverUrl+ "/sendemmail" + makeUrlParams(params);
    StringRequest request = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>(){
              @Override
              public void onResponse(String s) {

                Gson gson = new Gson();

                ServletResponse response = gson.fromJson(s, ServletResponse.class);
                if (response.isSuccess()){
                  callback.success(response.getErrorMessage());
                } else {
                  callback.failure(response.getErrorMessage());
                }

              }
            },
            new Response.ErrorListener(){
              @Override
              public void onErrorResponse(VolleyError volleyError) {
                // turn the error to string
                callback.failure(volleyError.getMessage());
              }
            }){

      protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {

        return params;
      };
    };
    queue.add(request);
  }

}
