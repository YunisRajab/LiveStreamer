package com.yunisrajab.livestreamer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver {

    AudioTrack mAudioTrack;
    private int sampleRate; // 44100 for music
    private int channelConfig;
    private int audioFormat;
    int minBufSize;
    private int port;
    String TAG = "VS Receiver";
    private boolean status = false;
    DatagramSocket mSocket;

    public void Receiver(int rate, int channel, int encoding, int buff, int port)
    {
        sampleRate = rate;
        channelConfig = channel;
        audioFormat = encoding;
        minBufSize = buff;
        this.port = port;
    }

    public void run()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate,
                            channelConfig, audioFormat, minBufSize,
                            AudioTrack.MODE_STREAM);
                    mAudioTrack.play();
                    // Define a socket to receive the audio
                    mSocket = new DatagramSocket(port);
                    byte[] buff = new byte[minBufSize];
                    status = true;
                    while(status) {
                        // Play back the audio received from packets
                        DatagramPacket packet = new DatagramPacket(buff, minBufSize);
                        mSocket.receive(packet);
                        Log.i(TAG, "Packet received: " + packet.getLength());
                        mAudioTrack.write(packet.getData(), 0, minBufSize);
                    }
                }catch (Exception e)
                {
                    Log.e(TAG, "Exception: "+e);
                }
            }
        }).start();
    }

    public void stop()  {
        if (status) {
            status = false;
            // Stop playing back and release resources
            mSocket.disconnect();
            mSocket.close();
            mAudioTrack.stop();
            mAudioTrack.flush();
            mAudioTrack.release();
        }
    }
}
