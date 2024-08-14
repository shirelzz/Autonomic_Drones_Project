package com.dji.sdk.sample.demo.accurateLandingController;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecordingVideo {

    private static final String TAG = "RecordingVideo";
    private static final String MIME_TYPE = "video/avc";
    private static final int WIDTH = 1280;  // Adjust to your desired width
    private static final int HEIGHT = 720;  // Adjust to your desired height
    private static final int FRAME_RATE = 30;
    private static final int BIT_RATE = 1000000;

    private boolean isRecording = false;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private BufferInfo bufferInfo;
    private int videoTrackIndex;
    private boolean isMuxerStarted = false;
    private Context context;

    public RecordingVideo(Context context) {
        this.context = context;
        bufferInfo = new BufferInfo();
    }

    public void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            try {
                startRecording();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean getIsRecording() {
        return isRecording;
    }

    public void writeFrame(Bitmap bitmap) {
        if (isRecording && bitmap != null) {
            encodeFrame(bitmap);
        }
    }

    private void startRecording() throws IOException {
        // Initialize MediaCodec
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();

        // Initialize MediaMuxer
        File externalStorage = context.getExternalFilesDir(null);
        assert externalStorage != null;
        String outputPath = externalStorage.getAbsolutePath() + "/output_" + System.currentTimeMillis() + ".mp4";
        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        isRecording = true;
    }

    private void encodeFrame(Bitmap bitmap) {
        // Convert bitmap to YUV format
        ByteBuffer inputBuffer = mediaCodec.getInputBuffers()[mediaCodec.dequeueInputBuffer(-1)];
        inputBuffer.clear();
        inputBuffer.put(convertBitmapToYUV(bitmap));

        // Queue the input buffer
        mediaCodec.queueInputBuffer(mediaCodec.dequeueInputBuffer(-1), 0, inputBuffer.position(), System.nanoTime(), 0);

        // Process output buffers
        MediaCodec.BufferInfo info = new BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffers()[outputBufferIndex];
            if (isMuxerStarted) {
                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, info);
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
        }
    }

    private ByteBuffer convertBitmapToYUV(Bitmap bitmap) {
        // Convert bitmap to YUV420 format (YUV420 format requires specific conversion)
        ByteBuffer yuvBuffer = ByteBuffer.allocate(WIDTH * HEIGHT * 3 / 2);

        // Your YUV conversion logic here
        // This is a placeholder; you'll need to implement actual conversion
        // for YUV420 format from the bitmap data

        return yuvBuffer;
    }

    private void stopRecording() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }

        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
        }

        isRecording = false;
    }
}
