package com.braincs.attrsc.opencamera.utils;


import java.nio.ByteBuffer;

/**
 * Created by Shuai
 * 08/09/2019.
 */
public class FrameUtil {

    public static byte[] convertFrame(ByteBuffer buffer){
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
