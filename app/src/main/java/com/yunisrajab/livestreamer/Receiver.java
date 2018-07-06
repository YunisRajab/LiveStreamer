package com.yunisrajab.livestreamer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver {

    AudioManager mAudioManager;
    int sampleRate, minBufSize, port;
    String TAG = "VS Receiver";

    public void Receiver(AudioManager am, int rate, int buff, int port)
    {
        mAudioManager = am;
        sampleRate = rate;
        minBufSize = buff;
        this.port = port;
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
                    while(true) {
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
}
