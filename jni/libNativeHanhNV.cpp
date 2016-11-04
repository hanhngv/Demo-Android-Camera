#include "libNativeHanhNV.h"
#include <string.h>
//#include <android/log.h>

extern "C"
JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return 25;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_square(
        JNIEnv *env,
        jobject,
        jint num) {
    return num * num;
}

extern "C"
JNIEXPORT jintArray JNICALL Java_com_hanhnv_JNI2_Test(
		JNIEnv *env,
		jobject,
		jbyteArray yuv){

    jintArray result = env->NewIntArray(env->GetArrayLength(yuv));
	jint* result_data = env->GetIntArrayElements(result, 0);

	jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);

	for(int i = 0; i < env->GetArrayLength(result); i++)
		result_data[i] = 2;

	env->SetIntArrayRegion(result, 0, env->GetArrayLength(result), result_data);

	//env->ReleaseByteArrayElements(yuv, yuv_data, 0);
	//env->ReleaseIntArrayElements(result, result_data, 0);

	return result;
}


JNIEXPORT void JNICALL Java_com_hanhnv_JNI2_Test2(
		JNIEnv *env,
		jobject,
		jintArray result,
		jbyteArray raw_data){
	jint* result_data = env->GetIntArrayElements(result, 0);

	jbyte* yuv_data = env->GetByteArrayElements(raw_data, 0);

	for(int i = 0; i < env->GetArrayLength(result); i++)
			result_data[i] = yuv_data[i] * yuv_data[i];

	env->SetIntArrayRegion(result, 0, env->GetArrayLength(result), result_data);

	env->ReleaseByteArrayElements(raw_data, yuv_data, 0);
}

