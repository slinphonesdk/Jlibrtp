package syk.com.headextension;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.rtp.AudioStream;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ertp.com.jlibrtp.DataFrame;
import ertp.com.jlibrtp.Participant;
import ertp.com.jlibrtp.RTPAppIntf;
import ertp.com.jlibrtp.RTPSession;

import static java.lang.Thread.sleep;

public class ReceiverSession implements RTPAppIntf
{

    RTPSession rtpSession;

    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static int sampleRateInHz = 44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;
    private static final int BUFFER_SIZE = 2048;

    private AudioTrack audioTrack = null;
    private byte[] buf;
    private List<byte[]> arrayList;

    private static int defaultRtpPort = 18888;
    private static int defaultRTcpPort = 18889;

    public ReceiverSession(int rtpPort, int rTcpPort) {

        // 获得缓冲区字节大小
        bufferSizeInBytes = android.media.AudioTrack.getMinBufferSize(sampleRateInHz,
                channelConfig,
                audioFormat);

        defaultRtpPort = rtpPort;
        defaultRTcpPort = rTcpPort;
        setupRtp();
        setupAudio();

        arrayList = Collections.synchronizedList(new ArrayList<byte[]>());
    }


    private void setupRtp() {
        DatagramSocket rtpSocket = null;
        DatagramSocket rTcpSocket = null;

        try {
            rtpSocket = new DatagramSocket(defaultRtpPort);
            rTcpSocket = new DatagramSocket(defaultRTcpPort);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        rtpSession = new RTPSession(rtpSocket, rTcpSocket);
        rtpSession.naivePktReception(true);
        rtpSession.RTPSessionRegister(this,null, null);

    }

    public ReceiverSession() {
        this(defaultRtpPort,defaultRTcpPort);
    }

    private void setupAudio() {

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                channelConfig,
                audioFormat,
                Math.max(bufferSizeInBytes,BUFFER_SIZE),
                AudioTrack.MODE_STREAM);
    }

    // 医护端接收
    @Override
    public void receiveData(DataFrame frame, Participant participant) {

        byte[] data = frame.getConcatenatedData();
        String msg = new String(data);
        if (msg.length() > 0) {
            if (TextUtils.equals(msg, "end")) {
                Log.w("End", "结束，重置");
                buf = null;
                reset();
                return;
            }
        }

        if (buf == null)
            buf = data;
        else
            buf = merge(buf, data);

        if (frame.marked()) {
            arrayList.add(buf);
            buf = null;
            if (arrayList.size() > 20 && !isEnter && !isPlaying) {

                isEnter = true;
                if (deArrHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("DeArrHandler");
                    handlerThread.start();
                    deArrHandler = new Handler(handlerThread.getLooper());
                }
                deArrHandler.post(deArrRunnable);
            }
        }
    }

    public void reset() {
        isPlaying = false;
        isEnter = false;
        arrayList.clear();
        deArrHandler.removeCallbacks(null);
        audioTrack.stop();
        audioTrack = null;

        rtpSession.endSession();
        rtpSession = null;
        setupAudio();
        setupRtp();
    }

    public void destroy() {
        rtpSession.endSession();
        rtpSession = null;
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }

    private boolean isPlaying;
    private boolean isEnter;
    private Handler deArrHandler;
    private Runnable deArrRunnable = new Runnable() {
        @Override
        public void run() {

            while (isEnter) {
                if (arrayList.size() > 0) {
                    isPlaying = true;
                    byte[] bytes = arrayList.remove(0);
                    decode(bytes);
                    if (audioTrack.getPlayState() == 1)
                        audioTrack.play();
                }

//                try {
//                    sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }
    };


    // 音频 - 停止
    public void audioStop() {
        audioTrack.stop();
    }

    // 音频 - 开启
    public void audioPlay() {
        audioTrack.play();
    }

    /**
     * 播放一帧数据
     * @param data
     */
    private void decode(byte[] data) {

        audioTrack.write(data, 0, data.length);
    }

    /**
     * byte 数组合并
     * @param first 原始数组
     * @param second 合并数组
     * @return 合并后数组
     */
    private byte[] merge(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
    @Override
    public void userEvent(int type, Participant[] participant) { }

    @Override
    public int frameSize(int payloadType) {
        return 1;
    }
}
