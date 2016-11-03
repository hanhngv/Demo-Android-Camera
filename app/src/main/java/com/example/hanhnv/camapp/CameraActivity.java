//package com.example.hanhnv.camapp;
//
//import android.app.Activity;
//import android.content.Context;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.os.Environment;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.support.v7.app.AppCompatActivity;
//
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//import static com.example.hanhnv.camapp.MainActivity.getCameraInstance;
//
///**
// * Created by hanhnv on 17/10/2016.
// */
//
//public class CameraActivity extends Activity {
//    private static final String TAG = "Camera activity";
//    private Camera mCamera;
//    private CameraPreview mPreview;
//
//    static File imgFile = null;
//    static Context context;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.activity_main);
//
//        TextView text_view = (TextView)findViewById(R.id.sample_text);
//        text_view.setText("Camera acitivy");
//
//
//        // Create an instance of Camera
//        mCamera = getCameraInstance();
//
//        // Create our Preview view and set it as the content of our activity.
//        mPreview = new CameraPreview(this, mCamera);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(mPreview);
//
//        Button captureButton = (Button) findViewById(R.id.button_capture);
//        captureButton.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        // get an image from the camera
//                        mCamera.takePicture(null, null, mPicture);
//                        //mCamera.release();
//                    }
//                }
//        );
//    }
//
//    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//
//            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
//            if (pictureFile == null) {
//                Log.d(TAG, "Error creating media file, check storage permissions: ");// +
//                //e.getMessage());
//                return;return
//            }
//
//            try {
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//            } catch (FileNotFoundException e) {
//                Log.d(TAG, "File not found: " + e.getMessage());
//            } catch (IOException e) {
//                Log.d(TAG, "Error accessing file: " + e.getMessage());
//            }
//
//        }
//    };
//
//    private static File getOutputMediaFile(int type){
//
//        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
//        if(!isSDPresent)
//        {
//            int duration = Toast.LENGTH_LONG;
//
//            Toast toast = Toast.makeText(context, "card not mounted", duration);
//            toast.show();
//
//            Log.d("ERROR", "Card not mounted");
//        }
//        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath() + "/cameraSpeed/");
//
//        if (! mediaStorageDir.exists()){
//            if (! mediaStorageDir.mkdirs()){
//
//                Log.d("MyCameraApp", "failed to create directory");
//                return null;
//            }
//        }
//
//        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        File mediaFile;
//        if (type == MEDIA_TYPE_IMAGE){
//            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
//                    "IMG_"+ timeStamp + ".jpg");
//            imgFile = mediaFile;
//        } else {
//            return null;
//        }
//
//        return mediaFile;
//    }
//
//}
