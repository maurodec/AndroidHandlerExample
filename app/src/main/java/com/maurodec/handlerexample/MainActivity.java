package com.maurodec.handlerexample;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;


public class MainActivity extends AppCompatActivity {
    // This is a thread that will be doing work in the background. This thread will run as long as
    // the activity is presented to the user. If the activity goes to the background then this thread is destroyed.
    private BackgroundThread bgThread;
    private View colorDisplay;

    // This Handler will take care of messages sent to the main thread (as it is created there).
    // Its job is to update the UI.
    private Handler uiThreadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Get the color that was sent to us.
            int color = msg.arg1;
            // Set it. this can only be done on the Main Thread.
            colorDisplay.setBackgroundColor(color);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        colorDisplay = findViewById(R.id.colorDisplay);

        // Watch for the switch changing its checked state.
        ((Switch) findViewById(R.id.mysteriousSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSwitchChanged(isChecked);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The activity will no longer be presented to the user.
        // Turn off the switch
        ((Switch) findViewById(R.id.mysteriousSwitch)).setChecked(false);

        // And kill the background thread.
        bgThread.interrupt();
        bgThread = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity was just created or is coming back to the foreground.
        // the activity is presented to the user, so create a background thread to do work.
        bgThread = new BackgroundThread();
        bgThread.start();
    }

    private void onSwitchChanged(boolean isChecked) {
        // When the checked state of the switch changes we need to send a message to the background thread, either
        // because it needs to start "doing work" ot it needs to stop "doing work".

        // Ask the background thread's handler for a message instance.
        Message messageToBG = this.bgThread.bgThreadHandler.obtainMessage();
        // Set the correct value for the what field so that it knows what this message is about.
        messageToBG.what = isChecked ? BackgroundThread.WORK : BackgroundThread.STOP;

        // Deliver the message to the background thread. This isn't handled immediately, the Handler actually puts
        // it in that thread's message queue.
        this.bgThread.bgThreadHandler.sendMessage(messageToBG);
    }

    // This is the background thread that will be doing work for us. This can be made a bit shorter by using
    // Android's HandlerThread class.
    public class BackgroundThread extends Thread {
        // How often we will change the background of the view
        private static final int BG_CHANGE_INTERVAL = 1 * 1000;

        // We define the type of messages in these constants.
        private static final int WORK = 1;
        private static final int STOP = 0;

        // Make our Handler an instance variable so we can access it from the MainActivity and ask it for
        // Message instances.
        private Handler bgThreadHandler;

        @Override
        public void run() {
            // Prepare a Looper for this thread as we want it to behave like an event loop
            Looper.prepare();

            // We create the handler here. It cannot be done when we declare the instance variable as
            // we need to call Looper.prepare() first. If we didn't call Looper.prepare() an exception
            // would be thrown
            this.bgThreadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // Depending on the type of message...
                    switch (msg.what) {
                        case STOP:
                            // Stop, so remove all pending messages and just sit there idling.
                            this.removeMessages(WORK);

                            break;
                        case WORK:
                            // Obtain a message that we will send to the Main Thread.
                            Message messageToUI = MainActivity.this.uiThreadHandler.obtainMessage();
                            // Get a random color and give it to the Message we just obtained.
                            messageToUI.arg1 = getRandomColor();
                            // Send the message to the Main Thread.
                            MainActivity.this.uiThreadHandler.sendMessage(messageToUI);

                            // We will enqueue a WORK message to be delivered to this Handler in the future.
                            // This is what keeps the "color display" view changing.
                            this.sendEmptyMessageDelayed(WORK, BG_CHANGE_INTERVAL);

                            break;
                    }
                }
            };

            // Now that we have a handler attached we can actually turn this into an event loop.
            Looper.loop();
        }

        private int getRandom() {
            return (int) (Math.random() * 256);
        }

        private int getRandomColor() {
            // Create a random fully opaque color packed in an int.
            return 0xFF000000 | (getRandom() << 16) | (getRandom() << 8) | getRandom();
        }
    }
}
