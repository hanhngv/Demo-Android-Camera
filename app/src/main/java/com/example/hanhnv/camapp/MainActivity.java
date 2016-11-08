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
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import static android.R.attr.subMenuArrow;
import static android.R.attr.width;
import static android.hardware.Camera.getNumberOfCameras;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import static android.R.attr.id;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import android.widget.Toast;

import com.hanhnv.JNI2;

public class MainActivity extends AppCompatActivity {
//    static {
//        //System.loadLibrary("native-lib");
//        System.loadLibrary("NativeHanhNV");
//    }

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
    protected boolean m_cur_rorate = true;

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
        Log.d(TAG, "Activity event: onCreate");

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

//        FrameLayout preview_layout = (FrameLayout)findViewById(R.id.camera_preview);
//        LinearLayout.LayoutParams params_preview = (LinearLayout.LayoutParams)preview_layout.getLayoutParams();
//
//        FrameLayout result_layout = (FrameLayout)findViewById(R.id.result_frame);
//        LinearLayout.LayoutParams params_result = (LinearLayout.LayoutParams) result_layout.getLayoutParams();
//
//        params_result.width = params_preview.width;
//        params_result.height = params_preview.height;
//
//        result_layout.setLayoutParams(params_result);

        //.join()
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        Log.d(TAG, "Activity event: onDestroy");
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.d(TAG, "Activity event: onStart");

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeAllViews();
        preview.addView(mPreview);

        mResult = new CameraResult(this);
        FrameLayout preview_result = (FrameLayout)findViewById(R.id.result_frame);
        preview_result.removeAllViews();
        preview_result.addView(mResult);


        m_status_thread = new StatusThread(this);
        m_status_thread.start();

        if(mCamera == null) {
            mCamera = getCameraInstance();
            mPreview.mCamera = mCamera;
        }
        mPreview.mCamera = mCamera;
        if(!mPreview.m_be_set_size) {
            Camera.Parameters parameters = mCamera.getParameters();

            parameters.setPreviewSize(640, 480);
            parameters.setPreviewFrameRate(30);
            //parameters.setPreviewFormat(ImageFormat.RGB_565);
            mCamera.setParameters(parameters);
            mPreview.m_be_set_size = true;

            parameters = mCamera.getParameters();
            mPreview.m_frame_size = parameters.getPreviewSize();
        }

        mCamera.setPreviewCallbackWithBuffer(mPreview.mPreviewCallback);
        for(int i = 0; i < 3; i++) {
            mPreview.m_buffer = new byte[460800];
            mCamera.addCallbackBuffer(mPreview.m_buffer);
        }
        try {
            mCamera.setPreviewDisplay(mPreview.mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        mPreview.m_processThread = new ProcessThread(mPreview, this);
        mPreview.m_processThread.start();

        //m_status_thread = new StatusThread(this);
        //m_status_thread.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d(TAG, "Activity event: onStop");

        if(mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;

                m_running = false;

                //m_status_thread.join();
                mPreview.m_processThread.join();

                JNI2.releaseBuffer();
            }
            catch (Exception e){
            }
        }
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        Log.d(TAG, "Activity event: onRestart");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "Activity event: onResume");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "Activity event: onPause");
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

    public synchronized void updateViewSize(final boolean rotate){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(rotate == m_cur_rorate)
                    return;

                FrameLayout preview_layout = (FrameLayout)findViewById(R.id.camera_preview);

                FrameLayout result_layout = (FrameLayout)findViewById(R.id.result_frame);
                LinearLayout.LayoutParams params_result = (LinearLayout.LayoutParams) result_layout.getLayoutParams();

                if(rotate){
                    params_result.width = preview_layout.getHeight();
                    params_result.height = preview_layout.getWidth();
                    params_result.gravity = Gravity.CENTER;
                }
                else {
                    params_result.width = preview_layout.getWidth();
                    params_result.height = preview_layout.getHeight();
                    params_result.gravity = Gravity.BOTTOM;
                }

                m_cur_rorate = rotate;

                result_layout.setLayoutParams(params_result);
            }
        });
    }

    public synchronized void setStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_ctrl_status.setText(status);
        }
        });
    }

    public synchronized void setResult(final Bitmap bmp){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(bmp != null){
                    if(bmp.getHeight() > bmp.getWidth())
                        updateViewSize(true);
                    else
                        updateViewSize(false);

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
