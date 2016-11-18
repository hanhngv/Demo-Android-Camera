LOCAL_PATH := $(call my-dir)
MY_JNI_PATH := $(LOCAL_PATH)

include $(call all-subdir-makefiles)

LOCAL_PATH := $(MY_JNI_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE    := NativeHanhNV
LOCAL_SRC_FILES := libNativeHanhNV.cpp \
		MCodec.cpp
LOCAL_SHARED_LIBRARIES := x264Prebuilt
LOCAL_LDLIBS := -llog -ldl

include $(BUILD_SHARED_LIBRARY)
