package com.ranchosoftware.speedymovinginventory;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.print.PrintHelper;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.Signature;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import java.util.ArrayList;

public class PrintActivity extends BaseActivity {


  private ArrayList<Job.Lifecycle> printables = new ArrayList<>();

  DatabaseObject<Job> jobRef;
  private Job job;

  private View noPrintablesMessage;
  private ListView listView;
  private String jobKey;
  private String companyKey;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_print);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Print a Signoff");
    setSupportActionBar(toolbar);

    Bundle b = getIntent().getExtras();
    jobKey = b.getString("jobKey");
    companyKey = b.getString("companyKey");

    noPrintablesMessage = findViewById(R.id.llNoPrintableItemsMessage);
    listView = (ListView) findViewById(R.id.printItemListView);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Job.Lifecycle lifecycle = printables.get(position);
        switch (lifecycle){
          case New:
            // this case shouldn't occur but handle
            break;
          case LoadedForStorage:
            print(job.getSignatureLoadedForStorage());
            break;
          case InStorage:
            print(job.getSignatureInStorage());
            break;
          case LoadedForDelivery:
            print(job.getSignatureLoadedForDelivery());
            break;
          case Delivered:
            print(job.getSignatureDelivered());
            break;
        }
      }
    });

    // fill the printables list
    jobRef = new DatabaseObject<>(Job.class, companyKey, jobKey);

    listView.setAdapter(new PrintItemsAdapter(this, printables));
  }

  private void print(Signature signature) {
    final PrintHelper photoPrinter = new PrintHelper((thisActivity));
    photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
    ImageLoader loader = MyVolley.getImageLoader();

    loader.get(signature.getImageUrl(), new ImageLoader.ImageListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Utility.error(getRootView(), thisActivity, "Failed to load Signoff Image: " + error.getLocalizedMessage());
      }

      @Override
      public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
        photoPrinter.printBitmap("speedyPrinting", response.getBitmap());
      }
    });
  }


  private void updateFromJob(){
    printables.clear();
    // fill the printables list
    if (job.getSignatureLoadedForStorage() != null ){
      printables.add(Job.Lifecycle.LoadedForStorage);
    }
    if (job.getSignatureInStorage() != null){
      printables.add(Job.Lifecycle.InStorage);
    }
    if (job.getSignatureLoadedForDelivery() != null){
      printables.add(Job.Lifecycle.LoadedForDelivery);
    }
    if (job.getSignatureDelivered() != null) {
      printables.add(Job.Lifecycle.Delivered);
    }

    if (printables.isEmpty()){
      noPrintablesMessage.setVisibility(View.VISIBLE);
      listView.setVisibility(View.GONE);
    } else {
      noPrintablesMessage.setVisibility(View.GONE);
      listView.setVisibility(View.VISIBLE);
    }

    PrintItemsAdapter adapter = (PrintItemsAdapter) listView.getAdapter();
    adapter.notifyDataSetChanged();
  }

  @Override
  protected void onStart(){
    super.onStart();
    if (jobRef != null){
      jobRef.addValueEventListener(jobChangeListener);
    }

  }

  DatabaseObjectEventListener<Job> jobChangeListener = new DatabaseObjectEventListener<Job> () {

    @Override
    public void onChange(String key, Job modelJob) {
      job  = modelJob;
      updateFromJob();
      //jobRef.removeValueEventListener(jobChangeListener);
    }

  };


  @Override
  protected  void onStop(){
    super.onStop();
    jobRef.removeValueEventListener(jobChangeListener);
  }

  private class PrintItemsAdapter extends ArrayAdapter<Job.Lifecycle>{
    private LayoutInflater inflater;

    public PrintItemsAdapter(Context context, ArrayList<Job.Lifecycle> items){
      super(context, R.layout.print_list_item, items);
      inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View v, ViewGroup parent) {

      if (v == null){
        v = inflater.inflate(R.layout.print_list_item, null);
      }

      Drawable icon = ResourcesCompat.getDrawable(getResources(), R.drawable.new_active, null);
      String message= "";
      Job.Lifecycle lifecycle = getItem(position);
      switch (lifecycle) {
        case New:
          // this case shouldn't occur but handle
          icon = ResourcesCompat.getDrawable(getResources(), R.drawable.new_active, null);
          message = "Print \"New\" Signoff";
          break;
        case LoadedForStorage:
          icon = ResourcesCompat.getDrawable(getResources(), R.drawable.loaded_for_storage_active, null);
          message = "Print \"Loaded For Storage\" Signoff";
          break;
        case InStorage:
          icon = ResourcesCompat.getDrawable(getResources(), R.drawable.in_storage_active, null);
          message = "Print \"In Storage\" Signoff";
          break;
        case LoadedForDelivery:
          icon = ResourcesCompat.getDrawable(getResources(), R.drawable.loaded_for_delivery_active, null);
          message = "Print \"Loaded For Delivery\" Signoff";
          break;
        case Delivered:
          icon = ResourcesCompat.getDrawable(getResources(), R.drawable.delivered_active, null);
          message = "Print \"Delivered\" Signoff";
          break;
      }


      ImageView imageView = (ImageView) v.findViewById(R.id.lifecycleImage);
      imageView.setImageDrawable(icon);
      TextView label = (TextView) v.findViewById(R.id.tvLabel);
      label.setText(message);


      return v;
    }
  }

}
