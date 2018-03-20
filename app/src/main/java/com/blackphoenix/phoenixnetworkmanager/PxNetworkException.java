package com.blackphoenix.phoenixnetworkmanager;

/**
 * Created by Praba on 02-02-2017.
 */

public class PxNetworkException extends Exception {
    private String _errorCode;

    public PxNetworkException(String message,String errorCode) {
        this(message);
        this._errorCode = errorCode;
    }

    public PxNetworkException(String message) {
        super("Hoyo Network Exception: "+message);
    }

    public void setErrorCode(String errorCode){
        this._errorCode = errorCode;
    }

    public String getErrorCode(){
        return _errorCode;
    }
}
