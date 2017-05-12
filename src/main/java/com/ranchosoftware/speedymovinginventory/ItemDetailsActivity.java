package com.ranchosoftware.speedymovinginventory;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.ScanRecord;
import com.ranchosoftware.speedymovinginventory.utility.Permissions;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ItemDetailsActivity extends BaseActivity {

  private static final String TAG = ItemDetailsActivity.class.getSimpleName();

  private String companyKey;
  private String jobKey;
  private String qrcCode;
  private Item item;
  private GridView photoGridView;
  private ImageAdapter adapter;
  private TextView tvNoPhotosMessage;
  private CheckBox checkBoxDamaged;
  private EditText claimNumber;
  private EditText damageDescription;
  private EditText specialHandling;
  private CheckBox checkBoxIsBox;
  private TextView descriptionView;
  private Switch scanOverrideSwitch;

  private MediaPlayer positivePlayer;
  private Vibrator vibrator;
  private DatabaseReference itemRef;
  private static final int RC_HANDLE_ACCESS_FINE  = 3;
  private Location currentLocation;
  private Job.Lifecycle lifecycle;


  private final LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(final Location location) {
      //your code here
      currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
  };
  private ValueEventListener itemRefEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      if (dataSnapshot.getValue() == null){
        // there is some sort of error
        Log.d(TAG, "Serious Error");
        Utility.error(getRootView(), thisActivity, "Error: Item Detail Activity, unexpected null.");
      } else {
        try {
          item = dataSnapshot.getValue(Item.class);
        } catch (Exception e){
          // err pulling the item out
          Utility.error(getRootView(), thisActivity, "Error retrieving item, contact support. ErrorCode=" + "ItemDetailsActivity:"+e.getMessage());
          return;
        }
        updateValuesFromItem();
        showProgress(false, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
      }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
    }
  };

  private void updateValuesFromItem(){
    // cleare the progrees bar/

    checkBoxIsBox.setChecked(item.getIsBox());
    descriptionView.setText(item.getDescription());
    checkBoxDamaged.setChecked(item.getHasClaim());
    damageDescription.setText(item.getDamageDescription());
    claimNumber.setText(item.getClaimNumber());
    adapter.clear();
    for (String key : item.getImageReferences().keySet()){
      String stringUri = item.getImageReferences().get(key);
      adapter.add(new ImageAdapterItem(key, stringUri));
    }

    if ( !app().userIsAtLeastForeman()){
      scanOverrideSwitch.setVisibility(View.INVISIBLE);
    } else {
      scanOverrideSwitch.setVisibility(View.VISIBLE);
    }

    ((TextView)findViewById(R.id.tvValue)).setText("$" + String.format("%.2f",item.getMonetaryValue()));
    ((TextView)findViewById(R.id.tvNumberOfPads)).setText(String.valueOf(item.getNumberOfPads()));
    ((TextView)findViewById(R.id.tvCategory)).setText(item.getCategory());
    ((TextView)findViewById(R.id.tvPackedBy)).setText(item.getPackedBy());
    specialHandling.setText(item.getSpecialHandling());

    ((CheckBox)findViewById(R.id.cbDisassembled)).setChecked(item.getIsDisassembled());

    String styled = String.format("%.1f", (float) item.getVolume()) + " ft3";
    SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
    superScript.setSpan(new SuperscriptSpan(), styled.length() - 1, styled.length(), 0);
    superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() - 1, styled.length(), 0);
    ((TextView)findViewById(R.id.tvVolume)).setText(superScript);

    ((TextView)findViewById(R.id.tvWeight)).setText(String.format("%.0f",item.getWeightLbs()) + " lbs");

    scanOverrideSwitch.setChecked(item.getIsScanned());
    adapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_item_details);
    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    qrcCode = params.getString("itemCode");
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Item Details: " + qrcCode.substring(0, 5));
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    positivePlayer = MediaPlayer.create(thisActivity, R.raw.success);
    positivePlayer.setVolume(1.0f, 1.0f);
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    lifecycle = Job.Lifecycle.valueOf(params.getString("lifecycle"));

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    showProgress(true, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
    itemRef = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + qrcCode);

    photoGridView = (GridView) findViewById(R.id.photoGridView);
    tvNoPhotosMessage = (TextView) findViewById(R.id.tvNoPhotosMessage);
    checkBoxDamaged = (CheckBox) findViewById(R.id.cbDamaged);

    claimNumber = (EditText) findViewById(R.id.tvClaimNumber);
    damageDescription = (EditText) findViewById(R.id.editDamageDescription);
    specialHandling = (EditText) findViewById(R.id.editSpecialHandling);
    checkBoxIsBox = (CheckBox) findViewById(R.id.cbIsBox);
    descriptionView = (TextView) findViewById(R.id.tvDescription);
    scanOverrideSwitch = (Switch) findViewById(R.id.scanOverrideSwitch);

    scanOverrideSwitch.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Log.d(TAG, "sttf");
        if (scanOverrideSwitch.isChecked()){
          positivePlayer.start();
          vibrator.vibrate(100);
        }
      }
    });
    scanOverrideSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (item != null){
          if (checked) {
            if (!item.getIsScanned()) {
              item.setIsScanned(checked);
              double latitude = 33.158092;
              double longitude = -117.350594;
              if (currentLocation != null){
                latitude = currentLocation.getLatitude();
                longitude = currentLocation.getLongitude();
              }
              ScanRecord scanRecord = new ScanRecord(new DateTime(), latitude,
                      longitude, app().getCurrentUser().getUid(), true, lifecycle);

              DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/scanHistory/" + qrcCode).push();
              ref.setValue(scanRecord);
            }
            //positivePlayer.start();
          } else {
            item.setIsScanned(false);
          }
        }
      }
    });

    FloatingActionButton addPhotos = (FloatingActionButton) findViewById(R.id.fabAddPhotos);
    addPhotos.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        attemptTakePhoto();
      }
    });

    adapter = new ImageAdapter(thisActivity, new ArrayList<ImageAdapterItem>());
    photoGridView.setVisibility(View.INVISIBLE);
    photoGridView.setAdapter(adapter);

    // Check location before accessing
    // permission is not granted yet, request permission.
    int rc2 = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
    if (rc2 == PackageManager.PERMISSION_GRANTED) {
      LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100.0f, locationListener);
    } else {
      requestLocationPermission();
    }
  }

  private void requestLocationPermission(){
    Log.w(TAG, "Location permission is not granted. Requesting permission");

    final String[] permissions = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION};

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACCESS_FINE_LOCATION)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_ACCESS_FINE);
      return;
    }

    final Activity thisActivity = this;

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions,
                RC_HANDLE_ACCESS_FINE);
      }
    };

    Snackbar.make(getRootView(), R.string.permission_location_rationale,
            Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.ok, listener)
            .show();
  }

  private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;
  private static final int TAKE_PHOTO_CODE = 5;
  private static final int QRC_CODE_REPLACEMENT = 234;

  private void attemptTakePhoto() {
    if (Permissions.hasPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      launchCameraForResult();
    } else {
      Permissions.requestPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
              REQUEST_WRITE_EXTERNAL_STORAGE);
    }
  }
  private void launchCameraForResult() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        createImageFile();
        photoFile = imageFile;

      } catch (IOException ex) {
        // Error occurred while creating the File
        int x = 5;
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        Uri photoUri = FileProvider.getUriForFile(thisActivity, BuildConfig.APPLICATION_ID + ".provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
      }
    }
  }

  private File imageFile;
  private void createImageFile() throws IOException {
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";

    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
    );
    imageFile = image;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_WRITE_EXTERNAL_STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

          // permission was granted, yay! Do the
          // contacts-related task you need to do.
          launchCameraForResult();
        } else {
          DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.dismiss();
            }
          };

          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("Speedy Moving Inventory")
                  .setMessage(R.string.no_external_storage_permission)
                  .setPositiveButton(R.string.ok, listener)
                  .show();
        }
        return;
      }
      case RC_HANDLE_ACCESS_FINE:{
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "location granted - initialize tehe location stuff");
          // we have permission, so create the camerasource
          LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
          int rc2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
          if (rc2 == PackageManager.PERMISSION_GRANTED) {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100.0f, locationListener);
          }
          return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            finish();
          }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Speedy Moving Inventory")
                .setMessage(R.string.no_location_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
      }
      // other 'case' lines to check for other
      // permissions this app might request
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == TAKE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {

      handleTakePictureResult();

    } else if (requestCode == QRC_CODE_REPLACEMENT && resultCode == Activity.RESULT_OK){
      handleCodeReplacementResult(data);
    }
  }

  private Bitmap rotateImage(Bitmap source, float angle){
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  private Bitmap scaleBitmapIfNecessary(Bitmap bitmap){
    int maxSize = Math.max(bitmap.getWidth(), bitmap.getHeight());
    // bitmap to be at most 800
    int newWidth = 0;
    int newHeight = 0;
    if (maxSize > 800){
      if (bitmap.getWidth() > bitmap.getHeight()){
        newWidth = 800;
        newHeight = (int) (bitmap.getHeight() * (800.0/ (double)bitmap.getWidth()));
      } else {
        newHeight = 800;
        newWidth = (int) (bitmap.getWidth() * (800.0/(double) bitmap.getHeight()));
      }
      bitmap = bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }
    return bitmap;
  }

  private void handleCodeReplacementResult(Intent data){
    String newCode = data.getStringExtra("newCode");
    changeQrcCodeTo(newCode);
  }

  private void handleTakePictureResult(){
    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.toString());
    bitmap = scaleBitmapIfNecessary(bitmap);
    try {
      ExifInterface ei = new ExifInterface(imageFile.toString());
      int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

      switch(orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          bitmap = rotateImage(bitmap, 90);
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          bitmap = rotateImage(bitmap, 180);
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          bitmap = rotateImage(bitmap, 270);
          break;
      }
      // etc.
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (bitmap != null) {
      //
      writeBitmapBackToFile(bitmap);
      //photoGridView
      //adapter.add(imageFile.toString());
      //adapter.notifyDataSetChanged();
      FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
      StorageReference storageRef  = firebaseStorage.getReferenceFromUrl(app().getStorageUrl());

      DateTime dateTime = new DateTime();
      final long milliseconds = dateTime.getMillis();


      final StorageReference imagesRef = storageRef.child("images/"+ companyKey + "/" + jobKey + "/" + qrcCode + "/" + Long.toString(milliseconds));

      Uri fileUri = Uri.fromFile(imageFile);
      UploadTask uploadTask = imagesRef.putFile(fileUri);
      uploadTask.addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {

          // updload not successfule
          imageFile.delete();
        }
      }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

          Uri downloadUrl = taskSnapshot.getDownloadUrl();
          item.getImageReferences().put(Long.toString(milliseconds), downloadUrl.toString());
          itemRef.child("imageReferences").setValue(item.getImageReferences());
          imageFile.delete();
        }
      });



    } else {
      Toast.makeText(this, "Take Before failed", Toast.LENGTH_SHORT).show();
    }
  }

  private void writeBitmapBackToFile(Bitmap bitmap){
    try {
      FileOutputStream out = new FileOutputStream(imageFile.toString());
      bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
      out.flush();
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class ImageAdapterItem {
    private String dateKey;
    private String imageUrl;
    public ImageAdapterItem(String dateKey, String imageUrl){
      this.dateKey = dateKey;
      this.imageUrl = imageUrl;
    }
  }

  private class ImageAdapter extends ArrayAdapter<ImageAdapterItem> {
    private Context context;
    private LayoutInflater inflater;


    public ImageAdapter(Context context, List<ImageAdapterItem> images) {
      super(context, R.layout.item_photo_grid_item, images);
      this.context = context;
      inflater = LayoutInflater.from(context);
    }

    // comment


    @Override
    public int getItemViewType(int position) {
      int type =  super.getItemViewType(position);
      return type;
    }

    @Override
    public boolean hasStableIds() {
      boolean answer =  super.hasStableIds();
      return answer;
    }

    @Override
    public int getViewTypeCount() {
      int count =  super.getViewTypeCount();
      return count;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
      photoGridView.setVisibility(View.VISIBLE);
      tvNoPhotosMessage.setVisibility(View.INVISIBLE);
      if (convertView == null){
        convertView = inflater.inflate(R.layout.item_photo_grid_item, null);
      }

      ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
      TextView timestamp = (TextView) convertView.findViewById(R.id.tvTimeStamp);

      ImageAdapterItem item = getItem(position);
      if (timestamp != null){
        timestamp.setText(app().getImageDateTimeFormatter().print(Long.parseLong(item.dateKey)));
      }


      ImageLoader loader = MyVolley.getImageLoader();
      loader.get(item.imageUrl, ImageLoader.getImageListener(imageView,
              R.drawable.loading, R.drawable.load_failed));

      return convertView;
    }
  }
  @Override
  public void onStart() {
    super.onStart();
    itemRef.addValueEventListener(itemRefEventListener);
  }

  private void updateItemFromControls(){
    item.setHasClaim(checkBoxDamaged.isChecked());
    item.setClaimNumber(claimNumber.getText().toString());
    item.setDamageDescription(damageDescription.getText().toString());
    item.setSpecialHandling(specialHandling.getText().toString());
  }

  @Override
  public void onStop() {
    super.onStop();

    if (item != null) {
      updateItemFromControls();
      itemRef.setValue(item);
    }
    itemRef.removeEventListener(itemRefEventListener);
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    if (imageFile != null){
      String path = imageFile.getPath();
      state.putString("imageFile", path);
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {

    super.onRestoreInstanceState(state);
    String path = state.getString("imageFile");
    if (path != null){
      imageFile = new File(path);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.item_details_menu, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {

      case R.id.change_qrc_code:
        reassignScannerCode();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }


  private void reassignScannerCode(){
    Log.d(TAG, "stuff");
    // launch the scanner and tell him this is reassignment
    Intent intent = new Intent(thisActivity, ScanActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", companyKey);
    params.putString("jobKey", jobKey);
    params.putBoolean("isReplacementCode", true);
    params.putString("lifecycle", lifecycle.toString());

    intent.putExtras(params);
    startActivityForResult(intent, QRC_CODE_REPLACEMENT);
  }

  private void changeQrcCodeTo(final String newCode){
    // QRC codes exist in 2 places in the database:
    //      itemlists/<JobKey>/items/<QrcCode>/itemdata
    // and in the lookup
    //      qrcList/< <QrcCode>:<JobKey> >

    // For replacement we need to change both.
    // remove the old

    DatabaseReference newQrcCode = FirebaseDatabase.getInstance().getReference("qrcList/" + newCode);
    newQrcCode.setValue(jobKey, new DatabaseReference.CompletionListener(){
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        // remove old code
        itemRef.removeEventListener(itemRefEventListener);
        // reassign to new item
        itemRef = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + newCode);
        DatabaseReference oldQrcCode = FirebaseDatabase.getInstance().getReference("qrcList/" + qrcCode);
        oldQrcCode.removeValue();

        // replace in the item
        DatabaseReference oldItemReference = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + qrcCode);
        oldItemReference.removeValue();

        itemRef.setValue(item);


      }
    });

  }

}
