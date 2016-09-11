package com.example.musicplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.example.musicplayer.bean.Mp3Info;
import com.example.musicplayer.utils.Constants;
import com.example.musicplayer.utils.SpTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author haopi
 * @Date 2016-09-06
 * @Des TODO
 */
public class MusicService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, OnAudioFocusChangeListener {

    private List<Mp3Info> mMusic_list = new ArrayList<>();
    private Messenger mMessenger;
    private MediaPlayer mPlayer;
    private MusicBroadReceiver receiver;
    private int mCurrentPosition;
    private boolean isFirst = true;
    private  boolean isPlaying;
    private int mPosition;
    public static int playMode = 2;//1.单曲循环 2.列表循环 0.随机播放
    private Timer mTimer;
    private Random mRandom = new Random();
    public static int prv_position;

    @Override
    public void onCreate() {
        System.out.println("service : onCreate");
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnErrorListener(this);//资源出错
        mPlayer.setOnPreparedListener(this);//资源准备好的时候
        mPlayer.setOnCompletionListener(this);//播放完成的时候
        regFilter();

        //创建audioManger
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mMusic_list = intent.getParcelableArrayListExtra("music_list");
            mMessenger = (Messenger) intent.getExtras().get("messenger");
            mPosition = SpTools.getInt(getApplicationContext(), "music_current_position", 0);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (receiver != null) {
            unregisterReceiver(receiver); // 服务终止时解绑
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }


    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        System.out.println("service : OnError");
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_NEXT);
        sendBroadcast(intent);
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        mPlayer.start();//开始播放

        if (mMessenger != null) {
            sentMessageToMain();
            sentMessageToMainByTimer();
        }
    }

    private void sentMessageToMain() {
        Message message = Message.obtain();
        message.what = Constants.MSG_PREPARED;
        message.arg1 = mPosition;
        message.obj = mPlayer.isPlaying();
        try {
            //发送位置信息
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentPlayToMain() {
        Message message = Message.obtain();
        message.what = Constants.MSG_PLAY;
        message.arg1 = mPosition;
        message.obj = mPlayer.isPlaying();
        try {
            //发送位置信息
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentMessageToMainByTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (mPlayer.isPlaying()) {
                        //1.准备好的时候.告诉activity,当前歌曲的总时长
                        int currentPosition = mPlayer.getCurrentPosition();
                        int totalDuration = mPlayer.getDuration();
                        Message msg = Message.obtain();
                        msg.what = Constants.MSG_ONPREPARED;
                        msg.arg1 = currentPosition;
                        msg.arg2 = totalDuration;
                        //2.发送消息
                        mMessenger.send(msg);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_NEXT);
        sendBroadcast(intent);
    }

    /**
     * 播放
     */
    private void play(int position) {
        if (mPlayer != null && mMusic_list.size() > 0) {
            mPlayer.reset();
            try {
                mPlayer.setDataSource(mMusic_list.get(position).getUrl());
                mPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 暂停
     */
    private void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mCurrentPosition = mPlayer.getCurrentPosition();
            mPlayer.pause();
            sentPlayToMain();
        }
    }

    /**
     * 注册广播
     */
    private void regFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_LIST_ITEM);
        intentFilter.addAction(Constants.ACTION_PAUSE);
        intentFilter.addAction(Constants.ACTION_PLAY);
        intentFilter.addAction(Constants.ACTION_NEXT);
        intentFilter.addAction(Constants.ACTION_PRV);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.setPriority(1000);
        if (receiver == null) {
            receiver = new MusicBroadReceiver();
        }
        registerReceiver(receiver, intentFilter);
    }

    /**
     * 广播接收者
     */
    public class MusicBroadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_LIST_ITEM:
                    //点击左侧菜单
                    isPlaying = true;
                    isFirst = false;
                    mPosition = intent.getIntExtra("position", 0);
                    play(mPosition);
                    break;
                case Constants.ACTION_PAUSE:
                    //暂停播放
                    isPlaying = false;
                    pause();
                    break;
                case Constants.ACTION_PLAY:
                    isPlaying = true;
                    //开始播放
                    if (isFirst) {
                        isFirst = false;
                        play(mPosition);
                    } else {
                        mPlayer.seekTo(mCurrentPosition);
                        mPlayer.start();
                        //通知是否在播放
                        sentPlayToMain();
                    }
                    break;
                case Constants.ACTION_NEXT:
                    //下一首
                    prv_position = mPosition;
                    isPlaying = true;
                    if (playMode % 3 == 1) {//1.单曲循环
                        play(mPosition);
                    } else if (playMode % 3 == 2) {//2.列表播放
                        mPosition++;
                        if (mPosition <= mMusic_list.size() - 1) {
                            play(mPosition);
                        } else {
                            mPosition = 0;
                            play(mPosition);
                        }
                    } else if (playMode % 3 == 0) {// 0.随机播放
                        play(getRandom());
                    }
                    break;
                case Constants.ACTION_PRV:
                    //上一首
                    prv_position = mPosition;
                    isPlaying = true;
                    if (playMode % 3 == 1) {//1.单曲循环
                        play(mPosition);
                    } else if (playMode % 3 == 2) {//2.列表播放
                        mPosition--;
                        if (mPosition < 0) {
                            mPosition = mMusic_list.size() - 1;
                            play(mPosition);
                        } else {
                            play(mPosition);
                        }
                    } else if (playMode % 3 == 0) {// 0.随机播放
                        play(getRandom());
                    }
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    //如果耳机拨出时暂停播放
                    Intent intent_pause = new Intent();
                    intent_pause.setAction(Constants.ACTION_PAUSE);
                    sendBroadcast(intent_pause);

                    break;
            }
        }
    }

    private int getRandom() {
        mPosition = mRandom.nextInt(mMusic_list.size());
        return mPosition;
    }

    /**
     * ---------------音频焦点处理相关的方法---------------
     **/
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN://你已经得到了音频焦点。
                System.out.println("-------------AUDIOFOCUS_GAIN---------------");
                // resume playback
                mPlayer.start();
                mPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS://你已经失去了音频焦点很长时间了。你必须停止所有的音频播放
                System.out.println("-------------AUDIOFOCUS_LOSS---------------");
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mPlayer.isPlaying())
                    mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://你暂时失去了音频焦点
                System.out.println("-------------AUDIOFOCUS_LOSS_TRANSIENT---------------");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying())
                    mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
                System.out.println("-------------AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK---------------");
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying())
                    mPlayer.setVolume(0.1f, 0.1f);
                break;
        }

    }

}
