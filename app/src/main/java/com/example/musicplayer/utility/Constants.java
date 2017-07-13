package com.example.musicplayer.utility;

/**
 * @Author haopi
 * @Date 2016-09-06
 * @Des TODO
 */
public class Constants {
    //musicservice的name
    public static final String MUSIC_SERVICE = "com.example.musicplayer.service.MusicService";
    //本地歌曲listview点击
    public static final String ACTION_LIST_ITEM = "com.example.musicplayer.listitem";
    //暂停音乐
    public static final String ACTION_PAUSE = "com.example.musicplayer.pause";
    //播放音乐
    public static final String ACTION_PLAY = "com.example.musicplayer.play";
    //下一曲
    public static final String ACTION_NEXT = "com.example.musicplayer.next";
    //上一曲
    public static final String ACTION_PRV = "com.example.musicplayer.prv";

    public static final String ACTION_CLOSE = "com.example.musicplayer.close";
    //seekbar手动控制
    public static final String ACTION_SEEK ="com.example.musicplayer.seek";
    //以上操作结束的时候
    public static final String ACTION_COMPLETION = "com.example.musicplayer.completion";

    public static final int MSG_PROGRESS = 001;
    public static final int MSG_PREPARED = 002;
    public static final int MSG_PLAY_STATE = 003;
    // 取消
    public static final int MSG_CANCEL = 004;

    public static final String URL_GET_MUSIC_ID = "http://s.music.163.com/search/get/?src=lofter&type=1&filterDj=true&s=";
    public static final String URl_GET_MUSIC_LRC = "http://music.163.com/api/song/media?id=";

    public static final int NOTIFICATION_CEDE = 100;
}
