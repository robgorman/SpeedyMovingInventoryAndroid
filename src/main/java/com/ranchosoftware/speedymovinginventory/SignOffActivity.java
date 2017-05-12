package com.ranchosoftware.speedymovinginventory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Address;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.Signature;
import com.ranchosoftware.speedymovinginventory.utility.TextUtility;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.ByteArrayOutputStream;

public class SignOffActivity extends BaseActivity {

  private static final String TAG = SignOffActivity.class.getSimpleName();

  private SignaturePad signaturePad;
  private Button clearPad;
  private Button accept;
  private TextView signHere;
  private TextView tvName;
  private String companyKey;
  private String jobKey;
  private Job.Lifecycle entryLifecycle;
  private Boolean storageInTransit;
  private Toolbar toolbar;
  private TextView tvDate;
  private TextView tvForeman;


  private ProgressDialog progressDialog;

  private TextView tvCompanyName;
  private TextView tvCompanyAddress;
  private TextView tvCompanyPhone;
  private TextView tvShipperName;
  private TextView tvShipperPhone;
  private TextView tvShipperAddress;
  private TextView tvShipperEmail;
  private TextView tvMoveSummary;

  private ImageView ivNew;
  private ImageView ivLoadedForStorage;
  private ImageView ivInStorage;
  private ImageView ivLoadedForDelivery;
  private ImageView ivDelivered;

  private ImageView ivCompanyLogo;

  private int totalItems;
  private int totalValue;
  private int totalPads;
  private float totalVolumeCubicFeet;
  private float totalWeightLbs;
  private int totalDamagedItems;

  private ImageLoader imageLoader;
  private Company company;
  private Job job;

