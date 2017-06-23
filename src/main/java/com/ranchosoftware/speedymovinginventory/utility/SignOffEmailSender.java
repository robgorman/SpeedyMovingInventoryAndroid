package com.ranchosoftware.speedymovinginventory.utility;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneNumberUtils;

import com.ranchosoftware.speedymovinginventory.R;
import com.ranchosoftware.speedymovinginventory.firebase.FirebaseServer;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.server.Server;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import static com.ranchosoftware.speedymovinginventory.model.Job.Lifecycle.Delivered;

/**
 * Created by rob on 5/12/17.
 */

public class SignOffEmailSender {

  public interface SenderListener {
    void success();

    void failure(String message);
  }

  private final Context context;
  private final Company company;
  private final String jobKey;

  private final String recipientList;
  private final String linkUrl;
  private final Server mailServer;
  private final FirebaseServer firebaseServer;
  private final String companyKey;
  private Job job;

  private SenderListener listener = null;

  public SignOffEmailSender(Context context, Server mailServer,
                            FirebaseServer firebaseServer, String linkUrl,
                            String recipientList, Company company,
                            String companyKey, String jobKey) {
    this.context = context;
    this.company = company;
    this.jobKey = jobKey;
    this.recipientList = recipientList;
    this.linkUrl = linkUrl;
    this.mailServer = mailServer;
    this.firebaseServer = firebaseServer;
    this.companyKey = companyKey;
  }

  private void sendSignOffEmails() {
    // we have to retrieve the job because it was just updated.
    firebaseServer.getJob(companyKey, jobKey, new FirebaseServer.GetJobSuccess() {
              @Override
              public void success(Job job) {

                SignOffEmailSender.this.job = job;
                sendUserEmail();
                sendCompanyEmail();
              }
            }, new FirebaseServer.Failure() {
              @Override
              public void error(String message) {
                // nothing to do
              }
            });
  }

  private String formSingleLineCompanyAddress(Company company) {
    String addressLine1 = company.getAddress().getStreet();
    String addressLine2 = "";
    String addressLine3 = "";
    if (company.getAddress().getAddressLine2().length() > 1) {
      addressLine2 = company.getAddress().getAddressLine2();
      addressLine3 = company.getAddress().getCity() + ", " + company.getAddress().getState() + " " + company.getAddress().getZip();
    } else {
      addressLine2 = company.getAddress().getCity() + ", " + company.getAddress().getState() + " " + company.getAddress().getZip();
      addressLine3 = "";
    }
    String address = addressLine1 + ", " + addressLine2;
    if (addressLine3.length() > 0) {
      address = address + ", " + addressLine3;
    }
    return address;
  }

  private String formMultiLineCompanyAddress(Company company) {
    String addressLine1 = company.getAddress().getStreet();
    String addressLine2 = "";
    String addressLine3 = "";
    if (company.getAddress().getAddressLine2().length() > 1) {
      addressLine2 = company.getAddress().getAddressLine2();
      addressLine3 = company.getAddress().getCity() + ", " + company.getAddress().getState() + " " + company.getAddress().getZip();
    } else {
      addressLine2 = company.getAddress().getCity() + ", " + company.getAddress().getState() + " " + company.getAddress().getZip();
      addressLine3 = "";
    }
    String multiLineAddress = addressLine1 + "\n" + addressLine2;
    if (addressLine3.length() > 0) {
      multiLineAddress = multiLineAddress + "\n" + addressLine3;
    }
    return multiLineAddress;
  }

  private String substitute(String template) {

    String customerName = job.getCustomerFirstName() + " " + job.getCustomerLastName();
    template = template.replaceAll("(?i)<<CustomerName>>", customerName);
    template = template.replaceAll("(?i)<<CompanyName>>", company.getName());
    template = template.replaceAll("(?i)<<CompanyMultiLineAddress>>", formMultiLineCompanyAddress(company));
    template = template.replaceAll("(?i)<<CompanySingleLineAddress>>", formSingleLineCompanyAddress(company));

    String companyPhone = null;
    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
      companyPhone = PhoneNumberUtils.formatNumber(company.getPhoneNumber(), "US");
    } else {
      companyPhone = PhoneNumberUtils.formatNumber(company.getPhoneNumber());
    }
    template = template.replaceAll("(?i)<<CompanyPhone>>", companyPhone);
    template = template.replaceAll("(?i)<<CompanyWebSite>>", company.getWebsite());

