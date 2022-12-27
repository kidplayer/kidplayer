package com.github.kidplayer.comm;

public interface HttpCallback {
    void onSuccess(int req_id, String method, String result);

    void onError(String toString);
}
