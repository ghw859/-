package com.icbc.lingmou.service;

import com.google.zxing.WriterException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 二维码图片生成服务
 */
public interface QRCodeService {

    /**
     * 将 content 编码为 PNG 二维码，写入 outputStream
     *
     * @param content  二维码内容
     * @param size     正方形像素边长，推荐 200~400
     * @param out      输出流
     */
    void writePng(String content, int size, OutputStream out) throws IOException, WriterException;

    /**
     * 生成 PNG 二维码的 byte[]
     */
    byte[] generatePngBytes(String content, int size) throws IOException, WriterException;
}
