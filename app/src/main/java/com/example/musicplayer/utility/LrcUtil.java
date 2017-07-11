package com.example.musicplayer.utility;

import android.content.Context;
import android.util.Log;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.bean.Mp3Info;
import com.example.musicplayer.functions.Subscriber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.musicplayer.utility.Constants.URL_GET_MUSIC_ID;
import static com.example.musicplayer.utility.Constants.URl_GET_MUSIC_LRC;

/**
 * 专门用来解析歌词，.lrc;.txt;
 */
public class LrcUtil {
    private static final String TAG = "LRCUtils";
    private MainActivity activity;
    public static Vector<Timelrc> lrclist; //ArrayList
    private boolean IsLyricExist = false;
    private int lastLine = 0;

    public LrcUtil(MainActivity activityActivity) {
        activity = activityActivity;
    }

    public Vector<Timelrc> getLrcList() {
        return lrclist;
    }

    public void setNullLrcList() {
        lrclist = null;
    }

    /**
     * 序列号的入口
     */
    public void ReadLRC(File f) {
        try {
            if (f == null || !f.exists()) {
                IsLyricExist = false;
                lrclist = null;
                //				activity.setMiniLrc("歌词不存在");
            } else {
                //				activity.setMiniLrc("歌词存在");
                lrclist = new Vector<Timelrc>();
                IsLyricExist = true;
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                BufferedReader br = new BufferedReader(new InputStreamReader(is, GetCharset(f)));
                String strTemp = "";
                while ((strTemp = br.readLine()) != null) {
                    // Log.d(TAG,"strTemp = "+strTemp);
                    strTemp = AnalyzeLRC(strTemp);
                }
                br.close();
                is.close();
                // 对歌词进行排序
                Collections.sort(lrclist, new Sort());
                //	Collections.sort(lrclist);

                for (int i = 0; i < lrclist.size(); i++) {
                    Timelrc one = lrclist.get(i);
                    if (i + 1 < lrclist.size()) {
                        Timelrc two = lrclist.get(i + 1);
                        one.setSleepTime(two.getTimePoint() - one.getTimePoint());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void ReadLRCAndConvertFile(File file) {
        if (file == null || !file.exists()) {
            IsLyricExist = false;
            lrclist = null;
            //				activity.setMiniLrc("歌词不存在");
        } else {
            //				activity.setMiniLrc("歌词存在");
            lrclist = new Vector<Timelrc>();
            IsLyricExist = true;

            FileInputStream fis = null;
            BufferedInputStream bis = null;
            BufferedReader reader;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                bis.mark(4);
                byte[] first3bytes = new byte[3];
                //   System.out.println("");
                //找到文档的前三个字节并自动判断文档类型。
                bis.read(first3bytes);
                bis.reset();
                if (first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB && first3bytes[2] == (byte) 0xBF) {// utf-8

                    reader = new BufferedReader(new InputStreamReader(bis, "utf-8"));

                } else if (first3bytes[0] == (byte) 0xFF && first3bytes[1] == (byte) 0xFE) {

                    reader = new BufferedReader(new InputStreamReader(bis, "unicode"));
                } else if (first3bytes[0] == (byte) 0xFE && first3bytes[1] == (byte) 0xFF) {

                    reader = new BufferedReader(new InputStreamReader(bis, "utf-16be"));
                } else if (first3bytes[0] == (byte) 0xFF && first3bytes[1] == (byte) 0xFF) {

                    reader = new BufferedReader(new InputStreamReader(bis, "utf-16le"));
                } else {

                    reader = new BufferedReader(new InputStreamReader(bis, "GBK"));
                }

                String strTemp = "";
                while ((strTemp = reader.readLine()) != null) {
                    strTemp = AnalyzeLRC(strTemp);
                }

                // 对歌词进行排序
                Collections.sort(lrclist, new Sort());
                //	Collections.sort(lrclist);

                for (int i = 0; i < lrclist.size(); i++) {
                    Timelrc one = lrclist.get(i);
                    if (i + 1 < lrclist.size()) {
                        Timelrc two = lrclist.get(i + 1);
                        one.setSleepTime(two.getTimePoint() - one.getTimePoint());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 具体解析歌词
     */
    private String AnalyzeLRC(String LRCText) {
        try {
            int pos1 = LRCText.indexOf("[");
            int pos2 = LRCText.indexOf("]");

            if (pos1 == 0 && pos2 != -1) {
                Long time[] = new Long[GetPossiblyTagCount(LRCText)];
                time[0] = TimeToLong(LRCText.substring(pos1 + 1, pos2));
                if (time[0] == -1)
                    return ""; // LRCText
                String strLineRemaining = LRCText;
                int i = 1;
                while (pos1 == 0 && pos2 != -1) {

                    strLineRemaining = strLineRemaining.substring(pos2 + 1);
                    pos1 = strLineRemaining.indexOf("[");
                    pos2 = strLineRemaining.indexOf("]");
                    if (pos2 != -1) {
                        time[i] = TimeToLong(strLineRemaining.substring(pos1 + 1, pos2));
                        if (time[i] == -1)
                            return ""; // LRCText
                        i++;
                    }
                }

                Timelrc tl = new Timelrc();
                for (int j = 0; j < time.length; j++) {
                    if (time[j] != null) {
                        tl.setTimePoint(time[j].intValue());
                        tl.setLrcString(strLineRemaining);
                        lrclist.add(tl);
                        tl = new Timelrc();

                    }
                }
                return strLineRemaining;
            } else
                return "";
        } catch (Exception e) {
            return "";
        }
    }

    private int GetPossiblyTagCount(String Line) {
        String strCount1[] = Line.split("\\[");
        String strCount2[] = Line.split("\\]");
        if (strCount1.length == 0 && strCount2.length == 0)
            return 1;
        else if (strCount1.length > strCount2.length)
            return strCount1.length;
        else
            return strCount2.length;
    }

    public long TimeToLong(String Time) {
        try {
            String[] s1 = Time.split(":");
            int min = Integer.parseInt(s1[0]);
            String[] s2 = s1[1].split("\\.");
            int sec = Integer.parseInt(s2[0]);
            int mill = 0;
            if (s2.length > 1)
                mill = Integer.parseInt(s2[1]);
            return min * 60 * 1000 + sec * 1000 + mill * 10;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取文件的编码格式
     */
    public String GetCharset(File file) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1)
                return charset;
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF)
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    private class Sort implements Comparator<Timelrc> {
        public Sort() {
        }

        public int compare(Timelrc tl1, Timelrc tl2) {
            if (tl1.getTimePoint() < tl2.getTimePoint())
                return -1;
            else if (tl1.getTimePoint() > tl2.getTimePoint())
                return 1;
            else
                return 0;
        }
    }

    /**
     * 歌词的实体类
     *
     * @author yu
     */
    public static class Timelrc {
        /**
         * 歌词内容
         */
        private String lrcString;
        /**
         * 歌词显示多长时间
         */
        private int sleepTime;
        /**
         * 歌词时间点,也可以叫时间戳
         */
        private int timePoint;

        Timelrc() {
            lrcString = null;
            sleepTime = 0;
            timePoint = 0;
        }

        public void setLrcString(String lrc) {
            lrcString = lrc;
        }

        public void setSleepTime(int time) {
            sleepTime = time;
        }

        public void setTimePoint(int tPoint) {
            timePoint = tPoint;
        }

        public String getLrcString() {
            return lrcString;
        }

        public int getSleepTime() {
            return sleepTime;
        }

        public int getTimePoint() {
            return timePoint;
        }

        @Override
        public String toString() {
            return "Timelrc [lrcString=" + lrcString + ", sleepTime=" + sleepTime + ", timePoint=" + timePoint + "]";
        }
    }

    /**
     * 歌词的作用:显示指定位置的歌词
     *
     * @param current 当前的歌词进度
     */
    public void RefreshLRC(int current) {
        if (IsLyricExist) {
            for (int i = 1; i < lrclist.size(); i++) {
                if (current < lrclist.get(i).getTimePoint())
                    if (i == 1 || current >= lrclist.get(i - 1).getTimePoint()) {
                        lastLine = i - 1;
                        String lrcString = lrclist.get(i - 1).getLrcString();
                        //activity.setMiniLrc(lrcString);//调用activity里面的方法
                    }
            }
        }
    }

    /**
     * 请求音乐id
     */
    public static void getMusicId(final String name, final String singer, final Subscriber<Mp3Info> subscriber) {
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                OkHttpClient okHttpClient = ClientUtil.getOkHttpClient();
                Request request = new Request.Builder().get().url(URL_GET_MUSIC_ID + name).build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String s = response.body().string();
                        Log.i(TAG, "getMusicId-onResponse: " + s);
                        JSONObject jsonObject = string2Json(s);
                        Mp3Info mp3Info = new Mp3Info();
                        try {
                            JSONObject result = jsonObject.getJSONObject("result");
                            JSONArray songs = result.getJSONArray("songs");
                            for (int i = 0; i < songs.length(); i++) {
                                JSONObject song = (JSONObject) songs.get(i);
                                JSONArray artists = song.getJSONArray("artists");
                                JSONObject artist = (JSONObject) artists.get(0);
                                String singerName = artist.getString("name");
                                if (songs.length() == 1 || singer.contains(singerName) || singerName.contains(singer) || i == songs.length() - 1) {
                                    if(i == songs.length() - 1){
                                        song = (JSONObject) songs.get(0);
                                    }
                                    String songId = song.getString("id");
                                    String audio = song.getString("audio");
                                    JSONObject album = song.getJSONObject("album");
                                    String picUrl = album.getString("picUrl");
                                    mp3Info.setSongId(songId);
                                    mp3Info.setAudio(audio);
                                    mp3Info.setPicUrl(picUrl);
                                    mp3Info.setSongName(name);
                                    mp3Info.setArtist(singerName);
                                    subscriber.onComplete(mp3Info);
                                    break;
                                }
                            }
                            subscriber.onError(new Exception("no"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            subscriber.onError(e);
                        }
                    }
                });
            }
        });
    }

    /**
     * 请求音乐歌词
     */
    public static void getMusicLrc(final String name, final String singer, final Subscriber<String> subscriber) {
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                getMusicId(name, singer, new Subscriber<Mp3Info>() {
                    @Override
                    public void onComplete(Mp3Info mp3Info) {
                        Log.i(TAG, "getMusicLrc-onComplete: " + mp3Info.toString());
                        OkHttpClient okHttpClient = ClientUtil.getOkHttpClient();
                        Request request = new Request.Builder().get().url(URl_GET_MUSIC_LRC + mp3Info.getSongId()).build();
                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String s = response.body().string();
                                Log.i(TAG, "getMusicLrc-onResponse: " + s);
                                JSONObject jsonObject = string2Json(s);
                                try {
                                    String lrc = jsonObject.getString("lyric");
                                    subscriber.onComplete(lrc);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    subscriber.onError(e);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        subscriber.onError(e);
                    }
                });
            }
        });
    }

    public static JSONObject string2Json(String s) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
