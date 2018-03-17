
package com.speedymovinginventory.speedyinventory.database;


public interface DatabaseObjectEventListener< T>{
  void onChange(String key, T modelObject);
}

