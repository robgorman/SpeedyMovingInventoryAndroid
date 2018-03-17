package com.speedymovinginventory.speedyinventory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.speedymovinginventory.speedyinventory.barcodereader.BarcodeGraphic;
import com.speedymovinginventory.speedyinventory.barcodereader.BarcodeGraphicTracker;
import com.speedymovinginventory.speedyinventory.model.Item;
import com.speedymovinginventory.speedyinventory.model.Job;
import com.speedymovinginventory.speedyinventory.model.ScanRecord;
import com.speedymovinginventory.speedyinventory.ui.camera2.CameraSource;
import com.speedymovinginventory.speedyinventory.ui.camera2.CameraSourcePreview;
import com.speedymovinginventory.speedyinventory.ui.camera2.GraphicOverlay;

import com.speedymovinginventory.speedyinventory.barcodereader.BarcodeTrackerFactory;
import com.speedymovinginventory.speedyinventory.utility.Utility;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

public class ScanActivity extends BaseActivity {

  private static final String TAG = "Barcode-reader";

  // intent request code to handle updating play services if needed.
  private static final int RC_HANDLE_GMS = 9001;

  // permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  private static final int RC_HANDLE_ACCESS_FINE  = 3;

  // constants used to pass extra data in the intent
  public static final String AutoFocus = "AutoFocus";
  public static final String UseFlash = "UseFlash";
  public static final String BarcodeObject = "Barcode";

  private CameraSource cameraSource;
  private CameraSourcePreview preview;
  private GraphicOverlay<BarcodeGraphic> graphicOverlay;

  // helper objects for detecting taps and pinches.
  private ScaleGestureDetector scaleGestureDetector;
  private GestureDetector gestureDetector;

  private MediaPlayer tadaPlayer;
  private MediaPlayer positivePlayer;
  private MediaPlayer itemExistsPlayer;
  private MediaPlayer negativePlayer;
  private MediaPlayer invalidCodePlayer;
  private Vibrator vibrator;
  private Job job;

  private String jobKey;
  private String companyKey;
  private Boolean isReplacementCode = false;
  private Boolean allowItemAddOutsideNew = false;
  private View topView;
  private TextView scannerMessage;

  private Button cameraLightButton;

  private boolean processingCode = false;
  private Job.Lifecycle lifecycle;

  private boolean allowScans = false;

  private Button nextButton;
  private ImageView checkMark;
  /**
   * Initializes the UI and creates the detector pipeline.
   */

  private Location currentLocation;

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


