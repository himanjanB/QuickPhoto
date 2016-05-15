package com.example.himanjan.quickphoto;

import java.io.Serializable;

/**
 * Created by Himanjan on 13-05-2016.
 *
 * This class acts like a holder class for the image from the gallery.
 * Every image from the gallery is stored as an Image object.
 * This is done to store details like the folder name and the timestamp of the image. This could also hold any other info if
 * necessary. For now it can store the folder name, image URI and timestamp of the image.
 *
 */

public class Image implements Serializable {
    private String folderName;
    private String imageUri;
    private String timestamp;

    public Image() {
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}