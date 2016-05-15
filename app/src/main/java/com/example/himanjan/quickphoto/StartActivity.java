package com.example.himanjan.quickphoto;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;


public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = StartActivity.class.getSimpleName();
    private static final String CURRENT_INDEX = "Current_Index";
    private static final String SLIDESHOW_PAUSED = "Paused";
    private static final String SLIDESHOW_RESUMED = "Resumed";

    // imageUriList contains a list of Image objects. This list is used by the view flipper to set all the images as views.
    private ArrayList<Image> imageUriList;

    // viewFlipper is used in this app to show the slideshow.
    private ViewFlipper viewFlipper;

    // These two fields are used to store the folder name and timestamp of the image currently loaded.
    private TextView title, time;

    // currentIndex is used to store the current index of the image being showed and maxIndex is the number of photos available for one single run.
    private int currentIndex = 0;
    private int maxIndex = 0;

    // Progress dialog
    ProgressDialog dialog;

    // mHandler is used to run the runnable piece of code after the images are loaded.
    private final Handler mHandler = new Handler();

    // interval is the time in milliseconds before the next image is shown
    int interval = 3000;

    // Runnable object
    private Runnable mShowPhotos;

    // imageIndexCount is the variable reference to the class ImageIndexCount.
    private ImageIndexCount imageIndexCount;

    // isRunning is a boolean used to decide if the slideshow should continue or paused.
    private boolean isRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // index is used to store the total number of photos that were viewed.
        int index;

        //Initializing the variables..
        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        title = (TextView) findViewById(R.id.title);
        time = (TextView) findViewById(R.id.date);
        ImageButton pauseButton, playButton;
        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        playButton = (ImageButton) findViewById(R.id.playButton);

        //Since the screen needs to stay on during the slideshow, we need to handle this as below.
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageIndexCount = new ImageIndexCount(getApplicationContext());

        /*
            If the app is running for the first time the savedInstanceState is null and the value of index will be zero (0).
            If the app is running for the subsequent time, the value of index will be fetched from the Bundle. The value
            of index would have been saved to the Bundle object the last time onSaveInstance() was called.
         */
        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(CURRENT_INDEX, 0);
        } else {
            index = imageIndexCount.getIndex();
        }

        /*
            Calling the corresponding block based on the value of index. The value of index determines from where the images should be loaded.
            If in the first run four images were view, the value of index will be set to four which means the next time the app is started
            the images from index 5 will be loaded.

            Start an AsyncTask class to fetch the images stored in the phone memory. We pass an int value to determine from where the images
            should be loaded.
         */

        if (index > 0) {
            new ImageAsyncTask().execute(index);
        } else {
            new ImageAsyncTask().execute(0);
        }

        //Create runnable for slideshow
        mShowPhotos = new Runnable() {
            public void run() {
                if (isRunning) {
                    startPhotoSliding();
                }
            }
        };

        // delay is the time of first execution.
        int delay = 3000;

        //Creating a timer object to start the runnable block and start it with a pre defined delay and interval.
        Timer mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                mHandler.post(mShowPhotos);
            }
        }, delay, interval);

        //Setting onClickListener on the two buttons, play and pause.
        assert pauseButton != null;
        pauseButton.setOnClickListener(this);
        assert playButton != null;
        playButton.setOnClickListener(this);
    }

    // interval is set to 3000, so that every time the app starts the slideshow is started with a interval of 3 seconds.
    @Override
    protected void onStart() {
        super.onStart();
        interval = 3000;
    }

    /*
        isRunning is set to false here, so that the run method of the runnable object does not run. Also the isRunning is set to false to stop
        the slideshow. We also set the index value to the (currentIndex + the already saved index).
     */

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
        int savedIndex = imageIndexCount.getIndex();
        imageIndexCount.setIndex(currentIndex + savedIndex);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialog.dismiss();
    }

    /*
        We save the index value to the (currentIndex + the already saved index) to the Bundle object.
    */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        int savedIndex = imageIndexCount.getIndex();
        outState.putInt(CURRENT_INDEX, currentIndex + savedIndex);
        super.onSaveInstanceState(outState);
    }

    /*
        The onClick method handles the button clicks for pause and play buttons by setting the isRunning to false and true respectively.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pauseButton) {
            pauseSlideShow();
        } else if (v.getId() == R.id.playButton) {
            playSlideShow();
        }
    }

    private void playSlideShow() {
        isRunning = true;
        Toast.makeText(this, SLIDESHOW_RESUMED, Toast.LENGTH_SHORT).show();
    }

    private void pauseSlideShow() {
        isRunning = false;
        Toast.makeText(this, SLIDESHOW_PAUSED, Toast.LENGTH_SHORT).show();
    }

    //ImageAsyncTask is an Async task class to fetch all the images stored in the phone memory and set the viewFlipper.
    class ImageAsyncTask extends AsyncTask<Integer, Void, Boolean> {
        private final String LOADING_MESSAGE = "Loading, please wait";

        //To show a progress dialog before loading the images.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(StartActivity.this);
            dialog.setMessage(LOADING_MESSAGE);
            dialog.show();
            dialog.setCancelable(false);
        }

        //doInBackground takes one argument, i.e. index.
        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                int index = params[0];
                imageUriList = getFilePaths(index);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return imageUriList.size() != 0;
        }

        /*
            After the doInBackground method completes, onPostExecution is called. doInBackground method returns a boolean to this method.
            If the result is false, show a toast and close the app or else set the viewFlipper with the images.
        */

        protected void onPostExecute(Boolean result) {
            dialog.cancel();
            if (!result) {
                Toast.makeText(getApplicationContext(), "No photos available in device. Click a photo first.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                setFlipperImage(imageUriList);
            }
        }
    }


    private void setFlipperImage(ArrayList<Image> imageUriList) {
        viewFlipper.removeAllViews();

        /*
            Iterate the list and then set the viewFlipper with all the images. We are using a third party library called Glide which can
            be used smoothly for image loading. The caching and memory allocation is done internally by this library. To compile this library
            to the project include the dependencies to the build.gradle for the app. Add the following line.
            compile 'com.github.bumptech.glide:glide:3.7.0'

            We also set the isRunning to be true here since by now we know for sure that the viewFlipper contains images if any and it is
            safe to start the slideshow.
         */

        for (int i = 0; i <= imageUriList.size() - 1; i++) {
            ImageView image = new ImageView(getApplicationContext());

            if (imageUriList.get(i) != null) {
                Glide.with(getApplicationContext()).load(imageUriList.get(i).getImageUri())
                        .thumbnail(0.5f)
                        .crossFade()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(image);
            }
            viewFlipper.addView(image);
        }
        isRunning = true;
    }

    //Method to start slide show
    private void startPhotoSliding() {
        showNext();
    }

    public void showNext() {
        /*
            If all the images are not shown, the if block executes. It sets the TextViews with the folder name and timestamp of the images.
            We are also removing the already showed views from the viewFlipper in an attempt to minimize the memory usage. This is done in
            a try catch block to stop the app crash in case there is operation performed on a null object.

            The else block executes once all the images available in the phone are shown. It closes the app. It also sets the
            index value to zero so that the next time the app runs it starts from the beginning.
         */
        if (currentIndex < maxIndex - 1) {
            currentIndex++;
            viewFlipper.showNext();
            title.setText(imageUriList.get(currentIndex).getFolderName());
            time.setText(imageUriList.get(currentIndex).getTimestamp());
            try {
                viewFlipper.removeViewAt(viewFlipper.getChildCount() - 1);
            } catch (NullPointerException e) {
                Log.i(LOG_TAG, "Cannot remove the previous view");
            }
        } else {
            imageIndexCount.setIndex(0);
            currentIndex = 0;
            viewFlipper.removeAllViews();
            Toast.makeText(this, "All Photos showed.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /*
        This method is used to fetch all the image URIs available in the phone. It returns an ArrayList of Image objects.
     */
    public ArrayList<Image> getFilePaths(int index) {
        Uri u = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.ImageColumns.DATA};
        Cursor c = null;
        SortedSet<String> dirList = new TreeSet<>();

        String[] directories = null;
        if (u != null) {
            c = getContentResolver().query(u, projection, null, null, null);
        }

        if ((c != null) && (c.moveToFirst())) {
            do {
                String tempDir = c.getString(0);
                tempDir = tempDir.substring(0, tempDir.lastIndexOf("/"));
                try {
                    dirList.add(tempDir);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            while (c.moveToNext());
            directories = new String[dirList.size()];
            dirList.toArray(directories);
        }

        if (c != null) {
            c.close();
        }

        Uri uri;
        long date = 0L;
        ArrayList<Long> dateArrayList = new ArrayList<>();
        Map<Long, Image> imageMap = new HashMap<>();

        File[] imageList = null;
        for (int i = 0; i < dirList.size(); i++) {
            File imageDir;
            if (directories != null) {
                imageDir = new File(directories[i]);
                imageList = imageDir.listFiles();
            }

            if (imageList == null)
                continue;
            for (File imagePath : imageList) {
                Image image = new Image();
                try {
                    if (imagePath.getName().contains(".jpg") || imagePath.getName().contains(".JPG")
                            || imagePath.getName().contains(".jpeg") || imagePath.getName().contains(".JPEG")
                            || imagePath.getName().contains(".png") || imagePath.getName().contains(".PNG")
                            || imagePath.getName().contains(".gif") || imagePath.getName().contains(".GIF")
                            || imagePath.getName().contains(".bmp") || imagePath.getName().contains(".BMP")
                            ) {

                        if (imagePath.exists()) {
                            date = imagePath.lastModified();
                            dateArrayList.add(date);
                            Date formatDate = new Date(date);
                            image.setTimestamp(formatDate + "");
                        }
                        String path = imagePath.getAbsolutePath();
                        uri = Uri.parse("file://" + path);
                        image.setImageUri(uri.toString());

                        String imageFolder = uri.toString().substring(0, uri.toString().lastIndexOf('/'));
                        String[] folderArray = imageFolder.split("/");
                        image.setFolderName("/" + folderArray[folderArray.length - 1]);
                        imageMap.put(date, image);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Collections.sort(dateArrayList);
        ArrayList<Image> imagesList = new ArrayList<>();

        int currentNumberOfPhotos = imageIndexCount.getCurrentNumberOfPhotos();

        /*
            If the app is running for the first time, the currentNumberOfPhotos is zero (0). We add all the images to the list and
            set the maxIndex to the number of images available in the phone and return the imagesList.

            If the app is running for subsequent times, we check if the number of images available are same as the number of images
            that were available before the app was exited. If it is same, we load the images from where the app last exited. For ex. If the
            app was exited at the image number 5, this time the ArrayList will contain images from index 6 till end.

            Else if the app is running for subsequent times and the number of images available is more than the number of images that were
            available before the app was exited, we add the new images to the ArrayList and then add all the other images from where the
            user had exited the app.

            There can be one more condition where some images were deleted between two consecutive runs. In this case, I am showing all the
            images from the start.
         */

        if (currentNumberOfPhotos == 0) {
            for (int i = dateArrayList.size() - 1; i >= 0; i--) {
                imagesList.add(imageMap.get(dateArrayList.get(i)));
            }
        } else {
            if (currentNumberOfPhotos == dateArrayList.size()) {
                for (int i = dateArrayList.size() - index - 1; i >= 0; i--) {
                    imagesList.add(imageMap.get(dateArrayList.get(i)));
                }
            } else if (currentNumberOfPhotos < dateArrayList.size()) {
                int difference = dateArrayList.size() - currentNumberOfPhotos;
                for (int i = dateArrayList.size() - 1; i > dateArrayList.size() - difference - 1; i--) {
                    imagesList.add(imageMap.get(dateArrayList.get(i)));
                }
                for (int i = dateArrayList.size() - difference - index - 1; i >= 0; i--) {
                    imagesList.add(imageMap.get(dateArrayList.get(i)));
                }
            } else if (currentNumberOfPhotos > dateArrayList.size()) {
                for (int i = dateArrayList.size() - 1; i >= 0; i--) {
                    imagesList.add(imageMap.get(dateArrayList.get(i)));
                }
            }
        }
        imageIndexCount.setCurrentNumberOfPhotos(dateArrayList.size());
        maxIndex = imagesList.size();
        return imagesList;
    }
}