extern "C"
JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21toRGB(
        JNIEnv *env,
        jobject,
        jintArray rgba_result,
        jbyteArray yuv,
        jint width,
        jint height){
    int frameSize = width * height;

    int ii = 0;
    int ij = 0;
    int di = +1;
    int dj = +1;

    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);
    jint* result_data = env->GetIntArrayElements(rgba_result, 0);
	int r = 255, g = 0, b = 255;
    int y, u, v;

    int a0, a1, a2, a3, a4;
    // ===========================================================================================
    // ===========================================================================================
    int a = 0;
    for (int i = 0, ci = ii; i < height; ++i, ci += di) {
        for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
            y = (0xff & ((int) yuv_data[ci * width + cj]));
            u = (0xff & ((int) yuv_data[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
            v = (0xff & ((int) yuv_data[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));

            y = y < 16 ? 16 : y;

//            r = y + (1.370705 * (v-128));
//            g = y - (0.698001 * (v-128)) - (0.337633 * (u-128));
//            b = y + (1.732446 * (u-128));
            a0 = 1192 * (y - 16);
            a1 = 1634 * (v - 128);
            a2 = 832 * (v - 128);
            a3 = 400 * (u - 128);
            a4 = 2066 * (u - 128);

            r = (a0 + a1) >> 10;
            g = (a0 - a2 - a3) >> 10;
            b = (a0 + a4) >> 10;

            r = r < 0 ? 0 : (r > 255 ? 255 : r);
            g = g < 0 ? 0 : (g > 255 ? 255 : g);
            b = b < 0 ? 0 : (b > 255 ? 255 : b);

            result_data[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
    }


    // ===========================================================================================
    // ===========================================================================================
//    int uv_index = 0;
//    for (int i = 0; i < frameSize; i++) {
//        u = (0xff & ((int)(yuv_data[frameSize + uv_index])));
//        v = (0xff & ((int)(yuv_data[frameSize + uv_index + 1])));
//        //u = 0;
//        //v = 0;
//
//        for (int k = 0; k < 2; k++) {
//            y = (0xff & ((int)(yuv_data[i])));
//            y = y < 16 ? 16 : y;
//
//            r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
//            g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
//            b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));
//
//            r = r < 0 ? 0 : (r > 255 ? 255 : r);
//            g = g < 0 ? 0 : (g > 255 ? 255 : g);
//            b = b < 0 ? 0 : (b > 255 ? 255 : b);
//
//            result_data[i] = 0xff000000 | (r << 16) | (g << 8) | b;
//
//            i++;
//        }
//        uv_index++;
//
//    }


    // ===========================================================================================
    // ===========================================================================================
//    int uv_index  = frameSize;
//    for(int result_row_index = 0; result_row_index < frameSize; result_row_index += 2 * width){
//        int in_row_index = 0;
//
//        for(int i = 0; i < width / 2; i++) {
//            u = (0xff & ((int) (yuv_data[uv_index++])));
//            v = (0xff & ((int) (yuv_data[uv_index++])));
//
//            for (int k = 0; k < 2; k++) {
//                y = (0xff & ((int) (yuv_data[result_row_index + in_row_index])));
//
//                y = y < 16 ? 16 : y;
//
//                a0 = 1192 * (y - 16);
//                a1 = 1634 * (v - 128);
//                a2 = 832 * (v - 128);
//                a3 = 400 * (u - 128);
//                a4 = 2066 * (u - 128);
//
//                r = (a0 + a1) >> 10;
//                g = (a0 - a2 - a3) >> 10;
//                b = (a0 + a4) >> 10;
//
//                r = r < 0 ? 0 : (r > 255 ? 255 : r);
//                g = g < 0 ? 0 : (g > 255 ? 255 : g);
//                b = b < 0 ? 0 : (b > 255 ? 255 : b);
//
//                result_data[result_row_index + in_row_index] =
//                        0xff000000 | (r << 16) | (g << 8) | b;
//
//                in_row_index++;
//            }
//        }
//    }
//    uv_index  = frameSize;
//    for(int result_row_index = width; result_row_index < frameSize; result_row_index += 2 * width){
//        int in_row_index = 0;
//
//        for(int i = 0; i < width / 2; i++) {
//            u = (0xff & ((int) (yuv_data[uv_index++])));
//            v = (0xff & ((int) (yuv_data[uv_index++])));
//
//            for (int k = 0; k < 2; k++) {
//                y = (0xff & ((int) (yuv_data[result_row_index + in_row_index])));
//
//                y = y < 16 ? 16 : y;
//
//                a0 = 1192 * (y - 16);
//                a1 = 1634 * (v - 128);
//                a2 = 832 * (v - 128);
//                a3 = 400 * (u - 128);
//                a4 = 2066 * (u - 128);
//
//                r = (a0 + a1) >> 10;
//                g = (a0 - a2 - a3) >> 10;
//                b = (a0 + a4) >> 10;
//
//                r = r < 0 ? 0 : (r > 255 ? 255 : r);
//                g = g < 0 ? 0 : (g > 255 ? 255 : g);
//                b = b < 0 ? 0 : (b > 255 ? 255 : b);
//
//                result_data[result_row_index + in_row_index] =
//                        0xff000000 | (r << 16) | (g << 8) | b;
//
//                in_row_index++;
//            }
//        }
//    }
    // ===========================================================================================
    // ===========================================================================================
//    long data_length = env->GetArrayLength(yuv);
//    int even_row_index = 0;
//    int odd_row_index = width;
//
//    memset((void*)result_data, 255, frameSize * 4);
//
//    unsigned char argb[4];
//    unsigned char* argb_tmp = NULL;
//    argb[3] = 255;
//    unsigned char yc, uc, vc;
//    for(int uv_index = frameSize; uv_index < data_length;){
//        vc = yuv_data[uv_index++];
//        uc = yuv_data[uv_index++];
//
//        // Row 0
//        for (int i = 0; i < 2; i++) {
//            yc = yuv_data[even_row_index];
//            yc = yc < 16 ? 16 : yc;
//
//            int rc = yc + (1.370705 * (vc-128));
//            int gc = yc - (0.698001 * (vc-128)) - (0.337633 * (uc-128));
//            int bc = yc + (1.732446 * (uc-128));
//
//            argb_tmp = (unsigned char*)(result_data + even_row_index);
//
//            argb_tmp[0] = rc < 0 ? 0 : (rc > 255 ? 255 : rc);
//            argb_tmp[1] = gc < 0 ? 0 : (gc > 255 ? 255 : gc);
//            argb_tmp[2] = bc < 0 ? 0 : (bc > 255 ? 255 : bc);
//
//            even_row_index++;
//        }
//
//        if(even_row_index % width == 0)
//            even_row_index += width;
//
//        // Row 1
//        for (int i = 0; i < 2; i++) {
//            yc = yuv_data[odd_row_index];
//            yc = yc < 16 ? 16 : yc;
//
//            int rc = yc + (1.370705 * (vc-128));
//            int gc = yc - (0.698001 * (vc-128)) - (0.337633 * (uc-128));
//            int bc = yc+ (1.732446 * (uc-128));
//
//            argb_tmp = (unsigned char*)(result_data + odd_row_index);
//
//            argb_tmp[0] = rc < 0 ? 0 : (rc > 255 ? 255 : rc);
//            argb_tmp[1] = gc < 0 ? 0 : (gc > 255 ? 255 : gc);
//            argb_tmp[2] = bc < 0 ? 0 : (bc > 255 ? 255 : bc);
//
//            odd_row_index++;
//        }
//
//        if(odd_row_index % width == 0)
//            odd_row_index += width;
//    }

    env->SetIntArrayRegion(rgba_result, 0, env->GetArrayLength(rgba_result), result_data);

    //env->ReleaseIntArrayElements(argb, result, 0);
    env->ReleaseByteArrayElements(yuv ,yuv_data, 0);
    env->ReleaseIntArrayElements(rgba_result, result_data, 0);

	return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21FlipHorizontal(
        JNIEnv *env,
        jobject,
        jbyteArray yuv,
        jint width,
        jint height){
    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);
    char* tmp_data = new char[width];

    int step = width;
    int upper = 0;
    int below = width * (height - 1);
    while (upper < below){
        memcpy(tmp_data, yuv_data + upper, step);
        memcpy(yuv_data + upper, yuv_data + below, step);
        memcpy(yuv_data + below, tmp_data, step);

        upper += step;
        below -= step;
    }

    //step = width / 2;
    upper = width * height;
    below = width * height + (height - 2) * step / 2;
    while(upper < below){
        memcpy(tmp_data, yuv_data + upper, step);
        memcpy(yuv_data + upper, yuv_data + below, step);
        memcpy(yuv_data + below, tmp_data, step);

        upper += step;
        below -= step;
    }

    delete[] tmp_data;
    env->SetByteArrayRegion(yuv, 0, env->GetArrayLength(yuv), yuv_data);
    env->ReleaseByteArrayElements(yuv, yuv_data, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21FlipVertical(
        JNIEnv *env,
        jobject,
        jbyteArray yuv,
        jint width,
        jint height){

    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);

    unsigned char tmp_char;
    for(int r = 0; r < height; r++){
        int first = r * width;
        int last = first + width - 1;
        while (first < last){
            tmp_char = yuv_data[first];
            yuv_data[first] = yuv_data[last];
            yuv_data[last] = tmp_char;

            first++;
            last--;
        }
    }

    int data_length = env->GetArrayLength(yuv);

    unsigned char char_buff[2];
    int begin_index = width * height;
    while (begin_index < data_length){
        int first = begin_index;
        int last = begin_index + width - 2;

        while (first < last){
            memcpy(char_buff, yuv_data + first, 2);
            memcpy(yuv_data + first, yuv_data + last, 2);
            memcpy(yuv_data + last, char_buff, 2);

            first += 2;
            last -= 2;
        }

        begin_index += width;
    }

    env->SetByteArrayRegion(yuv, 0, env->GetArrayLength(yuv), yuv_data);
    env->ReleaseByteArrayElements(yuv, yuv_data, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21RotateLeft(
        JNIEnv *env,
        jobject,
        jbyteArray yuv,
        jint width,
        jint height){

    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);

    int new_width = height;
    int new_height = width;

    int frame_size = width * height;

    // ----------- Y channel -----------------------
    unsigned char* tmp_data = new unsigned char[frame_size];
    memcpy(tmp_data, yuv_data, frame_size);

    for(int r = 0; r < new_height; r++){
        for (int c = 0; c < new_width; c++) {
            yuv_data[r * new_width + c] = tmp_data[c * width + (width - 1 - r)];
        }
    }

    // ------------ UV channel ----------------------
    int data_length = env->GetArrayLength(yuv);
    memcpy(tmp_data, yuv_data + frame_size, data_length - frame_size);

    int uv_width = width;
    //int uv_heigth = height / 2;
    int uv_new_width = height;
    int uv_new_height = width / 2;

    unsigned char* uv_result = (unsigned char*)(yuv_data + frame_size);

    for (int r = 0; r < uv_new_height; r++) {
        for (int c = 0; c < uv_new_width; c += 2) {
            memcpy(uv_result + r * uv_new_width + c, tmp_data + c/2 * uv_width + (uv_width - 2 - r * 2), 2);
        }
    }
    delete[] tmp_data;

    env->SetByteArrayRegion(yuv, 0, env->GetArrayLength(yuv), yuv_data);
    env->ReleaseByteArrayElements(yuv, yuv_data, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21RotateRight(
        JNIEnv *env,
        jobject,
        jbyteArray yuv,
        jint width,
        jint height){
    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);

    int new_width = height;
    int new_height = width;

    int frame_size = width * height;

    // ----------- Y channel -----------------------
    unsigned char* tmp_data = new unsigned char[frame_size];
    memcpy(tmp_data, yuv_data, frame_size);

    for(int r = 0; r < new_height; r++){
        for (int c = 0; c < new_width; c++) {
            yuv_data[r * new_width + c] = tmp_data[(new_width - 1 - c) * width + r];
        }
    }

    // ------------ UV channel ----------------------
    int data_length = env->GetArrayLength(yuv);
    memcpy(tmp_data, yuv_data + frame_size, data_length - frame_size);

    int uv_width = width;
    //int uv_heigth = height / 2;
    int uv_new_width = height;
    int uv_new_height = width / 2;

    unsigned char* uv_result = (unsigned char*)(yuv_data + frame_size);

    for (int r = 0; r < uv_new_height; r++) {
        for (int c = 0; c < uv_new_width; c += 2) {
            memcpy(uv_result + r * uv_new_width + c, tmp_data + (uv_new_width - 2 - c)/2 * uv_width + r * 2, 2);
        }
    }
    delete[] tmp_data;

    env->SetByteArrayRegion(yuv, 0, env->GetArrayLength(yuv), yuv_data);
    env->ReleaseByteArrayElements(yuv, yuv_data, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_YuvNV21RotateDown(
        JNIEnv *env,
        jobject,
        jbyteArray yuv,
        jint width,
        jint height){

    jbyte* yuv_data = env->GetByteArrayElements(yuv, 0);

    unsigned char tmp_char;
    int first = 0;
    int last = width * height;
    while (first < last){
        tmp_char = yuv_data[first];
        yuv_data[first] = yuv_data[last];
        yuv_data[last] = tmp_char;

        first++;
        last--;
    }

    unsigned char char_buff[2];

    first = width * height;
    last = env->GetArrayLength(yuv) - 2;

    while (first < last){
        memcpy(char_buff, yuv_data + first, 2);
        memcpy(yuv_data + first, yuv_data + last, 2);
        memcpy(yuv_data + last, char_buff, 2);

        first += 2;
        last -= 2;
    }

    env->SetByteArrayRegion(yuv, 0, env->GetArrayLength(yuv), yuv_data);
    env->ReleaseByteArrayElements(yuv, yuv_data, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_flipHorizontal(
        JNIEnv *env,
        jobject,
        jintArray rgba_img,
        jint width,
        jint height){

    jint* result_data = env->GetIntArrayElements(rgba_img, 0);

    int* tmp = new int[width];

    for (int r = 0; r < height / 2; r++) {
        memcpy((void*)tmp, (void*)(result_data + r * width), width * 4);
        memcpy((void*)(result_data + r * width), (void*)(result_data + (height - 1 - r) * width), width * 4);
        memcpy((void*)(result_data + (height - 1 - r) * width), (void*)tmp, width * 4);
    }

    delete[] tmp;

//    jint* temp_data = new jint[env->GetArrayLength(rgba_img)];
//    memcpy(temp_data, result_data, env->GetArrayLength(rgba_img) * 4);
//
//    for(int r = 0; r < height; r++){
//        memcpy((void*)(result_data + r * width), (void*)(temp_data + (height - 1 -  r) * width), width * 4);
//    }
//
//    delete[] temp_data;
    env->SetIntArrayRegion(rgba_img, 0, env->GetArrayLength(rgba_img), result_data);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_flipVertical(
        JNIEnv *env,
        jobject,
        jintArray rgba_img,
        jint width,
        jint height) {

    jint* result_data = env->GetIntArrayElements(rgba_img, 0);

    int tmp;
    for (int r = 0; r < height; r++) {
        int first = 0, last = width - 1;
        while (first < last){
            tmp = result_data[r * width + first];
            result_data[r * width + first] = result_data[r * width + last];
            result_data[r * width + last] = tmp;

            first++;
            last--;
        }
    }

//    jint* temp_data = new jint[env->GetArrayLength(rgba_img)];
//    memcpy(temp_data, result_data, env->GetArrayLength(rgba_img) * 4);
//
//    for (int r = 0; r < height; r++) {
//        for (int c = 0; c < width; c++) {
//            result_data[r * width + c] = temp_data[r * width + width - 1 - c];
//        }
//    }
//    delete[] temp_data;


    env->SetIntArrayRegion(rgba_img, 0, env->GetArrayLength(rgba_img), result_data);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_rotateLeft(
        JNIEnv *env,
        jobject,
        jintArray rgba_img,
        jint width,
        jint height){

    int new_width = height;
    int new_height = width;
    jint* result_data = env->GetIntArrayElements(rgba_img, 0);
    jint* temp_data = new jint[env->GetArrayLength(rgba_img)];
    memcpy((void*)temp_data, (void*)result_data, env->GetArrayLength(rgba_img) * 4);

    for (int r = 0; r < new_height; r++) {
        for (int c = 0; c < new_width; c++) {
            result_data[r * new_width + c] = temp_data[c * width + (width - 1 - r)];
        }
    }

//    int new_pos = 0;
//    for(int revert = 0; revert < width; revert++){
//        int old_pos = width - 1 - revert;
//        for (int i = 0; i < new_width; i++, ++new_pos) {
//            result_data[new_pos] = temp_data[old_pos];
//
//            old_pos += width;
//        }
//        //first_cell += new_width;
//    }

    delete[] temp_data;
    env->SetIntArrayRegion(rgba_img, 0, env->GetArrayLength(rgba_img), result_data);

    return 0;
}


JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_rotateRight(
        JNIEnv *env,
        jobject,
        jintArray rgba_img,
        jint width,
        jint height){

    jint* result_data = env->GetIntArrayElements(rgba_img, 0);
    jint* temp_data = new jint[env->GetArrayLength(rgba_img)];
    memcpy((void*)temp_data, (void*)result_data, env->GetArrayLength(rgba_img) * 4);

    int new_width = height;
    int new_height = width;

    for (int r = 0; r < new_height; r++) {
        for (int c = 0; c < new_width; c++) {
            result_data[r * new_width + c] = temp_data[(new_width - 1 - c) * width + r];
        }
    }

    delete[] temp_data;
    env->SetIntArrayRegion(rgba_img, 0, env->GetArrayLength(rgba_img), result_data);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hanhnv_JNI2_rotateDown(
        JNIEnv *env,
        jobject,
        jintArray rgba_img,
        jint width,
        jint height){
    jint* result_data = env->GetIntArrayElements(rgba_img, 0);
    //jint* temp_data = new jint[env->GetArrayLength(rgba_img)];
    //memcpy((void*)temp_data, (void*)result_data, env->GetArrayLength(rgba_img) * 4);

//    for (int r = 0; r < height; r++) {
//        for (int c = 0; c < width; c++) {
//            result_data[r * width + c] = temp_data[(height - 1 - r) * width + (width - 1 - c)];
//        }
//    }
//    long frame_size = width * height;
//    for (int i = 0; i < frame_size; i++) {
//        result_data[i] = temp_data[frame_size - 1 - i];
//    }

    //delete[] temp_data;

    long frame_size = width * height;
    int max_index = frame_size - 1;
    int process_index = frame_size / 2;
    int tmp;
    for (int i = 0; i < process_index; i++) {
        tmp = result_data[i];
        result_data[i] = result_data[max_index - i];
        result_data[max_index - i] = tmp;
    }


    env->SetIntArrayRegion(rgba_img, 0, env->GetArrayLength(rgba_img), result_data);

    return 0;
}
