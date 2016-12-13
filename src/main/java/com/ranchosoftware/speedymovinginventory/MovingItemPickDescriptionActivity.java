package com.ranchosoftware.speedymovinginventory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ranchosoftware.speedymovinginventory.app.RanchoApp;
import com.ranchosoftware.speedymovinginventory.model.MovingItemDataDescription;

import java.util.ArrayList;
import java.util.List;

public class MovingItemPickDescriptionActivity extends BaseActivity {

  private EditText filterEdit;
  private Button changeRoomButton;
  private ListView itemList;
  private Button cancel;

  private List<MovingItemDataDescription> originalItemList;
  private List<MovingItemDataDescription> filteredItemList;

  private MovingItemDataDescription.Room room;
  private boolean allowCancel;


  private int selectedIndex = -1; // neg is no selection

  private int lastFilterLength = 0;

  private void filter(List<MovingItemDataDescription> list, CharSequence filter){
    filteredItemList.clear();

    for (MovingItemDataDescription item : list){
      String lowerCaseFilter = filter.toString().toLowerCase();
      if (item.getItemName().toLowerCase().contains(lowerCaseFilter)){
        filteredItemList.add(item);
      }
    }

  }

  private void filter(CharSequence charSequence){
    List<MovingItemDataDescription> copy;
    if (charSequence.length() > lastFilterLength){
       copy = new ArrayList<MovingItemDataDescription>(filteredItemList);
    } else {
       copy =  new ArrayList<MovingItemDataDescription>(originalItemList);
    }
    lastFilterLength = charSequence.length();
    filter(copy, charSequence);
    MovingItemDescriptionAdapter adapter = (MovingItemDescriptionAdapter) itemList.getAdapter();
    adapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_moving_item_pick_description);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    String roomString = getIntent().getExtras().getString("room");
    room = MovingItemDataDescription.Room.valueOf(roomString);
    allowCancel  = getIntent().getExtras().getBoolean("allowCancel");

    filterEdit = (EditText) findViewById(R.id.editFilter);
    changeRoomButton = (Button) findViewById(R.id.changeRoomButton);
    changeRoomButton.setText(room + " >");
    changeRoomButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        launchChooseRoomSpinner();
      }
    });
    itemList = (ListView) findViewById(R.id.itemList);
    cancel = (Button) findViewById(R.id.cancelButton);
    List<MovingItemDataDescription> list = app().getListFor(room);

    originalItemList = new ArrayList<MovingItemDataDescription>(list);
    filteredItemList = new ArrayList<MovingItemDataDescription>(app().getListFor(room));


    if (!allowCancel){
      cancel.setVisibility(View.GONE);
    }
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
      }
    });

    itemList.setAdapter(new MovingItemDescriptionAdapter(this, filteredItemList));

    itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        selectedIndex = i;
        final MovingItemDataDescription item = filteredItemList.get(selectedIndex);
        ((MovingItemDescriptionAdapter)itemList.getAdapter()).notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            setResult(Activity.RESULT_OK);
            RanchoApp app = thisActivity.app();
            float weight = item.getCubicFeet() * app.getCompanyPoundsPerCubicFoot();
            Intent returnValue = new Intent();
            returnValue.putExtra("itemName",item.getItemName() );
            returnValue.putExtra("weightLbs", weight);
            returnValue.putExtra("cubicFeet", item.getCubicFeet());
            returnValue.putExtra("isBox", item.getIsBox());
            returnValue.putExtra("boxSize", item.getBoxSize());
            returnValue.putExtra("speciaInstructions", item.getSpecialInstructions());
            setResult(RESULT_OK, returnValue);
            finish();
            overridePendingTransition(R.xml.slide_in_from_left, R.xml.slide_out_to_right);
          }
        }, 100);
      }
    });

    filterEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        filter(charSequence);
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });

  }

  @Override
  public void onBackPressed() {
    if (allowCancel) {
      super.onBackPressed();
    }
  }

  private static final int  PICK_ROOM = 465;

  private int indexOf(MovingItemDataDescription.Room room){
    int index = 0;
    for (MovingItemDataDescription.Room r : MovingItemDataDescription.Room.values()){
      if (room.equals(r)){
        return index;
      }
      index = index + 1;
    }

    // shouldn't get to here
    return 0;
  }

  private void launchChooseRoomSpinner(){
    Intent intent = new Intent(this, SpinnerActivity.class);
    Bundle b = new Bundle();

    String names[] = new String[MovingItemDataDescription.Room.values().length];
    int i = 0;
    for (MovingItemDataDescription.Room r : MovingItemDataDescription.Room.values()){
      names[i] = r.name();
      i = i +1;
    }

    int index = indexOf(room);
    b.putInt(SpinnerActivity.paramSelectedIndex,index);
    b.putStringArray(SpinnerActivity.paramLabels, names);
    b.putString(SpinnerActivity.paramTitle, "Choose A Room");
    intent.putExtras(b);

    startActivityForResult(intent, PICK_ROOM);
    overridePendingTransition(R.xml.slide_in_from_right,R.xml.slide_out_to_left);
  }

  private class MovingItemDescriptionAdapter extends ArrayAdapter<MovingItemDataDescription> {
    private LayoutInflater inflater;

    public MovingItemDescriptionAdapter(Context context, List<MovingItemDataDescription> items){
      super(context, R.layout.moving_item_description_item, items);
      inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View v, ViewGroup parent) {
      if (v == null){
        v = inflater.inflate(R.layout.moving_item_description_item, null);
      }


      TextView label = (TextView) v.findViewById(R.id.tvLabel);
      ImageView selectedImage = (ImageView) v.findViewById(R.id.ivSelected);

      if (selectedIndex == position){
        selectedImage.setImageDrawable(getDrawable(R.drawable.spinner_selected));
      } else {
        selectedImage.setImageDrawable(getDrawable(R.drawable.spinner_not_selected));
      }

      MovingItemDataDescription desc = getItem(position);
      label.setText(desc.getItemName());

      return v;

    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to
   if (requestCode == PICK_ROOM){
      if (resultCode == Activity.RESULT_OK){
        // extract selected index
        int selectedIndex = data.getIntExtra(SpinnerActivity.paramSelectedIndex, 0);
        this.room = MovingItemDataDescription.Room.values()[selectedIndex];
        changeRoomButton.setText(room + " >");

        originalItemList = new ArrayList<MovingItemDataDescription>(app().getListFor(room));
        filteredItemList = new ArrayList<MovingItemDataDescription>(app().getListFor(room));

        itemList.setAdapter(new MovingItemDescriptionAdapter(this, filteredItemList ));

      }
    }
  }


}
