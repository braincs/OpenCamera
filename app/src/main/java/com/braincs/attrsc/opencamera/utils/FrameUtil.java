package com.braincs.attrsc.opencamera.utils;


import android.annotation.TargetApi;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by Shuai
 * 08/09/2019.
 */
public class FrameUtil {
    public static final int INVALID_ARGUMENT = -1;
    public static final int OK = 0;

    public static byte[] convertFrame(ByteBuffer buffer){
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * 根据ImageReader得到YUV的图像，此时的排列为YYYYYYYYUUVV这种格式，
     * 稍后要转换成 YYYYUVUV 的 NV21格式
     *
     * @param image Image对象，可以从png/yuv等格式创建
     * @param data  返回NV21格式数据
     * @return 成功则返回MG_UNLOCK_OK
     */
    @TargetApi(21)
    public static int image2NV21(Image image, byte[] data) {
        int result = readImageIntoBuffer(image, data);
        if (result == INVALID_ARGUMENT)
            return INVALID_ARGUMENT;
        revertHalf(data);
        return result;
    }

    /**
     * 将Image读取到data中
     *
     * @param image Image 图像
     * @param data  图像byte数组 byte[]
     * @return 成功则返回MG_UNLOCK_OK
     */
    @TargetApi(21)
    private static int readImageIntoBuffer(Image image, byte[] data) {
        if (image == null) {
            Log.e("NULL Image", "image is null");
            return INVALID_ARGUMENT;
        }
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        int offset = 0;
        for (int plane = 0; plane < planes.length; ++plane) {
            final ByteBuffer buffer = planes[plane].getBuffer();
            final int rowStride = planes[plane].getRowStride();
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed. That's silly, because we are
            // forced to unpack here.
            final int pixelStride = planes[plane].getPixelStride();
            final int planeWidth = (plane == 0) ? imageWidth : imageWidth / 2;
            final int planeHeight = (plane == 0) ? imageHeight : imageHeight / 2;
            if (pixelStride == 1 && rowStride == planeWidth) {
                // Copy whole plane from buffer into |data| at once.
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride.
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; ++row) {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; ++col) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data. See  http://crbug.com/458701  and
                // http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; ++col) {
                    data[offset++] = rowData[col * pixelStride];
                }
            }
        }
        return OK;
    }
    /**
     * 将 YYYYUUVV 转换为 YYYYUVUV
     *
     * @param yuvData yuv data
     */
    private static void revertHalf(byte[] yuvData) {
        int SIZE = yuvData.length;
        byte[] uv = new byte[SIZE / 3];
        int u = SIZE / 6 * 4;
        int v = SIZE / 6 * 5;
        for (int i = 0; i < uv.length - 1; i += 2) {
            uv[i] = yuvData[v++];
            uv[i + 1] = yuvData[u++];
        }
        for (int i = SIZE / 3 * 2; i < SIZE; i++) {
            yuvData[i] = uv[i - SIZE / 3 * 2];
        }

    }
}
