package com.yunisrajab.livestreamer;

import java.io.*;
import java.net.*;

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
import android.widget.Toast;

public class MainActivity extends Activity {
    private Button startButton,stopButton;

    public byte[] buffer;
    public static DatagramSocket socket;
    private int port=50005;

    AudioRecord recorder;
//    AudioStream streamer;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    private String TAG = "VS";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById (R.id.start_button);
        stopButton = (Button) findViewById (R.id.stop_button);

        startButton.setOnClickListener (startListener);
        stopButton.setOnClickListener (stopListener);

        requestRecordAudioPermission();
    }

    private final OnClickListener stopListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = false;
            recorder.release();
            Log.d(TAG,"Recorder released");
        }

    };

    private final OnClickListener startListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = true;
            Toast.makeText(getApplicationContext(),"MinBufferSize: " +minBufSize, Toast.LENGTH_SHORT).show();
            startStreaming();
            startReceiving();
        }

    };

    public void startStreaming() {


        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    Log.d(TAG, "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d(TAG,"Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName("192.168.0.18");
                    Log.d(TAG, "Address retrieved");

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,
                            channelConfig,audioFormat,minBufSize*10);
                    Log.d(TAG, "Recorder initialized");

                    recorder.startRecording();

                    while(status == true) {
                        //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.length);
                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,port);
                        socket.send(packet);
                        Log.d(TAG,"MinBufferSize: " +minBufSize);
                    }
                }
                catch(UnknownHostException e) {
                    Log.e(TAG, "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "IOException");
                } catch (Exception e)
                {
                    Log.e(TAG, "Exception: "+e);
                }
            }

        });
        streamThread.start();
    }

    public void startReceiving()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    Log.d(TAG, "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d(TAG,"Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName("192.168.0.18");
                    Log.d(TAG, "Address retrieved "+destination);

                    AudioManager audio =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    AudioGroup audioGroup = new AudioGroup();
                    audioGroup.setMode(AudioGroup.MODE_NORMAL);
                    AudioStream audioStream = new AudioStream(destination);
                    audioStream.setCodec(AudioCodec.PCMU);
                    audioStream.setMode(RtpStream.MODE_NORMAL);
                    //set receiver(vlc player) machine ip address(please update with your machine ip)
                    audioStream.associate(InetAddress.getByAddress(new byte[] {(byte)192, (byte)168,
                            (byte)1, (byte)19 }), 50005);
                    audioStream.join(audioGroup);

                    while (status == true)
                    {
                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,port);
                        socket.receive(packet);
                        Log.d(TAG,"MinBufferSize: " +minBufSize);
                    }

                }
                catch (Exception e)
                {

                }
            }
        }).start();
    }
    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
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
}