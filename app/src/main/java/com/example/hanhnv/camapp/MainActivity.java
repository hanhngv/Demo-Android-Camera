package com.example.hanhnv.camapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.hardware.Camera;
import static android.hardware.Camera.getNumberOfCameras;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import static android.R.attr.id;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import android.widget.Toast;

import com.hanhnv.JNI2;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
//    static {
//        //System.loadLibrary("native-lib");
//        System.loadLibrary("NativeHanhNV");
//    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();
//    public native int square(int edge);

    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public final int m_release_statis_time = 2000;
    public final int m_max_statis_object = 20;

    protected int m_time_process = 0;
    protected int m_time_draw = 0;
    protected float m_capture_fps = 0;
    protected float m_show_fps = 0;
    protected int m_num_capture = 0;
    protected int m_num_cur_capture = 0;
    protected int m_num_frame_cur_process = 0;
    protected int m_num_frame_cur_draw = 0;
    protected MMeasureTime m_time_begin_measure_capture;
    protected MMeasureTime m_time_begin_measure_process;
    protected MMeasureTime m_time_begin_measure_draw;
    protected String m_cur_status;

    protected List<Integer> m_list_time_convert;
    protected List<Integer> m_list_time_bitmap;
    protected List<Integer> m_list_time_process;

    protected float m_avg_time_convert = 0;
    protected float m_avg_time_bitmap = 0;
    protected float m_avg_time_process = 0;

    public boolean m_running = false;

    private static final String TAG = MainActivity.class.getSimpleName();

    public Camera mCamera;
    private CameraPreview mPreview;
    private CameraResult mResult;

    static File imgFile = null;
    static Context context;

    protected TextView m_ctrl_status;
    protected ImageView m_ctrl_result;

    private Lock m_lock_status;
    StatusThread m_status_thread;

    JNI2 m_native_lib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_cur_status = getString(R.string.defaut_status);
        m_ctrl_status = (TextView)findViewById(R.id.status_text);
        m_ctrl_result = (ImageView)findViewById(R.id.result_view);
        m_lock_status = new ReentrantLock();
        m_native_lib = new JNI2();
        m_running = true;

        if(checkCameraHardware(this))
            m_cur_status = "Ready (" + getNumberOfCameras() + ")";
        else
            m_cur_status = "Not ready";

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        setStatus(m_cur_status);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        m_list_time_convert = new ArrayList<Integer>();
        m_list_time_bitmap = new ArrayList<Integer>();
        m_list_time_process = new ArrayList<Integer>();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mResult = new CameraResult(this);
        FrameLayout preview_result = (FrameLayout)findViewById(R.id.result_frame);
        preview_result.addView(mResult);

        m_time_begin_measure_capture = new MMeasureTime();
        m_time_begin_measure_process = new MMeasureTime();
        m_time_begin_measure_draw = new MMeasureTime();

        m_status_thread = new StatusThread(this);
        m_status_thread.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;

//                while (m_status_thread.isAlive())
//                {
//                    Thread.sleep(100);
//                }
                m_running = false;
