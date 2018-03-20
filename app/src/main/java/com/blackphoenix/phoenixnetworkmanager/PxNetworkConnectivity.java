package com.blackphoenix.phoenixnetworkmanager;

/**
 * Created by Praba on 12/19/2017.
 */
public interface PxNetworkConnectivity {
    void onFinished(int timeMilliSecs);
    void onError(String userMessage, String errorMessage, String errorCode);
}
