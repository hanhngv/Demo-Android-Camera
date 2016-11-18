#include <stdint.h>

#include "x264/include/x264.h"



enum ENCODE_RETURN{
    ENR_SUCCESS = 0,
    ENR_NOT_INIT,
    ENR_SET_PARAME_FAIL,
    ENR_APPLY_PROFILE_FAIL,
    ENR_ALLOC_MEMORY_FAIL,
    ENR_OPEN_ENCODE_FAIL,
    ENR_ENCODE_FAIL
};

#ifdef __cplusplus
extern "C" {
#endif

class MCodec{
    x264_param_t m_param;
    x264_picture_t m_pic;
    x264_picture_t m_pic_out;
    x264_t* m_encode_handler;
    int i_frame;
    int i_frame_size;

    int m_luma_size;
    int m_chroma_size;

    x264_nal_t *m_nal;
    int i_nal;

    int m_encode_level;

    bool m_be_initted;

public:
    MCodec();

    ENCODE_RETURN init(int width, int height, int encode_level);
};


#ifdef __cplusplus
}
#endif