//                if(m_status_thread.isAlive())
//                    m_status_thread.interrupt();
                //if(m_status_thread.isAlive())
                    //m_status_thread.stop();
            }
            catch (Exception e){

            }
        }
    }

    public void captureOnClick(View view){
        Log.d(TAG, "Click capture button");

        CaptureThread captureThread = new CaptureThread(this);
        captureThread.start();
    }
    public void flipOnClick(View view){
        synchronized (mPreview.m_cur_process_state){
            if(mPreview.m_cur_process_state == PROCESSING_CODE.Flip_Vertical){
                mPreview.m_cur_process_state = PROCESSING_CODE.Flip_Horizontal;
            }
            else{
                mPreview.m_cur_process_state = PROCESSING_CODE.Flip_Vertical;
            }
        }
    }
    public void rotateOnClick(View view){
        synchronized (mPreview.m_cur_process_state){
            if(mPreview.m_cur_process_state == PROCESSING_CODE.Rotate_right)
                mPreview.m_cur_process_state = PROCESSING_CODE.None;
            else if(mPreview.m_cur_process_state == PROCESSING_CODE.Rotate_down)
                mPreview.m_cur_process_state = PROCESSING_CODE.Rotate_right;
            else if(mPreview.m_cur_process_state == PROCESSING_CODE.Rotate_left)
                mPreview.m_cur_process_state = PROCESSING_CODE.Rotate_down;
            else
                mPreview.m_cur_process_state = PROCESSING_CODE.Rotate_left;
        }
    }

    public synchronized void setStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_ctrl_status.setText(status);
            }
        });
    }

    public synchronized void setResultView(final Bitmap bitmap){
           runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_ctrl_result.setImageBitmap(bitmap);

                status_increaseNumDraw();
            }
        });
    }

    public synchronized void setResult(final Bitmap bmp, final int frame_width, final int frame_heigth){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(bmp != null){
                    Canvas canvas = mResult.mHolder.lockCanvas();
                    if(canvas != null){
                        canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, canvas.getWidth(), canvas.getHeight(), false), 0, 0, null);
                        mResult.mHolder.unlockCanvasAndPost(canvas);
                    }
                    status_increaseNumDraw();
                }
            }
        });
    }

    public PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");// +
                        //e.getMessage());
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                m_num_capture++;
                setStatus("Num capture: " + m_native_lib.square(m_num_capture));

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /// ------------------------------------------------------------------------------------------
    /// -----------------------------------------  STATUS ----------------------------------------
    /// ------------------------------------------------------------------------------------------
    public synchronized void status_updateNumcapture(int num_capture){
        m_num_capture = num_capture;
    }
    public synchronized void status_updateTimeProcess(int time_process){
        m_time_process = time_process;
    }
    public synchronized void status_updateTimeDraw(int time_draw){
        m_time_draw = time_draw;
    }
    public synchronized void status_updateCaptureFps(float capture_fps){m_capture_fps = capture_fps; }
    public synchronized void status_updateDrawFps(float draw_fps){
        m_show_fps = draw_fps;
    }
    public synchronized void status_resetNumCapture(){
        m_num_capture = 0;
    }
    public synchronized void status_resetNumProcess(){
        m_num_frame_cur_process = 0;
    }
    public synchronized void status_resetNumDraw(){
        m_num_frame_cur_draw = 0;
    }
    public synchronized void status_increaseNumCapture(){
        m_num_capture++;
    }
    public synchronized void status_increaseNumProcess(){
        m_num_frame_cur_process++;
    }
    public synchronized void status_increaseNumDraw(){
        m_num_frame_cur_draw++;
    }
    public synchronized void status_addTimeConvert(int new_time){m_list_time_convert.add(new_time);}
    public synchronized void status_addTimeBitmap(int new_time){m_list_time_bitmap.add(new_time);}
    public synchronized void status_addTimeProcess(int new_time){m_list_time_process.add(new_time);}

    public synchronized int status_getNumcapture(){
        return m_num_capture;
    }
    public synchronized int status_getNumDraw(){
        return m_num_frame_cur_draw;
    }
    public synchronized int status_getTimeProcess(){
        return m_time_process;
    }
    public synchronized int status_getTimeDraw(){
        return m_time_draw;
    }
    public synchronized float status_getCaptureFps(){
        return m_capture_fps;
    }
    public synchronized float status_getDrawFps(){
        return m_show_fps;
    }
    public synchronized float status_updateTimeConvert(){
        float avg = 0;

        synchronized (m_list_time_convert) {
            while (m_list_time_convert.size() > m_max_statis_object)
                m_list_time_convert.remove(0);

            for(int i = 0; i < m_list_time_convert.size(); i++){
                avg += m_list_time_convert.get(i);
            }
        }

        avg /= m_max_statis_object;

        return avg;
    }
    public synchronized float status_updateTimeBitmap(){
        float avg = 0;

        synchronized (m_list_time_bitmap) {
            while (m_list_time_bitmap.size() > m_max_statis_object)
                m_list_time_bitmap.remove(0);

            for(int i = 0; i < m_list_time_bitmap.size(); i++){
                avg += m_list_time_bitmap.get(i);
            }
        }

        avg /= m_max_statis_object;

        return avg;
    }
    public synchronized float status_updateTimeProcess(){
        float avg = 0;

        synchronized (m_list_time_process) {
            while (m_list_time_process.size() > m_max_statis_object)
                m_list_time_process.remove(0);

            for(int i = 0; i < m_list_time_process.size(); i++){
                avg += m_list_time_process.get(i);
            }
        }

        avg /= m_max_statis_object;

        return avg;
    }

    private static File getOutputMediaFile(int type){
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(!isSDPresent)
        {
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, "card not mounted", duration);
            toast.show();

            Log.d("ERROR", "Card not mounted");
        }
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath() + "/cameraSpeed/");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){

                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int mili = calendar.get(Calendar.MILLISECOND);

        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String timeStamp = year +""+ month +""+ day + "_" + hour +""+ min +""+ second + "_" + mili;
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
            imgFile = mediaFile;
        } else {
            return null;
        }

        return mediaFile;
    }

    // =========================================================
    // =========================== CAMERA ======================
    // =========================================================
    private boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;
        else
            return false;
    }

    //public void detectCamera(View view){
