package ertp.com.soundsender;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import ertp.com.jlibrtp.RtpSender;

import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    private RtpSender rtpSender;
    private boolean isRecord;
    private RAudioManager rAudioManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final HandlerThread handlerThread = new HandlerThread("ex");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.post(setupRunnable);

        rAudioManager = new RAudioManager();
        rAudioManager.addListener(new RAudioManager.Listener() {
            @Override
            public void audioFromMic(byte[] bytes) {
                sendData(bytes);
            }
        });

    }

    private Runnable setupRunnable = new Runnable() {
        @Override
        public void run() {
            final String[] pas = {"192.168.88.250","192.168.88.47","192.168.88.48","192.168.88.58","192.168.88.55","192.168.88.60"};
            rtpSender = new RtpSender();
            rtpSender.addParicipants(pas);
        }
    };

    private Handler handler = null;

    public void close() {
        handler.removeCallbacks(null);
        handler = null;
    }

    public void sendAgain(View view) {

    }
    /**
     * 将每帧进行分包并发送数据
     * @param bytes
     */
    private void sendData(byte[] bytes) {

        int dataLength = (bytes.length - 1) / 1480 + 1;
        final byte[][] data = new byte[dataLength][];
        final boolean[] marks = new boolean[dataLength];
        marks[marks.length - 1] = true;
        int x = 0;
        int y = 0;
        int length = bytes.length;
        for (int i = 0; i < length; i++){
            if (y == 0){
                data[x] = new byte[length - i > 1480 ? 1480 : length - i];
            }
            data[x][y] = bytes[i];
            y++;
            if (y == data[x].length){
                y = 0;
                x++;
            }
        }

        rtpSender.rtpSession.sendData(data, null, marks, -1, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
        rtpSender.rtpSession.endSession();
        rAudioManager.stopRecord();
    }

    public void startRecord(final View view) {

        isRecord = !isRecord;
        ((Button)view).setText(isRecord ? "停止录音" : "开始录音");

        if (isRecord) {
            handler.post(recordRunnable);
        }
        else {
            rAudioManager.stopRecord();
            handler.post(stopRunnable);
        }
    }

    private Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            rAudioManager.startRecord();
        }
    };
    private Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            rtpSender.rtpSession.sendData("end".getBytes(), -1, 0);
        }
    };


}