    String signupUrl = "";
    String authDomain = context.getString(R.string.auth_domain);
    try {

      signupUrl = "https://" + authDomain + "/user-sign-up"
              + "?companyname=" + URLEncoder.encode(company.getName(), "UTF-8")
              + "&customeremail=" + URLEncoder.encode(job.getCustomerEmail(), "UTF-8")
              + "&logourl=" + URLEncoder.encode(company.getLogoUrl(), "UTF-8")
              + "&customername=" + URLEncoder.encode(job.getCustomerFirstName() + " " + job.getCustomerLastName(), "UTF-8")
              + "&iscustomer=" + URLEncoder.encode("true", "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // not sure what we can do
    }

    signupUrl = "<a href=\"" + signupUrl + "\">Sign Up</a>";
    template = template.replaceAll("(?i)<<CustomerPortalSignupLink>>", signupUrl);

    String portalUrl =  "https://" + authDomain;
    portalUrl = "<a href=\"" + portalUrl + "\">Portal</a>";
    template = template.replaceAll("(?i)<<PortalLink>>", portalUrl);

    DateTimeFormatter fmt = DateTimeFormat.forPattern("MM/dd/yy, h:mm z");
    String moveDateTime = fmt.print(job.getPickupDateTime());
    template = template.replaceAll("(?i)<<MovePickupDateTime>>", moveDateTime);

    String companyLogo = "<img src=\"" + company.getLogoUrl() + "\">";
    template = template.replaceAll("(?i)<<CompanyLogo>>", companyLogo);

    template = template.replaceAll("(?i)<<JobStatus>>", job.getLifecycle().toString());

    template = template.replaceAll("(?i)<<JobNumber>>", job.getJobNumber());

    template = template.replaceAll("\\n", "<br>");

    return template;
  }

  private void sendCompanyEmail(){

    String messageBody = substitute(company.getTemplateEmailForEmployees());
    String subject = "Job Number: " + job.getJobNumber();
    mailServer.sendEmailMessage(recipientList, subject, messageBody, "noreply@speedymovinginventory.com", new Server.EmailCallback() {
      @Override
      public void success(String message) {
        // nothing to do
      }

      @Override
      public void failure(final String message) {
        // nothing to do for now
      }
    });

  }

  private void sendUserEmail() {
    Job.Lifecycle lifecycle = job.getLifecycle();
    String messageBody = "";
    String subject = "";
    switch (lifecycle) {
      case New:
        // this case shouldn't be possible
        break;
      case LoadedForStorage:
        if (company.getSendCustomerEmailAtJobPickup()) {
          messageBody = substitute(company.getTemplateEmailAtJobPickup());
          subject = "Job Pickup Complete!";
        }

        break;
      case InStorage:
      case LoadedForDelivery:
        if (company.getSendCustomerEmailEveryJobStatusChange()) {
          messageBody = substitute(company.getTemplateEmailEveryJobStatusChange());
          subject = "Job Status: " + job.getLifecycle().toString();
        }
        break;

      case Delivered:
        if (company.getSendCustomerEmailAtJobDelivery()) {
          messageBody = substitute(company.getTemplateEmailAtJobDelivery());
          subject = "Job Delivery Complete!";
          break;
        }
    }

    if (messageBody.length() > 0) {
      mailServer.sendEmailMessage(job.getCustomerEmail(), subject, messageBody, "noreply@speedymovinginventory.com", new Server.EmailCallback() {
        @Override
        public void success(String message) {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              listener.success();
            }
          });
        }

        @Override
        public void failure(final String message) {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              listener.failure(message);
            }
          });
        }
      });
    }
  }

  public void send(SenderListener listener) {
    this.listener = listener;
    sendSignOffEmails();

  }

}
