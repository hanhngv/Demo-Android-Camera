package com.example.hanhnv.camapp;

/**
 * Created by hanhnv on 17/10/2016.
 */


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hanhnv.JNI2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.R.attr.data;
import static com.example.hanhnv.camapp.PROCESSING_CODE.Encode;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = CameraPreview.class.getSimpleName();
    public SurfaceHolder mHolder;
    public Camera mCamera;
    Context m_parent;
    boolean m_be_set_size;
    byte[] m_buffer;
    ByteBuffer m_capture_buff;
    boolean m_is_has_wait_frame = false;
    Bitmap m_result_frame;
    boolean m_is_has_result_frame = false;
    Camera.Size m_frame_size;
//    synchronized boolean isLocked;
//
//    private void setLocked() {
//        isLocked = true;
//    }

    PROCESSING_CODE m_cur_process_state = PROCESSING_CODE.None;            ;
    PROCESSING_MODE m_cur_color_mode = PROCESSING_MODE.YUV;

    ProcessThread m_processThread;

    public boolean m_running = false;
    public boolean m_recording = false;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        m_parent = context;
        mCamera = camera;

        m_be_set_size = false;
        m_frame_size = null;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //mHolder.setFixedSize(500, 500);

        m_running = true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            Log.d(TAG, "surface created ...");
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public synchronized void setCapturedFlag(boolean flag){
        m_is_has_wait_frame = flag;
    }
    public synchronized boolean getCaptureFlag(){
        return m_is_has_wait_frame;
    }

    public Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            ((MainActivity)m_parent).status_increaseNumCapture();

            if(m_capture_buff == null){
                m_capture_buff = ByteBuffer.allocate(data.length);
            }
            synchronized (m_capture_buff) {
                System.arraycopy(data, 0, m_capture_buff.array(), 0, data.length);
                setCapturedFlag(true);
            }

            mCamera.addCallbackBuffer(m_buffer);
        }
    };

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface event: surface destroyed ...");

        mCamera.stopPreview();

        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        ((MainActivity)m_parent).mCamera = null;

        m_running = false;
        try {
            m_processThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(mCamera == null)
            return;
        Log.d(TAG, "Surface event: surface changed ...");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            return;
        }

        // stop preview before making changes
        try {
            //mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}

class EncodeThread extends Thread{
    MainActivity m_draw_activity;
    MMeasureTime m_time_begin_record = new MMeasureTime();
    public void run(){

    }
}

class ProcessThread extends Thread{
    CameraPreview m_parent;
    MainActivity m_draw_activity;
    IntBuffer m_rgba_buf = IntBuffer.allocate(0);
    ByteBuffer m_yuv_buf = ByteBuffer.allocate(0);

    Bitmap m_result_landscape = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
    Bitmap m_result_portrait = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);

    ProcessThread(CameraPreview context, MainActivity draw_activity){
        m_parent = context;
        m_draw_activity = draw_activity;
    }

    public void run() {
        MMeasureTime measure_time_begin = new MMeasureTime();
        boolean is_init_buffer = false;

        boolean is_init_buffer_tmp = false;
        int width = -1;
        int height = -1;

        String TAG = this.getClass().getSimpleName();
        PROCESSING_CODE cur_state;

        while (m_parent.m_running) {
            if (measure_time_begin.untilNow() > 5) {

                if (m_parent.getCaptureFlag()) {
                    m_parent.setCapturedFlag(false);

                    if (is_init_buffer_tmp == false) {
                        m_yuv_buf = ByteBuffer.allocate(m_parent.m_capture_buff.array().length);
                        is_init_buffer_tmp = true;
                    }

                    synchronized (m_parent.m_capture_buff) {
                        System.arraycopy(m_parent.m_capture_buff.array(), 0, m_yuv_buf.array(), 0, m_yuv_buf.array().length);
                    }
                    m_parent.setCapturedFlag(false);


                    if (is_init_buffer == false) {
                        m_rgba_buf = IntBuffer.allocate((int) (m_parent.m_capture_buff.array().length / 1.5));
                        is_init_buffer = true;
                    }

                    if (width == -1) {
                        width = m_parent.m_frame_size.width;
                        height = m_parent.m_frame_size.height;

                        m_result_landscape = Bitmap.createBitmap(m_rgba_buf.array(), width, height, Bitmap.Config.ARGB_8888);
                        m_result_portrait = Bitmap.createBitmap(m_rgba_buf.array(), height, width, Bitmap.Config.ARGB_8888);
                    }

                    synchronized (m_parent.m_cur_process_state) {
                        cur_state = m_parent.m_cur_process_state;
                    }

                    if (cur_state != PROCESSING_CODE.Encode) {
                        realtimeProcess(width, height);

                    }
                    else{
                        realtimeEncode(width, height);
                    }

                    measure_time_begin.update();

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void realtimeProcess(int width, int height){
        if (m_parent.m_cur_color_mode == PROCESSING_MODE.RGB) {
            // ============================================================
            // --------------------- Convert YUV to RGBA
            MMeasureTime begin_convert = new MMeasureTime();
            JNI2.YuvNV21toRGB(m_rgba_buf.array(), m_yuv_buf.array(), width, height);
            int time_process = begin_convert.untilNow();
            m_draw_activity.status_addTimeConvert(time_process);
            //Log.d(TAG, "Convert time: " + time_process);

            // ============================================================
            // --------------------------- Process
            MMeasureTime begin_process = new MMeasureTime();
            MSize new_size = processFrame(width, height);
            int new_height = new_size.m_height;
            int new_width = new_size.m_width;

            time_process = begin_process.untilNow();
            m_draw_activity.status_addTimeProcess(time_process);
            //Log.d(TAG, "Process time: " + time_process);

            // ============================================================
            // ---------------- Convert to bitmap format
            MMeasureTime begin_bitmap = new MMeasureTime();
            m_rgba_buf.rewind();
            if (new_width > new_height) {
                m_result_landscape.copyPixelsFromBuffer(m_rgba_buf);
                m_draw_activity.setResult(m_result_landscape);

                time_process = begin_bitmap.untilNow();
                m_draw_activity.status_addTimeBitmap(time_process);
                //Log.d(TAG, "Render time: " + time_process);
            } else {
                m_result_portrait.copyPixelsFromBuffer(m_rgba_buf);
                m_draw_activity.setResult(m_result_portrait);

                time_process = begin_bitmap.untilNow();
                m_draw_activity.status_addTimeBitmap(time_process);
                //Log.d(TAG, "Render time: " + time_process);
            }
        }else{
            // ============================================================
            // --------------------------- Process
            MMeasureTime begin_process = new MMeasureTime();
            MSize new_size = processFrame(width, height);
            int new_height = new_size.m_height;
            int new_width = new_size.m_width;

            int time_process = begin_process.untilNow();
            m_draw_activity.status_addTimeProcess(time_process);
            //Log.d(TAG, "Process time: " + time_process);

            // ============================================================
            // --------------------- Convert YUV to RGBA
            MMeasureTime begin_convert = new MMeasureTime();
            JNI2.YuvNV21toRGB(m_rgba_buf.array(), m_yuv_buf.array(), new_width, new_height);
            //Log.d("Processing time CVT: ", "" + begin_convert.untilNow());
            time_process = begin_convert.untilNow();
            m_draw_activity.status_addTimeConvert(time_process);
            //Log.d(TAG, "Convert time: " + time_process);

            // ============================================================
            // ---------------- Convert to bitmap format
            MMeasureTime begin_bitmap = new MMeasureTime();
            m_rgba_buf.rewind();
            if (new_width > new_height) {
                m_result_landscape.copyPixelsFromBuffer(m_rgba_buf);
                m_draw_activity.setResult(m_result_landscape);

                time_process = begin_bitmap.untilNow();
                m_draw_activity.status_addTimeBitmap(time_process);
                //Log.d(TAG, "Render time: " + time_process);
            } else {
                m_result_portrait.copyPixelsFromBuffer(m_rgba_buf);
                m_draw_activity.setResult(m_result_portrait);

                time_process = begin_bitmap.untilNow();
                m_draw_activity.status_addTimeBitmap(time_process);
                //Log.d(TAG, "Render time: " + time_process);
            }
        }

    }

    protected void realtimeEncode(int width, int height){

    }

    protected MSize processFrame(int width, int height){
        MSize new_size = new MSize(width, height);
        PROCESSING_CODE cur_state;

        synchronized (m_parent.m_cur_process_state) {
            cur_state = m_parent.m_cur_process_state;
        }

        PROCESSING_MODE cur_mode;
        synchronized (m_parent.m_cur_color_mode){
            cur_mode = m_parent.m_cur_color_mode;
            //cur_mode = PROCESSING_MODE.RGB;
        }

        if(cur_mode == PROCESSING_MODE.RGB) {
            if (cur_state == PROCESSING_CODE.Rotate_left) {
                JNI2.rotateLeft(m_rgba_buf.array(), width, height);
                new_size.m_width = height;
                new_size.m_height = width;
            } else if (cur_state == PROCESSING_CODE.Rotate_right) {
                JNI2.rotateRight(m_rgba_buf.array(), width, height);
                new_size.m_width = height;
                new_size.m_height = width;
            } else if (cur_state == PROCESSING_CODE.Rotate_down) {
                JNI2.rotateDown(m_rgba_buf.array(), width, height);
            } else if (cur_state == PROCESSING_CODE.Flip_Horizontal) {
                JNI2.flipHorizontal(m_rgba_buf.array(), width, height);
            } else if (cur_state == PROCESSING_CODE.Flip_Vertical) {
                JNI2.flipVertical(m_rgba_buf.array(), width, height);
            }

            return new_size;
        }

        if(cur_mode == PROCESSING_MODE.YUV) {
            //String typeProcess = "";
            //long start = System.currentTimeMillis();
            if (cur_state == PROCESSING_CODE.Rotate_left) {
                JNI2.YuvNV21RotateLeft(m_yuv_buf.array(), width, height);
                //typeProcess = "Rorate Left: ";
                new_size.m_width = height;
                new_size.m_height = width;
            } else if (cur_state == PROCESSING_CODE.Rotate_right) {
                JNI2.YuvNV21RotateRight(m_yuv_buf.array(), width, height);
                //typeProcess = "Rotate Right";
                new_size.m_width = height;
                new_size.m_height = width;
            } else if (cur_state == PROCESSING_CODE.Rotate_down) {
                //typeProcess = "Rotate Down";
                JNI2.YuvNV21RotateDown(m_yuv_buf.array(), width, height);
            } else if (cur_state == PROCESSING_CODE.Flip_Horizontal) {
                JNI2.YuvNV21FlipHorizontal(m_yuv_buf.array(), width, height);
                //typeProcess = "Rotate Horizontal";
            } else if (cur_state == PROCESSING_CODE.Flip_Vertical) {
                JNI2.YuvNV21FlipVertical(m_yuv_buf.array(), width, height);
                //typeProcess = "Rotate Vertical";
            }
//            long timeProcess = System.currentTimeMillis() - start;
//            Log.d("CameraPreview", typeProcess + " Down time process: " + timeProcess);
        }

        return new_size;
    }

}

class MSize{
    public int m_width = -1;
    public int m_height = -1;

    public MSize(int width, int height){
        m_width = width;
        m_height = height;
    }
}

enum PROCESSING_MODE{
    RGB,
    YUV
}

enum  ENCODE_LEVEL{
    ULTRA_FAST,// 0
    SUPER_FAST,
    VERY_FAST,
    FASTER,
    FAST,
    MEDIUM,
    SLOW,
    SLOWER,
    VERY_SLOW,
    PLACEBO
}