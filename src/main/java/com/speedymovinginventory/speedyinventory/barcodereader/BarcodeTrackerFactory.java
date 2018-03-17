
package com.speedymovinginventory.speedyinventory.barcodereader;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.speedymovinginventory.speedyinventory.ui.camera2.GraphicOverlay;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
  private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

  private BarcodeGraphicTracker.Callback callback;

  public BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay,
                               BarcodeGraphicTracker.Callback callback) {
    mGraphicOverlay = barcodeGraphicOverlay;
    this.callback = callback;
  }

  @Override
  public Tracker<Barcode> create(Barcode barcode) {
    BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
    return new BarcodeGraphicTracker(mGraphicOverlay, graphic, callback);
  }

}

