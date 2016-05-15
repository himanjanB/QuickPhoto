package com.example.himanjan.quickphoto;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Himanjan on 13-05-2016.
 * <p/>
 * This class acts as a helper class to store the state information of the app in a Shared Preference. This is done
 * to fetch the latest image being viewed when the app was stopped and to store the total number of images available
 * in the device.
 */

public class ImageIndexCount {
    //Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    //Shared preference mode
    int PRIVATE_MODE = 0;

    //Shared Preferences Keys
    private static final String PREF_NAME = "QuickPhoto";
    private static final String CURRENT_INDEX = "current_index";
    private static final String CURRENT_NUMBER_OF_PHOTOS = "current_number_of_photos";


    /*
    An object of this class can be created by passing the context of the activity as an argument. Creating of this object creates the
    shared preference with name and proper mode.
     */
    public ImageIndexCount(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setIndex(int index) {
        editor.putInt(CURRENT_INDEX, index);
        editor.commit();
    }

    public int getIndex() {
        return pref.getInt(CURRENT_INDEX, 0);
    }

    public void setCurrentNumberOfPhotos(int count) {
        editor.putInt(CURRENT_NUMBER_OF_PHOTOS, count);
        editor.commit();
    }

    public int getCurrentNumberOfPhotos() {
        return pref.getInt(CURRENT_NUMBER_OF_PHOTOS, 0);
    }

}
