package com.icbc.lingmou.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.icbc.lingmou.service.QRCodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ZXing 二维码图片生成实现
 */
@Service
public class QRCodeServiceImpl implements QRCodeService {

    private final QRCodeWriter writer = new QRCodeWriter();

    @Override
    public void writePng(String content, int size, OutputStream out) throws IOException, WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
    }

    @Override
    public byte[] generatePngBytes(String content, int size) throws IOException, WriterException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            writePng(content, size, baos);
            return baos.toByteArray();
        }
    }
}
