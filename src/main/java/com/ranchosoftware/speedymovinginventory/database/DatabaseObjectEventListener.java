
package com.ranchosoftware.speedymovinginventory.database;


import com.ranchosoftware.speedymovinginventory.model.Model;


public interface DatabaseObjectEventListener< T>{
  void onChange(String key, T modelObject);
}

