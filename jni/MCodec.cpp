#include "MCodec.h"

MCodec::MCodec() {
    m_be_initted = false;

    m_luma_size = 0;
    m_chroma_size = 0;

    m_encode_handler = NULL;
    m_nal = NULL;

    i_frame = 0;

    if(x264_param_default_preset(&m_param, "medium", NULL) < 0)
        return;

    m_param.i_csp = X264_CSP_NV21;
    m_param.b_vfr_input = 0;
    m_param.b_repeat_headers = 1;
    m_param.b_annexb = 1;


}

ENCODE_RETURN MCodec::init(int width, int height, int encode_level){
    m_encode_level = encode_level;

    m_param.i_width = width;
    m_param.i_height = height;
    m_luma_size = width * height;
    m_chroma_size = m_luma_size / 4;

    if(x264_param_apply_profile(&m_param, "high") < 0)
        return ENR_APPLY_PROFILE_FAIL;

    if(x264_picture_alloc(&m_pic, m_param.i_csp, m_param.i_width, m_param.i_height) < 0)
        return ENR_ALLOC_MEMORY_FAIL;

    m_encode_handler = x264_encoder_open(&m_param);
    if(!m_encode_handler)
        return ENR_ENCODE_FAIL;

    m_be_initted = true;

    return ENR_SUCCESS;
}