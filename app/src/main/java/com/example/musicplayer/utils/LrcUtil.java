package com.example.musicplayer.utils;

import com.example.musicplayer.MainActivity;

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

/**
 * 专门用来解析歌词，.lrc;.txt;
 */
public class LrcUtil {
    private static final String TAG = "LRCUtils";
    MainActivity activity;
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
     *
     * @param f
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

    public void ReadLRCAndCconvertFile(File file) {
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
     *
     * @param LRCText
     * @return
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
     *
     * @param file
     * @return
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
                        activity.setMiniLrc(lrcString);//调用activity里面的方法
                    }
            }
        }
    }
}
