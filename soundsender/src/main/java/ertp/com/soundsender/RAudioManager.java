package ertp.com.soundsender;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
public class RAudioManager
{

    public interface Listener {
        void audioFromMic(byte[] bytes);
    }

    // 音频获取源
    private int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static int sampleRateInHz = 44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private byte[] mBuffer;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;
    private static final int BUFFER_SIZE = 2048;
    private AudioRecord audioRecord;
    private boolean isRecord = false;// 设置正在录制的状态
    private Listener listener;

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public RAudioManager() {
        mBuffer = new byte[BUFFER_SIZE];
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
                channelConfig, audioFormat);

        // 创建AudioRecord对象
        audioRecord = new AudioRecord(audioSource, sampleRateInHz,
                channelConfig, audioFormat, Math.max(bufferSizeInBytes, BUFFER_SIZE));
    }



    // TODO: 开始录制
    public void startRecord() {
        if (audioRecord == null)
            audioRecord = new AudioRecord(audioSource, sampleRateInHz,
                    channelConfig, audioFormat, bufferSizeInBytes);

        audioRecord.startRecording();
        // 让录制状态为true
        isRecord = true;

        readFromMic();
    }



    // TODO: 停止录制
    public void stopRecord() {
        if (audioRecord != null) {
            System.out.println("stopRecord");
            isRecord = false;//停止文件写入
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }


    // TODO: 读取mic
    private void readFromMic() {
        while (isRecord) {
            //只要还在录音就一直读取
            int read = audioRecord.read(mBuffer, 0, BUFFER_SIZE);
            if(read>0){

                byte[] bt = new byte[read];
                for (int i = 0; i < read; i++) {
                    bt[i] = mBuffer[i];
                }

                if (listener != null)
                    listener.audioFromMic(bt);
            }
        }
    }


}
