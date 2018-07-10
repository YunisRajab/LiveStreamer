package com.yunisrajab.livestreamer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Sender {

    AudioRecord recorder;
    private int sampleRate; // 44100 for music
    private int channelConfig;
    private int audioFormat;
    int minBufSize;
    private boolean status = false;
    private String TAG = "VS Sender";
    private String mAddress;
    private int port;
    DatagramSocket mSocket;

    public void Sender(String address, int rate, int channel, int encoding, int buff, int port)    {

        mAddress = address;
        sampleRate = rate;
        channelConfig = channel;
        audioFormat = encoding;
        minBufSize = buff;
        this.port = port;
    }

    public void run() {


        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mSocket = new DatagramSocket(/*port*/);
                    Log.d(TAG, "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d(TAG,"Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(mAddress);

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,
                            channelConfig,audioFormat,minBufSize*10);
                    Log.d(TAG, "Recorder initialized");

                    recorder.startRecording();

                    status = true;
                    while(status) {
                        //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.length);
                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,port);
                        mSocket.send(packet);
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

    public void stop()  {
        if (status) {
            status = false;
            mSocket.disconnect();
            mSocket.close();
            recorder.stop();
            recorder.release();
            recorder = null;
            Log.d(TAG,"Recorder released");
        }
    }

}
