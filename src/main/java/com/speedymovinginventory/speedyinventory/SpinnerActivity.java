package com.speedymovinginventory.speedyinventory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SpinnerActivity extends BaseActivity {

  private ListView listView;

  private String labels[];
  private int selectedIndex = -1; // call provides

  public static final String paramSelectedIndex = "selectedIndex";
  public static final String paramLabels = "labels";
  public static final String paramTitle = "title";



  public static Intent getLaunchIntent(Context context, int initialPosition, String labels[],
                                       String title){
    Intent intent = new Intent(context, SpinnerActivity.class);
    Bundle b = new Bundle();
    b.putInt(SpinnerActivity.paramSelectedIndex,  initialPosition);
    b.putStringArray(SpinnerActivity.paramLabels, labels);
    b.putString(SpinnerActivity.paramTitle, "Choose A Sort");
    intent.putExtras(b);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_spinner);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    Bundle params = getIntent().getExtras();
    String title = params.getString(paramTitle);
    toolbar.setTitle(title);
    setSupportActionBar(toolbar);

    listView = (ListView) findViewById(R.id.listView);


    selectedIndex = params.getInt(paramSelectedIndex);
    labels = params.getStringArray(paramLabels);



    ListAdapter adapter = new ListAdapter(this, R.layout.spinner_list_item, labels);
    listView.setAdapter(adapter);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        selectedIndex = position;
        ListAdapter adapter = (ListAdapter) listView.getAdapter();
        adapter.notifyDataSetChanged();

        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Intent returnValue = new Intent();
            returnValue.putExtra(paramSelectedIndex, selectedIndex);
            setResult(RESULT_OK, returnValue);
            finish();
            overridePendingTransition(R.xml.slide_in_from_left, R.xml.slide_out_to_right);
          }
        }, 100);
      }
    });

  }

  private class ListAdapter extends ArrayAdapter<String> {

    public ListAdapter(Context context, int resource, String[] items) {
      super(context, resource, items);
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      if (v == null) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        v = inflater.inflate(R.layout.spinner_list_item, null);
      }

      TextView label = (TextView) v.findViewById(R.id.tvLabel);
      ImageView selectedImage = (ImageView) v.findViewById(R.id.ivSelected);

      if (selectedIndex == position){
        selectedImage.setImageDrawable(getDrawable(R.drawable.spinner_selected));
      } else {
        selectedImage.setImageDrawable(getDrawable(R.drawable.spinner_not_selected));
      }
      label.setText(getItem(position));

      return v;
    }

  }

}
