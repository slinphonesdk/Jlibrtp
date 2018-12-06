package syk.com.headextension;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements Thread.UncaughtExceptionHandler {

    private ReceiverSession receiverSession;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                receiverSession = new ReceiverSession();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        receiverSession.destroy();
        receiverSession = null;
        super.onDestroy();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.i("AAA", "uncaughtException   " + e);
        receiverSession.reset();
    }
}
