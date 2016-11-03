#include <jni.h>
#include <string>

extern "C"
jstring
Java_com_example_hanhnv_camapp_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++ Native";
    return env->NewStringUTF(hello.c_str());
}
