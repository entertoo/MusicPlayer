package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.example.musicplayer.bean.Mp3Info;
import com.example.musicplayer.service.MusicService;
import com.example.musicplayer.utils.Constants;
import com.example.musicplayer.utils.LrcUtil;
import com.example.musicplayer.utils.MusicUtil;
import com.example.musicplayer.view.SlidingMenu;
import com.example.musicview.MusicPlayerView;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MusicPlayerView mpv;
    private ListView mListView;
    private ImageView mNext;
    private ImageView mPrevious;
    private List<Mp3Info> mMusic_list;
    private ImageView mIv_back;
    private SlidingMenu mSlidingMenu;
    private TextView mCurrentLrc;
    private TextView mSong;
    private TextView mSinger;
    private ImageView mPlayMode;
    private int mPosition;

    private RemoteViews remoteViews;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Mp3Info mMp3Info;
    private String mSongTitle;
    private String mSingerArtist;
    private Bitmap mBitmap;
    private LrcUtil mLrcUtil;// 歌词工具

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MSG_ONPREPARED) {
                int currentPosition = msg.arg1;
                int totalDuration = msg.arg2;
                mpv.setProgress(currentPosition);
                mpv.setMax(totalDuration);

                if (mLrcUtil == null) {
                    mLrcUtil = new LrcUtil(MainActivity.this);
                }
                // 序列化歌词
                File file = MusicUtil.getLrcFile(mMusic_list.get(mPosition).getUrl());
                if(file != null){
                    mLrcUtil.ReadLRCAndCconvertFile(file);
                    // 使用功能
                    mLrcUtil.RefreshLRC(currentPosition);
                    // 1. 设置集合
                    //mTv_lyricShow.SetTimeLrc(LrcUtil.lrclist);
                    // 2. 更新滚动歌词
                    //mTv_lyricShow.SetNowPlayIndex(currentPosition);
                }
            }
            if (msg.what == Constants.MSG_PREPARED) {
                mPosition = msg.arg1;
                refreshPlayUI(mPosition, true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initData();
        initEvent();
    }

    private void initView() {
        // 去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mSlidingMenu = (SlidingMenu) findViewById(R.id.sm);
        // left
        mListView = (ListView) findViewById(R.id.listview);
        // content
        mIv_back = (ImageView) findViewById(R.id.back);//切换左右控件
        mSong = (TextView) findViewById(R.id.textViewSong);//歌名
        mSinger = (TextView) findViewById(R.id.textViewSinger);//歌手
        mCurrentLrc = (TextView) findViewById(R.id.lrc);//当前歌词
        mpv = (MusicPlayerView) findViewById(R.id.mpv);//自定义播放控件
        mPrevious = (ImageView) findViewById(R.id.previous);//上一首
        mPlayMode = (ImageView) findViewById(R.id.play_mode);//播放模式
        mNext = (ImageView) findViewById(R.id.next);//下一首
        remoteViews = new RemoteViews(getPackageName(), R.layout.customnotice);
    }

    private void initData() {
        //音乐列表
        mMusic_list = MusicUtil.getMp3Infos(this);
        //启动音乐服务
        startMusicService();
        //消息通知
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mListView.setAdapter(new MusicListAdapter());
        //初始化控件UI
        refreshPlayUI(mPosition, false);
    }

    /**
     * 开始音乐服务
     */
    private void startMusicService() {
        Intent musicService = new Intent();
        musicService.setClass(getApplicationContext(), MusicService.class);
        //musicService.putParcelableArrayListExtra("music_list", (ArrayList<? extends Parcelable>) mMusic_list);
        musicService.putExtra("messenger", new Messenger(handler));
        startService(musicService);
    }

    /**
     * 刷新播放控件的歌名，歌手，图片，按钮的形状
     */
    private void refreshPlayUI(int position, boolean canRotating) {
        mMp3Info = mMusic_list.get(position);
        mSongTitle = mMp3Info.getTitle();
        mSingerArtist = mMp3Info.getArtist();
        mBitmap = MusicUtil.getArtwork(MainActivity.this, mMp3Info.getId(), mMp3Info.getAlbumId(), true, false);
        mSong.setText(mSongTitle);
        mSinger.setText(mSingerArtist);
        mCurrentLrc.setText("暂无歌词");
        mpv.setCoverBitmap(mBitmap);
        // 选中左侧播放中的歌曲颜色
        changeColorSelected();
        // 播放控件
        if (canRotating) {
            if (!mpv.isRotating()) {
                mpv.start();
            }
        }

        // 创建并设置通知栏中remoteViews的图片文字
        remoteViews.setImageViewBitmap(R.id.widget_album, mBitmap);
        remoteViews.setTextViewText(R.id.widget_title, mMp3Info.getTitle());
        remoteViews.setTextViewText(R.id.widget_artist, mMp3Info.getArtist());
        if (MusicService.isPlaying) {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_btn_pause_normal);
        } else {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_btn_play_normal);
        }
        // 创建并设置通知栏
        setNotification();
    }

    /**
     * 设置通知
     */
    @SuppressLint("NewApi")
    private void setNotification() {
        mBuilder = new NotificationCompat.Builder(this);
        // 点击跳转到主界面
        Intent intent_main = new Intent(this, MainActivity.class);
        PendingIntent pending_intent_go = PendingIntent.getActivity(this, 1, intent_main, PendingIntent.FLAG_UPDATE_CURRENT);
        //remoteViews.setOnClickPendingIntent(R.id.notice, pending_intent_go);

        // 4个参数context, requestCode, intent, flags
        PendingIntent pending_intent_close = PendingIntent.getActivity(this, 2, intent_main, PendingIntent.FLAG_UPDATE_CURRENT);
        //remoteViews.setOnClickPendingIntent(R.id.widget_close, pending_intent_close);

        // 设置上一曲
        Intent intent_prv = new Intent();
        intent_prv.setAction(Constants.ACTION_PRV);
        PendingIntent pending_intent_prev = PendingIntent.getBroadcast(this, 3, intent_prv, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev, pending_intent_prev);

        // 设置播放
        if (MusicService.isPlaying) {//如果正在播放——》暂停
            Intent intent_playorpause = new Intent();
            intent_playorpause.setAction(Constants.ACTION_PAUSE);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 4, intent_playorpause, PendingIntent.FLAG_UPDATE_CURRENT);
            //remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        if (!MusicService.isPlaying) {//如果暂停——》播放
            Intent intent_playorpause = new Intent();
            intent_playorpause.setAction(Constants.ACTION_PLAY);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 5, intent_playorpause, PendingIntent.FLAG_UPDATE_CURRENT);
            //remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }

        // 下一曲
        Intent intent_next = new Intent();
        intent_next.setAction(Constants.ACTION_NEXT);
        PendingIntent pending_intent_next = PendingIntent.getBroadcast(this, 6, intent_next, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_next, pending_intent_next);

        //mBuilder.setSmallIcon(R.drawable.notification_bar_icon); // 设置顶部图标（状态栏）

        Notification notification = mBuilder.build();
        notification.contentView = remoteViews; // 设置下拉图标
        notification.bigContentView = remoteViews; // 防止显示不完全,需要添加apisupport
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notification.icon = R.drawable.notification_bar_icon;

        mNotificationManager.notify(100, notification);
    }

    private void initEvent() {
        mIv_back.setOnClickListener(this);
        mpv.setOnClickListener(this);
        mPrevious.setOnClickListener(this);
        mPlayMode.setOnClickListener(this);
        mNext.setOnClickListener(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //点击左侧菜单
                changeColorNormal();
                sendBroadcast(Constants.ACTION_LIST_ITEM, i);
                mSlidingMenu.switchMenu(false);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back://切换左右布局
                mSlidingMenu.toggle();
                break;
            case R.id.mpv://自定义播放控件，点击播放或暂停
                if (mpv.isRotating()) {
                    mpv.stop();
                    sendBroadcast(Constants.ACTION_PAUSE);
                } else {
                    mpv.start();
                    sendBroadcast(Constants.ACTION_PLAY);
                }
                break;
            case R.id.previous://上一首
                changeColorNormal();
                sendBroadcast(Constants.ACTION_PRV);
                break;
            case R.id.play_mode://切换播放模式
                MusicService.playMode++;
                switch (MusicService.playMode % 3) {
                    case 0://随机播放
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_shuffle_normal);
                        break;
                    case 1://单曲循环
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_loopsingle_normal);
                        break;
                    case 2://列表播放
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_playall_normal);
                        break;
                }
                break;
            case R.id.next://下一首
                changeColorNormal();
                sendBroadcast(Constants.ACTION_NEXT);
                break;
        }
    }

    /**
     * 发送广播
     *
     * @param action
     */
    private void sendBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        sendBroadcast(intent);
    }

    /**
     * 发送广播
     *
     * @param action
     */
    private void sendBroadcast(String action, int position) {
        Intent intent = new Intent();
        intent.putExtra("position", position);
        intent.setAction(action);
        sendBroadcast(intent);
    }

    /**
     * 左侧音乐列表适配器
     */
    public class MusicListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMusic_list.size();
        }

        @Override
        public Object getItem(int position) {
            return mMusic_list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(MainActivity.this, R.layout.music_listitem, null);
                holder.mImgAlbum = (ImageView) convertView.findViewById(R.id.img_album);
                holder.mTvTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.mTvArtist = (TextView) convertView.findViewById(R.id.tv_artist);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mImgAlbum.setImageBitmap(MusicUtil.getArtwork(MainActivity.this, mMusic_list.get(position).getId(), mMusic_list.get(position).getAlbumId(), true, true));
            holder.mTvTitle.setText(mMusic_list.get(position).getTitle());
            holder.mTvArtist.setText(mMusic_list.get(position).getArtist());

            if (mPosition == position) {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorAccent));
            } else {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorNormal));
            }
            System.out.println("mPosition : " + mPosition + ",  position : " + position );

            holder.mTvTitle.setTag(position);

            return convertView;
        }
    }

    public class ViewHolder {
        ImageView mImgAlbum;
        TextView mTvTitle;
        TextView mTvArtist;
    }

    public void changeColorNormal() {
        TextView tv = (TextView) mListView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorNormal));
        }
    }

    public void changeColorSelected() {
        TextView tv = (TextView) mListView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }

    /**
     * 修改minilrc的文本
     */
    public void setMiniLrc(String lrcString) {
        mCurrentLrc.setText(lrcString);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remoteViews != null) {
            mNotificationManager.cancel(100);
        }
    }
}
