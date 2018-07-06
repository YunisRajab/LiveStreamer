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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Button startButton,stopButton;
    private EditText ipAdd;

    AudioRecord recorder;
    AudioManager mAudioManager;
//    AudioStream streamer;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    private String TAG = "VS";
    private String mAddress;

    public byte[] buffer = new byte[minBufSize];
    public static DatagramSocket socket;
    private int port=50005;
    Toast mToast;

    // this is a wild guess
    public static final int STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;

    // actually play the PCM stream
    private AudioTrack mAudioTrack;
    // used to communicate with the server
    private DatagramSocket mSocket;
    // audio stream configuration values
//    private StreamConfig mStreamConfig;

    // target server info
//    private final String mNewAddress;
//    private InetAddress mInetAddress;
//    private final int mPort;

    // holds data for the audio frame packet
    private byte[] mPcmFrame;
    // prevent over buffering the AudioTrack
    private int mSkipFrameEveryTot;
    // number of byte received
    private long byteCount;

//    private final AudioClientListener mListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set address to previously saved address when the app starts
//        ipAdd.setText(mAddress);

        mToast.makeText(getApplicationContext(), "Returning", Toast.LENGTH_SHORT);

        TextView view = (TextView) findViewById(R.id.textView);
        view.setText(getLocalIPAddress());
        startButton = (Button) findViewById (R.id.start_button);
        stopButton = (Button) findViewById (R.id.stop_button);

        startButton.setOnClickListener (startListener);
        stopButton.setOnClickListener (stopListener);

        ipAdd = (EditText) findViewById(R.id.addText);

        requestRecordAudioPermission();

        Toast.makeText(getApplicationContext(),"MinBufferSize: " +minBufSize, Toast.LENGTH_SHORT).show();
    }

    private final OnClickListener stopListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (status) {
                status = false;
                recorder.release();
                Log.d(TAG,"Recorder released");
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
            }
        }

    };

    private final OnClickListener startListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = true;
            mAddress = ipAdd.getText().toString();
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

                    final InetAddress destination = InetAddress.getByName(mAddress);
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
//                        Log.d(TAG,"MinBufferSize: " +minBufSize);
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
                    mAudioManager =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STREAM);
                    track.play();
                        // Define a socket to receive the audio
                        DatagramSocket socket = new DatagramSocket(port);
                        byte[] buf = new byte[minBufSize];
                        while(status) {
                            // Play back the audio received from packets
                            DatagramPacket packet = new DatagramPacket(buf, minBufSize);
                            socket.receive(packet);
                            Log.i(TAG, "Packet received: " + packet.getLength());
                            track.write(packet.getData(), 0, minBufSize);
                        }
                        // Stop playing back and release resources
                        socket.disconnect();
                        socket.close();
                        track.stop();
                        track.flush();
                        track.release();
                        return;
//
//                    DatagramSocket socket = new DatagramSocket(port);
//                    Log.d(TAG, "Socket Created");
//
//                    byte[] buffer = new byte[minBufSize];
//
//                    Log.d(TAG,"Buffer created of size " + minBufSize);
//                    DatagramPacket packet;
//
////                    final InetAddress senderAddress = InetAddress.getByName(mAddress);
//                    final InetAddress localAddress = InetAddress.getByName(getLocalIPAddress());
//                    Log.d(TAG, "Local address "+localAddress);
//

//                    AudioGroup audioGroup = new AudioGroup();
//                    audioGroup.setMode(AudioGroup.MODE_NORMAL);
//                    AudioStream audioStream = new AudioStream(localAddress);
//                    audioStream.setCodec(AudioCodec.PCMU);
//                    audioStream.setMode(RtpStream.MODE_NORMAL);
//                    //set receiver(vlc player) machine ip address(please update with your machine ip)
//                    audioStream.associate(InetAddress.getByName(mAddress), port);
//                    audioStream.join(audioGroup);
//
//                    initAudioTrack();
//                    Log.d(TAG, "Initialized track");
//                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
//                            sampleRate, AudioFormat.CHANNEL_IN_MONO, audioFormat,
//                            buffer.length, AudioTrack.MODE_STREAM);
//                    audioTrack.play();
//
////                    while (status == true)
//                    {
//                        //putting buffer in the packet
//                        packet = new DatagramPacket (buffer,buffer.length);
//                        socket.receive(packet);
//                        Log.d(TAG,"MinBufferSize: " +minBufSize);
//                    }

                }catch (Exception e)
                {
                    Log.e(TAG, "Exception: "+e);
                }
            }
        }).start();
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

    public static String getLocalIPAddress () {
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

    private void initAudioTrack(){
        Log.d(TAG,"initAudioTrack()");

        int bitDepth = buffer.length == 1 ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT;

        mAudioTrack = new AudioTrack(STREAM_TYPE, 16000,
                AudioFormat.CHANNEL_OUT_MONO,
                bitDepth, buffer.length,
                AudioTrack.MODE_STREAM);

        mAudioTrack.setVolume(AudioTrack.getMaxVolume());
        mPcmFrame = new byte[buffer.length];
        mAudioTrack.play();
    }


}