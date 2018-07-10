package com.yunisrajab.livestreamer;

import java.io.*;
import java.net.*;
import java.util.Enumeration;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Button startButton,stopButton;
    private EditText editAddress, editRate, editChannel, editEncoding, editBuff, editPort;
    private Switch customSwitch;

    AudioManager mAudioManager;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = false;
    private String TAG = "VS";
    private String mAddress;

    private int port=50005;

    // this is a wild guess
    public static final int STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;

    Receiver receiver = new Receiver();
    Sender sender = new Sender();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set address to previously saved address when the app starts
//        ipAdd.setText(mAddress);;

        TextView view = (TextView) findViewById(R.id.textView);
        view.setText(getLocalIPAddress());
        startButton = (Button) findViewById (R.id.start_button);
        stopButton = (Button) findViewById (R.id.stop_button);
        customSwitch = (Switch) findViewById(R.id.customSwitch);

        startButton.setOnClickListener (startListener);
        stopButton.setOnClickListener (stopListener);
        customSwitch.setChecked(true);

        editAddress = (EditText) findViewById(R.id.addText);
        editRate = (EditText) findViewById(R.id.rateText);
        editChannel = (EditText) findViewById(R.id.channelText);
        editEncoding = (EditText) findViewById(R.id.encodingText);
        editBuff = (EditText) findViewById(R.id.buffText);
        editPort = (EditText) findViewById(R.id.portText);

        requestRecordAudioPermission();

        Toast.makeText(getApplicationContext(),"MinBufferSize: " +minBufSize, Toast.LENGTH_SHORT).show();
    }

    private final OnClickListener stopListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (status) {
                status = false;
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                receiver.stop();
                sender.stop();
            }
        }
    };

    private final OnClickListener startListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = true;
            mAddress = editAddress.getText().toString();
            if (customSwitch.isChecked())   {
                grabCustomSettings();
            }
            receiver.Receiver(sampleRate, channelConfig, audioFormat, minBufSize, port);
            sender.Sender(mAddress, sampleRate, channelConfig, audioFormat, minBufSize, port);
            receiver.run();
            sender.run();
        }

    };

    private void grabCustomSettings()   {
        sampleRate = Integer.parseInt(editRate.getText().toString());
        channelConfig = Integer.parseInt(editChannel.getText().toString());
        audioFormat = Integer.parseInt(editEncoding.getText().toString());
        minBufSize = Integer.parseInt(editBuff.getText().toString());
        port = Integer.parseInt(editPort.getText().toString());
    }
    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Activity Granted!");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "Activity Denied!");
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private static String getLocalIPAddress () {
        String ip = "";
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
//                        ip= inetAddress.getAddress();
                        String sAddr = inetAddress.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (isIPv4)
                            ip = sAddr;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.i("SocketException ", ex.toString());
        }
        return ip;
    }
}