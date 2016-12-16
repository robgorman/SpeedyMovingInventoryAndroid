package com.ranchosoftware.speedymovinginventory;

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
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
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
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.barcodereader.BarcodeGraphic;
import com.ranchosoftware.speedymovinginventory.barcodereader.BarcodeGraphicTracker;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.ui.camera.CameraSource;
import com.ranchosoftware.speedymovinginventory.ui.camera.CameraSourcePreview;
import com.ranchosoftware.speedymovinginventory.ui.camera.GraphicOverlay;

import com.ranchosoftware.speedymovinginventory.barcodereader.BarcodeTrackerFactory;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import java.io.IOException;

public class ScanActivity extends BaseActivity {

  private static final String TAG = "Barcode-reader";

  // intent request code to handle updating play services if needed.
  private static final int RC_HANDLE_GMS = 9001;

  // permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 2;

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

  private MediaPlayer positivePlayer;
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

  private boolean processingCode = false;
  /**
   * Initializes the UI and creates the detector pipeline.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
     setContentView(R.layout.activity_scan);

    Bundle b = getIntent().getExtras();
    jobKey = b.getString("jobKey");
    companyKey = b.getString("companyKey");
    isReplacementCode = b.getBoolean("isReplacementCode",false);
    allowItemAddOutsideNew = b.getBoolean("allowItemAddOutsideNew", false);



    positivePlayer = MediaPlayer.create(thisActivity, R.raw.checkout_beep);
    positivePlayer.setVolume(1.0f, 1.0f);
    negativePlayer = MediaPlayer.create(thisActivity, R.raw.negative_beep);
    negativePlayer.setVolume(1.0f, 1.0f);

    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    invalidCodePlayer = MediaPlayer.create(thisActivity, R.raw.negative_beep_2);
    invalidCodePlayer.setVolume(1.0f, 1.0f);
    scannerMessage = (TextView) findViewById(R.id.tvScannerMessage);

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
            scannerMessage.setText("Point camera at a QRC Code");
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

          thisActivity.runOnUiThread(new Runnable(){
            public void run(){
              barcodeScanned(code);
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
            .setFlashMode(Camera.Parameters.FLASH_MODE_AUTO)
            .setRequestedFps(15.0f);

    // make sure that auto focus is an available option
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    cameraSource = builder.build();
  }

  /**
   * Restarts the camera.
   */
  @Override
  protected void onResume() {
    super.onResume();
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
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Camera permission granted - initialize the camera source");
      // we have permission, so create the camerasource
      boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
      boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
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

    BarcodeGraphic graphic = graphicOverlay.getFirstGraphic();
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

  private void barcodeScanned(final String code){
    Log.d(TAG, code);

    if (!Utility.isQrcCodeValid(code)){
      invalidCodePlayer.start();
      vibrator.vibrate(200);
      scannerMessage.setText("Invalid QRC Code -- Not a Speedy Moving Inventory Code");
      processingCode = false;
      return;
    }

    //scannerMessage.setText("Point camera at a QRC Code");
    // lookup or create item based on scan
    //final Job job = app().getCurrentJob();
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("qrcList/" + code);
    ref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot.getValue() == null){
          // item is new
          if (isReplacementCode){
            // if its a replament code do one thing
            replaceCode(job, code);
          } else {
            createNewItem(job, code);
          }
          processingCode = false;
        } else {
          // item exists see if it is this job
          String itemJobKey = dataSnapshot.getValue(String.class);
          if (!itemJobKey.equals(jobKey)){
            scannerMessage.setText("This item belongs to another job.");
            negativePlayer.start();
            vibrator.vibrate(200);
            Log.d(TAG, "Item does not belong to this job");
            processingCode = false;
          } else {
            positivePlayer.start();
            DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey + "/items/" + code);
            itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
              @Override
              public void onDataChange(DataSnapshot dataSnapshot) {
                Item item;
                try {
                  item = dataSnapshot.getValue(Item.class);
                } catch (Exception e){
                  scannerMessage.setText("Unable to read item from database; Contact support. ErrorCode=" + e.getMessage());
                  processingCode = false;
                  return;
                }
                if (job.getLifecycle() == Job.Lifecycle.New) {
                  editItem(job, code, item);
                } else {   // for any other job lifecycle just mark as scanned
                  // check to see if already scanned if so note
                  if (item.getIsScanned()){
                    scannerMessage.setText("This item has already been scanned.");
                    vibrator.vibrate(50);
                  } else {
                    // set the item as scanned
                    FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey + "/items/" + code + "/isScanned").setValue(true);
                  }

                }
                processingCode = false;
              }

              @Override
              public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
              }
            });

          }
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });


  }

  private void editItem(Job job, String code, Item item){
    Intent intent = new Intent(thisActivity, NewItemActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", companyKey);
    params.putString("jobKey", jobKey);
    params.putString("itemCode", code);
    params.putBoolean("itemIsOutOfPhase", allowItemAddOutsideNew);
    intent.putExtras(params);
    startActivity(intent);

  }

  private void replaceCode(Job job, String newCode){
    // This is a special case. Not sure its a great design, but when
    // we are replacing a a code we have been invoked via startActivityForResult()
    // and we need just return the new code. No other work to dod
    positivePlayer.start();
    Intent returnData = new Intent();
    returnData.putExtra("newCode", newCode);
    setResult(RESULT_OK, returnData);
    finish();
  }

  private void createNewItem(Job job, String code){
    // the job has to be in status New for additional items to be created
    if (job.getLifecycle() != Job.Lifecycle.New && !allowItemAddOutsideNew){
      // its an error
      Utility.error(topView, this, R.string.item_not_found);
      negativePlayer.start();
      vibrator.vibrate(200);
    } else {
      positivePlayer.start();
      Intent intent = new Intent(thisActivity, NewItemActivity.class);
      Bundle params = new Bundle();
      params.putString("companyKey", companyKey);
      params.putString("jobKey", jobKey);
      params.putString("itemCode", code);
      intent.putExtras(params);
      startActivity(intent);

    }
  }
}