  public static Intent getLaunchIntent(Context context,
                                       String companyKey,
                                       String jobKey,
                                       String lifecycle,
                                       Boolean storageInTransit,
                                       int totalItems,
                                       int totalValue,
                                       int totalPads,
                                       Float totalVolumeCubicFeet,
                                       Float totalWeightLbs,
                                       int totalDamagedItems){
    Intent intent = new Intent(context, SignOffActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey",companyKey);
    params.putString("jobKey", jobKey);;
    params.putString("lifecycle", lifecycle);
    params.putBoolean("storageInTransit", storageInTransit);
    params.putInt("totalItems", totalItems);
    params.putInt("totalValue", totalValue );
    params.putInt("totalPads",  totalPads);
    params.putFloat("totalVolumeCubicFeet",  totalVolumeCubicFeet);
    params.putFloat("totalWeightLbs",  totalWeightLbs);
    params.putInt("totalDamagedItems",  totalDamagedItems);

    intent.putExtras(params);
    return intent;
  }

  private void updateLifecycle(Job job){
    if (job.getStorageInTransit()){
      ivLoadedForStorage.setVisibility(View.VISIBLE);
      ivInStorage.setVisibility(View.VISIBLE);

    } else {
      ivLoadedForStorage.setVisibility(View.GONE);
      ivInStorage.setVisibility(View.GONE);
    }

    switch (job.getLifecycle()){
      case New:
        ivNew.setImageDrawable(ResourcesCompat.getDrawable(this.getResources(), R.drawable.new_active, null));
        break;
      case LoadedForStorage:
        ivLoadedForStorage.setImageDrawable(ResourcesCompat.getDrawable(this.getResources(), R.drawable.loaded_for_storage_active, null));
        break;
      case InStorage:
        ivInStorage.setImageDrawable(ResourcesCompat.getDrawable(this.getResources(), R.drawable.in_storage_active, null));
        break;
      case LoadedForDelivery:
        ivLoadedForDelivery.setImageDrawable(ResourcesCompat.getDrawable(this.getResources(), R.drawable.loaded_for_delivery_active, null));
        break;
      case Delivered:
        ivDelivered.setImageDrawable(ResourcesCompat.getDrawable(this.getResources(), R.drawable.delivered_active, null));
        break;
    }
  }

  private void updateFromJob(){
    if (job == null)
      return; // this is an error

    tvShipperName.setText( job.getCustomerFirstName() + " " + job.getCustomerLastName());

    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
      tvShipperPhone.setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone(), "US" ));
    } else {
      tvShipperPhone.setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone()));
    }

    Address dest = job.getDestinationAddress();
    tvShipperAddress.setText(TextUtility.formSingleLineAddress(dest));

    tvShipperEmail.setText(job.getCustomerEmail());

    toolbar.setTitle("Job Number: " + job.getJobNumber() + "  " + determineSignoffTitle());

    updateLifecycle(job);

  }
  private void loadJob(){
    DatabaseObject<Job> jobRef = new DatabaseObject<Job>(Job.class, companyKey, jobKey);
    jobRef.addValueEventListener(new DatabaseObjectEventListener<Job>() {
      @Override
      public void onChange(String key, Job job) {
        SignOffActivity.this.job = job;
        updateFromJob();
        tvMoveSummary.setText(constructSummary());
      }
    });

  }


  private void makeGone(int id){
    View v = findViewById(id);
    v.setVisibility(View.GONE);
  }

  private void show(int id){
    View v = findViewById(id);
    v.setVisibility(View.VISIBLE);
  }


  private TextView getTextView(int id){
    TextView tv = (TextView) findViewById(id);
    return tv;
  }

  private void updateFromCompany(){
    if (company == null){
      return; // this is an error
    }
    tvCompanyName.setText(company.getName());
    String address = TextUtility.formSingleLineAddress(company.getAddress());
    tvCompanyAddress.setText(address);

    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
      tvCompanyPhone.setText(PhoneNumberUtils.formatNumber(company.getPhoneNumber(), "US" ));
    } else {
      tvCompanyPhone.setText(PhoneNumberUtils.formatNumber(company.getPhoneNumber()));
    }

    if (TextUtility.isBlank(company.getUsDot())){
      makeGone(R.id.tvUsDot);
      makeGone(R.id.tvUsDotLabel);
    } else {
      show(R.id.tvUsDot);
      show(R.id.tvUsDotLabel);
      getTextView(R.id.tvUsDot).setText(company.getUsDot());
    }

    if (TextUtility.isBlank(company.getIccMc())){
      makeGone(R.id.tvIccMc);
      makeGone(R.id.tvIccMcLabel);
    } else {
      show(R.id.tvIccMc);
      show(R.id.tvIccMcLabel);
      getTextView(R.id.tvIccMc).setText(company.getIccMc());
    }

    if (TextUtility.isBlank(company.getCalT())){
      makeGone(R.id.tvCalT);
      makeGone(R.id.tvCalTLabel);
    } else {
      show(R.id.tvCalT);
      show(R.id.tvCalTLabel);
      getTextView(R.id.tvCalT).setText(company.getCalT());
    }


    String logoImage = app().getCompanyLogoUrl();
    if (logoImage != null && logoImage.length() > 0){
      imageLoader.get(logoImage, ImageLoader.getImageListener(ivCompanyLogo,
              R.drawable.transparent, R.drawable.transparent));
    }

  }

  private String moveDestination(){
    String result = "";
    switch (job.getLifecycle()){
      case New:
        if (job.getStorageInTransit()){
          result = "Storage";
        } else {
          result =  TextUtility.formSingleLineAddress(job.getDestinationAddress());
        }
        break;
      case LoadedForStorage:
        result =  "Storage";
        break;
      case InStorage:
        result =  TextUtility.formSingleLineAddress(job.getDestinationAddress());
        break;
      case LoadedForDelivery:
        result =  TextUtility.formSingleLineAddress(job.getDestinationAddress());
        break;
      case Delivered:
        result =  TextUtility.formSingleLineAddress(job.getDestinationAddress());
        break;
    }
    return result;
  }


  private String constructSummary(){


    String summary = "";
    if (app().getCurrentCompany() != null && app().getCurrentCompany().getExposeValueToCustomers()){
      summary =  "• " + Integer.toString(totalItems) + " Items valued at $" + totalValue + "\n" +
              "• " + "Move destination is " + moveDestination() + "\n";
    } else {
      // remove the value phrease
      summary =  "• " + Integer.toString(totalItems) + " Items\n" +
              "• " + "Move destination is " + moveDestination() + "\n";
    }

    return summary;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sign_off);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    entryLifecycle = Job.Lifecycle.valueOf(params.getString("lifecycle"));
    storageInTransit = params.getBoolean("storageInTransit");

    imageLoader = MyVolley.getImageLoader();

    totalItems = params.getInt("totalItems");
    totalValue = params.getInt("totalValue");
    totalPads = params.getInt("totalPads");
    totalVolumeCubicFeet = params.getFloat("totalVolumeCubicFeet");
    totalWeightLbs = params.getFloat("totalWeightLbs");
    totalDamagedItems = params.getInt("totalDamagedItems");

    tvDate = (TextView) findViewById(R.id.tvDate);
    DateTimeFormatter fmt = DateTimeFormat.forPattern("EEE, MMM d yyyy, h:mm a z");
    DateTime now = new DateTime();
    tvDate.setText(fmt.print(now));
    tvForeman = (TextView) findViewById(R.id.tvForeman);
    tvForeman.setText(app().getCurrentUser().getFirstName() + " " + app().getCurrentUser().getLastName());

    tvCompanyName = (TextView) findViewById(R.id.tvCompanyName);
    tvCompanyAddress = (TextView) findViewById(R.id.tvCompanyAddress);
    tvCompanyPhone = (TextView) findViewById(R.id.tvCompanyPhone);
    tvShipperName = (TextView) findViewById(R.id.tvShipperName);
    tvShipperPhone = (TextView) findViewById(R.id.tvShipperPhone);
    tvShipperAddress = (TextView) findViewById(R.id.tvShipperAddress);
    tvShipperEmail = (TextView) findViewById(R.id.tvShipperEmail);
    tvMoveSummary = (TextView) findViewById(R.id.tvMoveSummary);
    ivCompanyLogo = (ImageView) findViewById(R.id.ivCompanyLogo);


    ivNew = (ImageView) findViewById(R.id.ivNew);
    ivLoadedForStorage = (ImageView) findViewById(R.id.ivLoadedForStorage);
    ivInStorage = (ImageView) findViewById(R.id.ivInStorage);
    ivLoadedForDelivery = (ImageView) findViewById(R.id.ivLoadedForDelivery);
    ivDelivered = (ImageView) findViewById(R.id.ivDelivered);

    // we have to load both the company and job
    loadJob();

    company = app().getCurrentCompany();

    updateFromCompany();

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Sign Off");
    setSupportActionBar(toolbar);


    signaturePad = (SignaturePad) findViewById(R.id.signature_pad);
    clearPad = (Button) findViewById(R.id.buttonClearPad);
    accept = (Button) findViewById(R.id.buttonAcceptSignature);
    signHere = (TextView) findViewById(R.id.tvSignHere);
    tvName = (TextView) findViewById(R.id.tvCustomerName);


    signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
      @Override
      public void onStartSigning() {

        Log.d(TAG, "onStartSigning");
        signHere.setVisibility(View.INVISIBLE);
      }

      @Override
      public void onSigned() {
        Log.d(TAG, "onSigned");
        accept.setEnabled(true);
        clearPad.setEnabled(true);

      }

      @Override
      public void onClear() {
        Log.d(TAG, "onClear");

        accept.setEnabled(false);
        clearPad.setEnabled(false);
        signHere.setVisibility(View.VISIBLE);
      }
    });

    accept.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (tvName.getText().length() == 0){
          tvName.setError("Please Enter Your Name");
          return;
        }
        tvName.setError(null);
        progressDialog  = ProgressDialog.show(thisActivity, "Saving Signature", "");
        progressDialog.show();

        Bitmap signaturePage = getScreenShot(getRootView());
        saveChanges(signaturePage, tvName.getText().toString());

      }
    });
    clearPad.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        signaturePad.clear();
        tvName.setError(null);
      }
    });

  }

  public  Bitmap getScreenShot(View view) {
    View screenView = view.getRootView();
    screenView.setDrawingCacheEnabled(true);
    Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
    screenView.setDrawingCacheEnabled(false);
    return bitmap;
  }


  private void saveChanges(Bitmap signatureBitmap, final String name){
    // move the job to Loaded,
    // add the customer signature
    FirebaseStorage storage = FirebaseStorage.getInstance();


    StorageReference customerSignature = storage.getReferenceFromUrl(app().getStorageUrl()).child("signatures/" + companyKey + "/" + jobKey + "/" + entryLifecycle);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 55, outputStream);
    final byte[] data = outputStream.toByteArray();
    UploadTask uploadTask = customerSignature.putBytes(data);
    uploadTask.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Log.d(TAG, "upload failed");
        Utility.error(getRootView(), thisActivity, "Signature upload failed. Try again");
        progressDialog.dismiss();

        progressDialog = null;
      }
    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){

      @Override
      public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        // signature image upload succeeded. now we need to move the lifecycle and update signatures

        Job.Lifecycle nextState = getNextLifecyle();

        new DatabaseObject<Job>(Job.class, companyKey, jobKey).child("lifecycle").setValue(nextState);
        //FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/lifecycle").setValue(getNextLifecyle().toString());
        @SuppressWarnings("VisibleForTests")
        Uri downloadUrl = taskSnapshot.getDownloadUrl();
        //FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/signature" + entryLifecycle.toString()).setValue(new Signature(name, downloadUrl.toString()));
        new DatabaseObject<Job>(Job.class, companyKey, jobKey).child("signature" + nextState)
                .setValue(new Signature(name, downloadUrl.toString(), new DateTime().getMillis()));
        progressDialog.dismiss();
        progressDialog = null;
        setResult(RESULT_OK);
        finish();
      }
    });



  }
  private String determineSignoffTitle(){
    if (entryLifecycle == Job.Lifecycle.New){
      return "Pickup";
    } else if (entryLifecycle == Job.Lifecycle.LoadedForStorage ){
      return "Unload Warehouse";
    } else if (entryLifecycle == Job.Lifecycle.InStorage){
      return "Loaded on Truck";
    } else if (entryLifecycle == Job.Lifecycle.LoadedForDelivery){
      return "Delivery";
    } else {
      // this case shouldn't occure
      return "";
    }
  }
  private Job.Lifecycle getNextLifecyle(){
    if (entryLifecycle == Job.Lifecycle.New && storageInTransit) {
      return Job.Lifecycle.LoadedForStorage;
    } else if (entryLifecycle == Job.Lifecycle.New && !storageInTransit) {
      return Job.Lifecycle.LoadedForDelivery;
    } else if (entryLifecycle == Job.Lifecycle.LoadedForStorage  ){
      return Job.Lifecycle.InStorage;
    }else if (entryLifecycle == Job.Lifecycle.InStorage ){
      return Job.Lifecycle.LoadedForDelivery;
    } else if (entryLifecycle == Job.Lifecycle.LoadedForDelivery){
      return Job.Lifecycle.Delivered;
    } else {
      // some error/ try to fail fast
      return null;
    }
  }
}
