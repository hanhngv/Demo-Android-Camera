package com.hanhnv;

import java.nio.IntBuffer;

/**
 * Created by hanhnv on 19/10/2016.
 */

public class JNI2 {
    static {
        //System.loadLibrary("native-lib");
//        System.loadLibrary("libx264.so.148");
        System.loadLibrary("x264");
        System.loadLibrary("NativeHanhNV");
    }

    public static native int YuvNV21toRGB(int[]rgba_result, byte[] yuv, int width, int height);
    public static native int YuvNV21FlipHorizontal(byte[]yuv, int width, int height);
    public static native int YuvNV21FlipVertical(byte[]yuv, int width, int height);
    public static native int YuvNV21RotateLeft(byte[]yuv, int width, int height);
    public static native int YuvNV21RotateRight(byte[]yuv, int width, int height);
    public static native int YuvNV21RotateDown(byte[]yuv, int width, int height);
    public static native int flipHorizontal(int[]rgba_img, int width, int height);
    public static native int flipVertical(int[]rgba_img, int width, int height);
    public static native int rotateLeft(int[]rgba_img, int width, int height);
    public static native int rotateRight(int[]rgba_img, int width, int height);
    public static native int rotateDown(int[]rgba_img, int width, int height);
    public static native int releaseBuffer();

    public native int ENCODEinit(int width, int heigth, int encode_level);

    // public static native int init();
    //public
}