  public static Intent getLaunchIntent(
          Context context,
          String companyKey,
          String jobKey,
          Boolean allowItemAddOutsideNew,
          String lifecycle){
    Intent intent = new Intent(context, ScanActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", companyKey);
    params.putString("jobKey", jobKey);
    params.putBoolean("allowItemAddOutsideNew", allowItemAddOutsideNew);
    params.putString("lifecycle", lifecycle);
    intent.putExtras(params);
    return intent;
  }


  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
     setContentView(R.layout.activity_scan);

    Bundle b = getIntent().getExtras();
    jobKey = b.getString("jobKey");
    companyKey = b.getString("companyKey");
    isReplacementCode = b.getBoolean("isReplacementCode",false);
    allowItemAddOutsideNew = b.getBoolean("allowItemAddOutsideNew", false);
    lifecycle = Job.Lifecycle.valueOf(b.getString("lifecycle"));


    tadaPlayer = MediaPlayer.create(thisActivity, R.raw.tada);
    tadaPlayer.setVolume(1.0f, 1.0f);

    positivePlayer = MediaPlayer.create(thisActivity, R.raw.success);
    positivePlayer.setVolume(1.0f, 1.0f);
    negativePlayer = MediaPlayer.create(thisActivity, R.raw.item_not_found);
    negativePlayer.setVolume(1.0f, 1.0f);
    itemExistsPlayer = MediaPlayer.create(thisActivity, R.raw.alreadyscanned);
    if (itemExistsPlayer != null) {
      itemExistsPlayer.setVolume(1.0f, 1.0f);
    } else {
      itemExistsPlayer = positivePlayer;
    }
    cameraLightButton = (Button) findViewById(R.id.buttonCameraLight);
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    invalidCodePlayer = MediaPlayer.create(thisActivity, R.raw.negative_beep);
    invalidCodePlayer.setVolume(1.0f, 1.0f);
    scannerMessage = (TextView) findViewById(R.id.tvScannerMessage);
    nextButton = (Button) findViewById(R.id.buttonNext);
    nextButton.setVisibility(View.INVISIBLE);
    nextButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        processingCode = false;
        nextButton.setVisibility(View.INVISIBLE);
        checkMark.setVisibility(View.INVISIBLE);
        scannerMessage.setText("Point camera at a QR Code");
      }
    });

    checkMark = (ImageView) findViewById(R.id.ivCheckMark);
    checkMark.setVisibility(View.INVISIBLE);



    FirebaseDatabase database = FirebaseDatabase.getInstance();
    scannerMessage.setText("QRC Scanner initializing...");
    database.getReference("joblists/" + companyKey + "/jobs/" + jobKey).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        try {
          job = dataSnapshot.getValue(Job.class);
          if (isReplacementCode){
            scannerMessage.setText("Point camera at QRC Label Replacement");
          } else {
            scannerMessage.setText("Point camera at a QR Code");
          }
          enableControlsForJob();
        } catch (Exception e){
          Utility.error(getRootView(), thisActivity, "Unable to read job from database. Contact support. ErrorCode=" + "ScanActivity:" + e.getMessage());
          finish();
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {
        Utility.error(getRootView(), thisActivity, "unknown error, exiting scanner");
        finish();
      }
    });

    topView = findViewById(R.id.topLayout);
    preview = (CameraSourcePreview) findViewById(R.id.preview);
    graphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.graphicOverlay);

    // read parameters from the intent used to launch the activity.
    boolean autoFocus = true;
    boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

    // Check for the camera permission before accessing the camera.  If the
    // permission is not granted yet, request permission.
    int rc = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      updateUIBasedOnFlash();
      createCameraSource(autoFocus, useFlash);

    } else {
      requestCameraPermission();
    }
    gestureDetector = new GestureDetector(this, new CaptureGestureListener());
    scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

    Button endScan = (Button) findViewById(R.id.buttonEndScan);
    endScan.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
        // this only necessary when we were launched in activityForResult
        // which only occurs for QRC code replacement
        setResult(RESULT_CANCELED);
      }
    });
    if (isReplacementCode){
      endScan.setText("Cancel");
    }


    cameraLightButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (cameraSource.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)){
          cameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        } else {
          cameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }

      }
    });

    // hide the cameraLight button if the device doesn't have one



    // Check location before accessing
    // permission is not granted yet, request permission.
    int rc2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    if (rc2 == PackageManager.PERMISSION_GRANTED) {
      allowScans = true;
      LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (currentLocation == null) {
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      }

      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100.0f, locationListener);
    } else {
      requestLocationPermission();
    }



  }


  private void updateUIBasedOnFlash(){

    //return;



      Camera camera = Camera.open();
      if (camera == null){
        return;
      }
      Camera.Parameters params = camera.getParameters();
      if (params == null) {
        camera.release();
        return ;
      }
      camera.release();


      List<String> supportedFlashModes = params.getSupportedFlashModes();
      if (supportedFlashModes == null || supportedFlashModes.size() == 0) {

        return ;
      }

      for (int i = 0; i < supportedFlashModes.size(); i++) {
        String mode = supportedFlashModes.get(i);
        if (mode.equalsIgnoreCase( Camera.Parameters.FLASH_MODE_TORCH)) {
          cameraLightButton.setVisibility(View.VISIBLE);

        }
      }

    return ;

  }
  private void requestLocationPermission(){
    Log.w(TAG, "Location permission is not granted. Requesting permission");

    final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

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

    Snackbar.make(graphicOverlay, R.string.permission_location_rationale,
            Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.ok, listener)
            .show();
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

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM);
      }
    };

    Snackbar.make(graphicOverlay, R.string.permission_camera_rationale,
            Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.ok, listener)
            .show();
  }


  /**
   * Creates and starts the camera.  Note that this uses a higher resolution in comparison
   * to other detection examples to enable the barcode detector to detect small barcodes
   * at long distances.
   *
   * Suppressing InlinedApi since there is a check that the minimum version is met before using
   * the constant.
   */
  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash) {
    Context context = getApplicationContext();

    // A barcode detector is created to track barcodes.  An associated multi-processor instance
    // is set to receive the barcode detection results, track the barcodes, and maintain
    // graphics for each barcode on screen.  The factory is used by the multi-processor to
    // create a separate tracker instance for each barcode.
    BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
    .setBarcodeFormats(Barcode.QR_CODE)
    .build();


    BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(graphicOverlay, new BarcodeGraphicTracker.Callback() {
      @Override
      public void itemRecognized(final String code) {

        // no new scans accepted if we are already processing
        if (processingCode){
          return;
        }
        if (job != null) {
          processingCode = true;
          thisActivity.runOnUiThread(new Runnable(){
            public void run(){
              if(allowScans) {
                barcodeScanned(code);
              } else {
                processingCode = false;
              }
            }

          });

        } else {

        }

        // create an item with this code
      }
    });
    barcodeDetector.setProcessor(
            new MultiProcessor.Builder<>(barcodeFactory).build());

    if (!barcodeDetector.isOperational()) {
      // Note: The first time that an app using the barcode or face API is installed on a
      // device, GMS will download a native libraries to the device in order to do detection.
      // Usually this completes before the app is run for the first time.  But if that
      // download has not yet completed, then the above call will not detect any barcodes
      // and/or faces.
      //
      // isOperational() can be used to check if the required native libraries are currently
      // available.  The detectors will automatically become operational once the library
      // downloads complete on device.
      Log.w(TAG, "Detector dependencies are not yet available.");

      // Check for low storage.  If there is low storage, the native library will not be
      // downloaded, so detection will not become operational.
      IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

      if (hasLowStorage) {
        Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
        Log.w(TAG, getString(R.string.low_storage_error));
      }
    }

    // Creates and starts the camera.  Note that this uses a higher resolution in comparison
    // to other detection examples to enable the barcode detector to detect small barcodes
    // at long distances.
    CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
            .setRequestedFps(15.0f);

    // make sure that auto focus is an available option
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }



    cameraSource = builder.build();
    //cameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
  }

  /**
   * Restarts the camera.
   */
  @Override
  protected void onResume() {
    super.onResume();
    processingCode = false;
    scannerMessage.setText("Point camera at a QR Code");
    startCameraSource();
  }

  /**
   * Stops the camera.
   */

  @Override
  protected void onPause() {
    super.onPause();

    if (preview != null) {
      preview.stop();
    }
  }

  /**
   * Releases the resources associated with the camera source, the associated detectors, and the
   * rest of the processing pipeline.
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();


    if (preview != null) {
      preview.release();
    }
  }

  /**
   * Callback for the result from requesting permissions. This method
   * is invoked for every call on {@link #requestPermissions(String[], int)}.
   * <p>
   * <strong>Note:</strong> It is possible that the permissions request interaction
   * with the user is interrupted. In this case you will receive empty permissions
   * and results arrays which should be treated as a cancellation.
   * </p>
   *
   * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
   * @param permissions  The requested permissions. Never null.
   * @param grantResults The grant results for the corresponding permissions
   *                     which is either {@link PackageManager#PERMISSION_GRANTED}
   *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
   * @see #requestPermissions(String[], int)
   */
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (!(requestCode == RC_HANDLE_CAMERA_PERM || requestCode == RC_HANDLE_ACCESS_FINE)) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }
    if (requestCode == RC_HANDLE_CAMERA_PERM) {

      if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Camera permission granted - initialize the camera source");
        // we have permission, so create the camerasource
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
        updateUIBasedOnFlash();
        createCameraSource(autoFocus, useFlash);

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
              .setMessage(R.string.no_camera_permission)
              .setPositiveButton(R.string.ok, listener)
              .show();
    } else if (requestCode == RC_HANDLE_ACCESS_FINE){
      if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "location granted - initialize tehe location stuff");
        // we have permission, so create the camerasource
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        int rc2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (rc2 == PackageManager.PERMISSION_GRANTED) {
          currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          if (currentLocation == null) {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
          }
          locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100.0f, locationListener);
        }

        allowScans = true;
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


  }

  /**
   * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() throws SecurityException {
    // check that the device has play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg =
              GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (cameraSource != null) {
      try {
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  /**
   * onTap is called to capture the oldest barcode currently detected and
   * return it to the caller.
   *
   * @param rawX - the raw position of the tap
   * @param rawY - the raw position of the tap.
   * @return true if the activity is ending.
   */
  private boolean onTap(float rawX, float rawY) {

    BarcodeGraphic graphic = graphicOverlay.getGraphics().get(0);
    Barcode barcode = null;
    if (graphic != null) {
      barcode = graphic.getBarcode();
      if (barcode != null) {
        Intent data = new Intent();
        data.putExtra(BarcodeObject, barcode);
        setResult(CommonStatusCodes.SUCCESS, data);
        finish();
      }
      else {
        Log.d(TAG, "barcode data is null");
      }
    }
    else {
      Log.d(TAG,"no barcode detected");
    }
    return barcode != null;
  }

  private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {

      return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
    }
  }

  private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

    /**
     * Responds to scaling events for a gesture in progress.
     * Reported by pointer motion.
     *
     * @param detector The detector reporting the event - use this to
     *                 retrieve extended info about event state.
     * @return Whether or not the detector should consider this event
     * as handled. If an event was not handled, the detector
     * will continue to accumulate movement until an event is
     * handled. This can be useful if an application, for example,
     * only wants to update scaling factors if the change is
     * greater than 0.01.
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      return false;
    }

    /**
     * Responds to the beginning of a scaling gesture. Reported by
     * new pointers going down.
     *
     * @param detector The detector reporting the event - use this to
     *                 retrieve extended info about event state.
     * @return Whether or not the detector should continue recognizing
     * this gesture. For example, if a gesture is beginning
     * with a focal point outside of a region where it makes
     * sense, onScaleBegin() may return false to ignore the
     * rest of the gesture.
     */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      return true;
    }

    /**
     * Responds to the end of a scale gesture. Reported by existing
     * pointers going up.
     * <p/>
     * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
     * and {@link ScaleGestureDetector#getFocusY()} will return focal point
     * of the pointers remaining on the screen.
     *
     * @param detector The detector reporting the event - use this to
     *                 retrieve extended info about event state.
     */
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      cameraSource.doZoom(detector.getScaleFactor());
    }
  }

  private void enableControlsForJob(){
    // job is valid barcode scanning can begin

  }

  private void dataChanged(DataSnapshot dataSnapshot, String code){
    Item item;
    try {
      item = dataSnapshot.getValue(Item.class);
    } catch (Exception e){
      scannerMessage.setText("Unable to read item from database; Contact support. ErrorCode=" + e.getMessage());
      resetProcessing();
      return;
    }
    if (job.getLifecycle() == Job.Lifecycle.New) {
      editItem(job, code, item);
    } else {   // for any other job lifecycle just mark as scanned
      // check to see if already scanned if so note
      if (item.getIsScanned()){
        scannerMessage.setText("Item " + code.substring(0,5) + " has already been scanned.");
        showNext();
        //vibrator.vibrate(100);
      } else {
        // set the item as scanned
        double latitude = 33.158092;
        double longitude = -117.350594;
        if (currentLocation != null){
          latitude = currentLocation.getLatitude();
          longitude = currentLocation.getLongitude();
        }
        final ScanRecord scanRecord = new ScanRecord(new DateTime(), latitude,
                longitude, app().getCurrentUser().getUid(), false, lifecycle);


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/scanHistory/" + code).push();
        ref.setValue(scanRecord);
        FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey + "/items/" + code + "/isScanned").setValue(true);
        scannerMessage.setText("Item " + code.substring(0, 5) + " successfully scanned.");
        checkMark.setVisibility(View.VISIBLE);
        showNext();

        Query allScanned = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey + "/items/").orderByChild("isScanned");
        allScanned.addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() == null){
              Log.d(TAG, "Serious Error");

            } else {
              for (DataSnapshot nextSnapshot : dataSnapshot.getChildren()){
                try {
                  final Item item = nextSnapshot.getValue(Item.class);
                  if (!item.getIsScanned()){
                    // do something special
                    return;
                  }
                } catch (Exception e){
                  // nothing to do
                }
              }
              // if we get to here all items have been
              // scanned
              itemExistsPlayer.stop();
              scannerMessage.setText(scannerMessage.getText() + "\n" + "ALL ITEMS HAVE BEEN SCANNED!");
              tadaPlayer.start();
            }
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {

          }
        });
      }
      // delay another scan by 1 second


    }
  }

  private void dataChangedQrcList(DataSnapshot dataSnapshot, final String code){
    if (dataSnapshot.getValue() == null){
      // item is new
      if (isReplacementCode){
        // if its a replament code do one thing
        replaceCode(job, code);
      } else {
        createNewItem(job, code);
      }
      resetProcessing();
    } else {
      // item exists see if it is this job
      String itemJobKey = dataSnapshot.getValue(String.class);
      if (!itemJobKey.equals(jobKey)){
        scannerMessage.setText("This item belongs to another job.");
        negativePlayer.start();
        vibrator.vibrate(200);
        Log.d(TAG, "Item does not belong to this job");
        showNext();

      } else {
        itemExistsPlayer.start();
        vibrator.vibrate(100);
        //scannerMessage.setText("Item: Found");
        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey + "/items/" + code);
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            dataChanged(dataSnapshot, code);


          }

          @Override
          public void onCancelled(DatabaseError databaseError) {
            Log.d(TAG, "onCancelled");
          }
        });

      }
    }
  }

  private void barcodeScanned(final String code){
    Log.d(TAG, code);
    processingCode = true;

    if (!Utility.isQrcCodeValid(code)){
      invalidCodePlayer.start();
      vibrator.vibrate(300);
      scannerMessage.setText("Invalid QR Code -- Not a Speedy Moving Inventory Code");
      showNext();
      return;
    }

    //scannerMessage.setText("Point camera at a QRC Code");
    // lookup or create item based on scan
    //final Job job = app().getCurrentJob();
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("qrcList/" + code);
    ref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(final DataSnapshot dataSnapshot) {
        thisActivity.runOnUiThread(new Runnable(){
          @Override
          public void run() {
            dataChangedQrcList(dataSnapshot, code);
          }
        });


      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });


  }

  private void showNext(){
    nextButton.setVisibility(View.VISIBLE);
  }
  private void resetProcessing(){
    // delay at least 1 second between scanns
    new Handler().postDelayed(new Runnable(){
      @Override
      public void run() {
        processingCode = false;
      }
    }, 1000);
  }

  private void editItem(Job job, String code, Item item){
    Intent intent = new Intent(thisActivity, NewItemActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", companyKey);
    params.putString("jobKey", jobKey);
    params.putString("itemCode", code);
    params.putBoolean("itemIsOutOfPhase", allowItemAddOutsideNew);
    params.putBoolean("invokedByScanner", true);
    params.putString("lifecycle", job.getLifecycle().toString());
    intent.putExtras(params);
    startActivity(intent);

  }

  private void replaceCode(Job job, String newCode){
    // This is a special case. Not sure its a great design, but when
    // we are replacing a a code we have been invoked via startActivityForResult()
    // and we need just return the new code. No other work to dod
    positivePlayer.start();
    vibrator.vibrate(100);
    Intent returnData = new Intent();
    returnData.putExtra("newCode", newCode);
    setResult(RESULT_OK, returnData);
    finish();
  }

  private void createNewItem(Job job, String code){
    // the job has to be in status New for additional items to be created
    if (job.getLifecycle() != Job.Lifecycle.New && !allowItemAddOutsideNew){
      // its an error
      //Utility.error(topView, this, R.string.item_not_found);
      scannerMessage.setText(R.string.item_not_found);

      negativePlayer.start();
      vibrator.vibrate(300);
    } else {
      positivePlayer.start();
      vibrator.vibrate(100);
      Intent intent = new Intent(thisActivity, NewItemActivity.class);
      Bundle params = new Bundle();
      params.putString("companyKey", companyKey);
      params.putString("jobKey", jobKey);
      params.putString("itemCode", code);
      params.putBoolean("invokedByScanner", true);
      params.putString("lifecycle", job.getLifecycle().toString());
      intent.putExtras(params);
      startActivity(intent);

    }
  }


}
