package com.example.musicplayer.service;

import android.app.NotificationManager;
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
import android.util.Log;

import com.example.musicplayer.bean.Mp3Info;
import com.example.musicplayer.utility.Constants;
import com.example.musicplayer.utility.SpTools;
import com.example.musicplayer.utility.ThreadPoolUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MusicService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, OnAudioFocusChangeListener {

    private static final String TAG = "MusicService";
    private List<Mp3Info> mMusic_list = new ArrayList<>();
    private Messenger mMessenger;
    private MediaPlayer mPlayer;
    private MusicBroadReceiver receiver;
    private int mCurrentPosition;
    private boolean isFirst = true;
    private int mPosition;
    public static int playMode = 2;//1.单曲循环 2.列表循环 0.随机播放
    private Random mRandom = new Random();
    public static int prv_position;
    private Message mMessage;
    private static boolean isLoseFocus;
    private NotificationManager notificationManager;

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
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
        Log.i(TAG, "onDestroy: ");
        notificationManager.cancel(Constants.NOTIFICATION_CEDE);
        mMessage = Message.obtain();
        mMessage.what = Constants.MSG_CANCEL;
        try {
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        stopSelf();
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
            sentPreparedMessageToMain();
            sentPositionToMainByTimer();
        }
    }

    private void sentPreparedMessageToMain() {
        mMessage = new Message();
        mMessage.what = Constants.MSG_PREPARED;
        mMessage.arg1 = mPosition;
        mMessage.obj = mPlayer.isPlaying();
        try {
            //发送播放位置
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentPlayStateToMain() {
        mMessage = Message.obtain();
        mMessage.what = Constants.MSG_PLAY_STATE;
        mMessage.obj = mPlayer.isPlaying();
        try {
            //发送播放状态
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentPositionToMainByTimer() {
        ThreadPoolUtil.getScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPlayer.isPlaying()) {
                        //1.准备好的时候.告诉activity,当前歌曲的总时长
                        int currentPosition = mPlayer.getCurrentPosition();
                        int totalDuration = mPlayer.getDuration();
                        mMessage = Message.obtain();
                        mMessage.what = Constants.MSG_PROGRESS;
                        mMessage.arg1 = currentPosition;
                        mMessage.arg2 = totalDuration;
                        //2.发送消息
                        mMessenger.send(mMessage);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
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
            sentPlayStateToMain();
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
        intentFilter.addAction(Constants.ACTION_CLOSE);
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
                    Log.i(TAG, "onReceive: ACTION_LIST_ITEM");
                    //点击左侧菜单
                    isFirst = false;
                    mPosition = intent.getIntExtra("position", 0);
                    play(mPosition);
                    break;
                case Constants.ACTION_PAUSE:
                    Log.i(TAG, "onReceive: ACTION_PAUSE");
                    //暂停播放
                    pause();
                    break;
                case Constants.ACTION_PLAY:
                    Log.i(TAG, "onReceive: ACTION_PLAY");
                    //开始播放
                    if (isFirst) {
                        isFirst = false;
                        play(mPosition);
                    } else {
                        mPlayer.seekTo(mCurrentPosition);
                        mPlayer.start();
                        //通知是否在播放
                        sentPlayStateToMain();
                    }
                    break;
                case Constants.ACTION_NEXT:
                    Log.i(TAG, "onReceive: ACTION_NEXT");
                    //下一首
                    prv_position = mPosition;
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
                    Log.i(TAG, "onReceive: ACTION_PRV");
                    //上一首
                    prv_position = mPosition;
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
                case Constants.ACTION_CLOSE:
                    Log.i(TAG, "onReceive: ACTION_CLOSE");
                    onDestroy();
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    Log.i(TAG, "onReceive: ACTION_AUDIO_BECOMING_NOISY");
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
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_GAIN---------------");
                // resume playback
                if (isLoseFocus) {
                    isLoseFocus = false;
                    mPlayer.start();
                    mPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS://你已经失去了音频焦点很长时间了。你必须停止所有的音频播放
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS---------------");
                // Lost focus for an unbounded amount of time: stop playback and release media player
                isLoseFocus = false;
                if (mPlayer.isPlaying())
                    mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://你暂时失去了音频焦点
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS_TRANSIENT---------------");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying()) {
                    isLoseFocus = true;
                    mPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK---------------");
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying()) {
                    isLoseFocus = true;
                    mPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }

    }

}