//        TextView text_view = (TextView)findViewById(R.id.sample_text);
//        if(checkCameraHardware(this))
//            text_view.setText("Ready (" + getNumberOfCameras() + ")");
//        else
//            text_view.setText("Not ready");

        //Intent intent = new Intent(this, CameraActivity.class);
        //intent.putExtra(EXTRA_MESSAGE, "start");
        //startActivity(intent);
    //}

    public static Camera getCameraInstance(){
        Camera cam = null;
        try{
            cam = Camera.open();
        }
        catch (Exception e){
        }

        return cam;
    }
}
//
//class TimeStatis{
//  public
//};

class CaptureThread extends Thread{
    Context m_parent;

    CaptureThread(Context context){
        m_parent = context;
    }

    public void run(){
        MainActivity parent = (MainActivity)m_parent;
        Thread.currentThread().getName();
        parent.setStatus("Begin capture!");

        MMeasureTime measure_time_begin = new MMeasureTime();

        int count = 0;
        while(count < 5) {
            if(measure_time_begin.untilNow() > 2000){
                // get an image from the camera
                parent.mCamera.takePicture(null, null, parent.mPicture);
                count++;

                measure_time_begin.update();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class StatusThread extends Thread{
    Context m_parent;

    StatusThread(Context context){
        m_parent = context;
    }

    String TAG = this.getClass().getSimpleName();

    public void run(){
        MainActivity parent = (MainActivity)m_parent;
        Thread.currentThread().getName();

        NumberFormat double_format = new DecimalFormat("#0.00");

        parent.m_time_begin_measure_capture.update();
        parent.m_time_begin_measure_process.update();
        parent.m_time_begin_measure_draw.update();

        while(((MainActivity) m_parent).m_running == true) {
            // Capture
            int time_measure = parent.m_time_begin_measure_capture.untilNow();
            if(time_measure > 1000){
                int num_cur_capture = parent.status_getNumcapture();
                parent.status_updateCaptureFps(1000 * num_cur_capture / (float)time_measure);

                parent.m_time_begin_measure_capture.update();
                parent.status_resetNumCapture();
            }

            // Process
//            int time_measure = parent.m_time_begin_measure_process.untilNow();
//            if(time_measure > 1000){
//                parent.status_updateCaptureFps(1000 * parent.status_getNumcapture() / (float)time_measure);
//
//                parent.m_time_begin_measure_process.update();
//                parent.status_resetNumProcess();
//            }

            // Show
            time_measure = parent.m_time_begin_measure_draw.untilNow();
            if(time_measure > 1000){
                int num_cur_draw = parent.status_getNumDraw();
                Log.d(TAG, "Num draw: " + num_cur_draw);
                parent.status_updateDrawFps(1000 * num_cur_draw / (float)time_measure);

                parent.m_time_begin_measure_draw.update();
                parent.status_resetNumDraw();
            }

            String status = new String();
            status += "Capture: " + double_format.format(parent.status_getCaptureFps()) + "  ";
            status += "Draw: " + double_format.format(parent.status_getDrawFps()) + "  ";
            status += "Convert: " + double_format.format(parent.status_updateTimeConvert()) + "  ";
            status += "Process: " + double_format.format(parent.status_updateTimeProcess()) + "  ";
            status += "Render: " + double_format.format(parent.status_updateTimeBitmap()) + "  ";

            parent.setStatus(status);

            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
