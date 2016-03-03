package com.app.coolweaher.util;

/**
 * Created by liuyang on 2016/3/2.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
