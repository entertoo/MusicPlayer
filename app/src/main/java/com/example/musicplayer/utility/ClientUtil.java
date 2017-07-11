package com.example.musicplayer.utility;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ClientUtil {

    private static OkHttpClient mClient;

    public static OkHttpClient getOkHttpClient() {
        if (mClient == null) {
            mClient = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                    .writeTimeout(10, TimeUnit.SECONDS)//设置写的超时时间
                    .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                    .build();
        }
        return mClient;
    }

}
