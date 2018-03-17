package com.speedymovinginventory.speedyinventory;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.speedymovinginventory.speedyinventory.utility.Permissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rob on 6/23/17.
 */

public abstract class LaunchCameraBaseActivity  extends BaseMenuActivity{
  private static final String TAG = LaunchCameraBaseActivity.class.getSimpleName();

  private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;
  private static final int TAKE_PHOTO_CODE = 5;
  private static final int RC_HANDLE_ACCESS_FINE  = 3;
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  protected Location currentLocation;


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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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

  protected void attemptTakePhoto() {
    if (Permissions.hasPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            && ActivityCompat.checkSelfPermission(thisActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      launchCameraForResult();
    } else if (!Permissions.hasPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
      Permissions.requestPermission(thisActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE,
              REQUEST_WRITE_EXTERNAL_STORAGE);
    } else {
      requestCameraPermission();
    }
  }

  /**
   * Handles the requesting of the camera permission.  This includes
   * showing a "Snackbar" message of why the permission is needed then
   * sending the request.
   */
  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission is not granted. Requesting permission");

    final String[] permissions = new String[]{android.Manifest.permission.CAMERA};

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
            android.Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;


    ActivityCompat.requestPermissions(thisActivity, permissions,
            RC_HANDLE_CAMERA_PERM);



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
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_WRITE_EXTERNAL_STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

          // permission was granted, yay! Do the
          // contacts-related task you need to do.
          if (ActivityCompat.checkSelfPermission(thisActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            launchCameraForResult();
          } else {
            requestCameraPermission();
          }

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

      case RC_HANDLE_CAMERA_PERM:{
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
          launchCameraForResult();
        }
      }
      case RC_HANDLE_ACCESS_FINE:{
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "location granted - initialize tehe location stuff");
          // we have permission, so create the camerasource
          LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
          int rc2 = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
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
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {

      handleTakePictureResult();

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
      tookPicture(imageFile);


    } else {
      Toast.makeText(this, "Take Before failed", Toast.LENGTH_SHORT).show();
    }
  }

  abstract protected void tookPicture(final File imageFile);

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
}
