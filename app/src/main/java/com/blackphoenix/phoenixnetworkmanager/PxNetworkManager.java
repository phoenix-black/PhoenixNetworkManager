package com.blackphoenix.phoenixnetworkmanager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.blackphoenix.phoenixwidgets.CustomProgressDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Praba on 12/19/2017.
 *
 * ToDo:
 * 1. Add Location Permission dialog (TO the cellinfo permission check method)
 *
 */
public class PxNetworkManager {

    private static String LOG_TITLE = PxNetworkManager.class.getSimpleName();

    public static String E_CONNECTIVITY_SERVICE_INIT_FAILED = "E0311";
    public static String E_TELEPHONY_SERVICE_INIT_FAILED = "E0312";
    public static String E_HOST_NOT_REACHABLE_300 = "E0313";
    public static String E_HOST_IO_EXCEPTION = "E0314";
    public static String E_NO_CELL_INFO = "E0315";
    public static String E_UNKNOWN = "E031000";

    private Context context;
    private TelephonyManager mTelephonyManager;
    private int signalStrength;
    private PhoneStateListener phoneStateListener;
    private SignalStrengthListener signalStrengthListener;

    private static int SIGNAL_STRENGTH_GOOD = 3;
    private static int SIGNAL_STRENGTH_GREAT = 4;
    private static int SIGNAL_STRENGTH_MODERATE = 2;
    private static int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    private static int SIGNAL_STRENGTH_POOR = 1;

    public interface SignalStrengthListener {
        void onSignalStrengthChanged(int signalStrength, List<PxSignalStrength> signalLevelList);
    }


    public PxNetworkManager(@NonNull final Context context) {
        this.context = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                List<PxSignalStrength> signalStrengthList = null;

                try {
                    signalStrengthList = getSignalStrengthLevel(context);
                } catch (PxNetworkException e){

                }

                if (signalStrength.getGsmSignalStrength() == 0 || signalStrength.getGsmSignalStrength() == 99) {
                    PxNetworkManager.this.signalStrength = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
                    if (signalStrengthListener != null) {
                        signalStrengthListener.onSignalStrengthChanged(SIGNAL_STRENGTH_NONE_OR_UNKNOWN, signalStrengthList);
                    }
                } else if (signalStrength.getGsmSignalStrength() > 12) {
                    PxNetworkManager.this.signalStrength = SIGNAL_STRENGTH_GREAT;
                    if (signalStrengthListener != null) {
                        signalStrengthListener.onSignalStrengthChanged(SIGNAL_STRENGTH_GREAT,signalStrengthList);
                    }
                } else if (signalStrength.getGsmSignalStrength() > 8) {
                    PxNetworkManager.this.signalStrength = SIGNAL_STRENGTH_GOOD;
                    if (signalStrengthListener != null) {
                        signalStrengthListener.onSignalStrengthChanged(SIGNAL_STRENGTH_GOOD,signalStrengthList);
                    }
                } else if (signalStrength.getGsmSignalStrength() >= 5) {
                    PxNetworkManager.this.signalStrength = SIGNAL_STRENGTH_MODERATE;
                    if (signalStrengthListener != null) {
                        signalStrengthListener.onSignalStrengthChanged(SIGNAL_STRENGTH_MODERATE,signalStrengthList);
                    }
                } else if (signalStrength.getGsmSignalStrength() < 5) {
                    PxNetworkManager.this.signalStrength = SIGNAL_STRENGTH_POOR;
                    if (signalStrengthListener != null) {
                        signalStrengthListener.onSignalStrengthChanged(SIGNAL_STRENGTH_POOR,signalStrengthList);
                    }
                }
            }
        };

    }

    /*
        till of V1.0
     */
    public void registerSignalStrengthListener() {
        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        // ToDo : Handle the else Part Here
    }

    /*
        Above V1.0
     */
    public void registerSignalStrengthListener(SignalStrengthListener listener) {
        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        // ToDo : Handle the else Part Here

        this.signalStrengthListener = listener;
    }

    public void unRegisterSignalStrengthListener() {
        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        // ToDo : Handle the else Part Here
    }


    public int getSignalStrength() {
        return signalStrength;
    }

    public void destroy() {
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            mTelephonyManager = null;
        }
    }



    public static boolean isConnected(@NonNull Context context) throws PxNetworkException {
        return isConnected(context,false);
    }

    public static boolean isConnected(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connManager == null){
            if(isDebug){
                Log.e(LOG_TITLE,"Connection Manager is Null. getSystemService() Failed");
            }
            throw new PxNetworkException("Internal Error! Unable to get Connectivity Manager Instance",E_CONNECTIVITY_SERVICE_INIT_FAILED);
        }

        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }


       /*
        Get the Type of Network Connected to (WiFi/Mobile/Ethernet/Bluetooth)
        @Parm: Context
        @Ret: Boolean
        // https://developer.android.com/reference/android/telephony/TelephonyManager.html
     */

    public static String getNetworkType(@NonNull Context context) throws PxNetworkException {
        return getNetworkType(context,false);
    }

    public static String getNetworkType(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        if (isConnected(context)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // toDo : Handle Null pointer Exception
            if(connManager == null){

                if(isDebug){
                    Log.e(LOG_TITLE,"Connection Manager is Null. getSystemService() Failed");
                }

                throw new PxNetworkException("Internal Error! Unable to get Connectivity Manager Instance",E_CONNECTIVITY_SERVICE_INIT_FAILED);
            }

            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return "WiFi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "Mobile";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "Ethernet";
                case ConnectivityManager.TYPE_BLUETOOTH:
                    return "Bluetooth";
                default:
                    return "Unknown Network";
            }
        }

        if(isDebug){
            Log.e(LOG_TITLE,"Not Connected to Any Network. isConnected returned false");
        }

        return "No Network Available";
    }


    public static void isConnectionStrong(@NonNull final Context context, final PxNetworkConnectivity networkInterface) throws PxNetworkException, IOException {
        isConnectionStrong(context,networkInterface,false);
    }

    public static void isConnectionStrong(@NonNull final Context context, final PxNetworkConnectivity networkInterface, final boolean isDebug) throws PxNetworkException, IOException {

        if (isConnected(context)) {

            final CustomProgressDialog pa_progressDialog = new CustomProgressDialog(context, R.style.ProgressDialogTheme);
            pa_progressDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // ToDo: Replace google with kodwell
                        if (InetAddress.getByName("www.kodwell.co.uk").isReachable(100)) {

                            if(isDebug)
                                Log.e("NW MNGR", "Internet Connection is Strong");

                            if(networkInterface!=null)
                                networkInterface.onFinished(100);

                        } else if(InetAddress.getByName("www.kodwell.co.uk").isReachable(300)) {

                            if(isDebug)
                                Log.e("NW MNGR", "Internet Connection is Strong");

                            if(networkInterface!=null)
                                networkInterface.onFinished(300);

                        } else {

                            if(isDebug)
                                Log.e("NW MNGR", "Internet Connection is Weak");

                            if(networkInterface!=null)
                                networkInterface.onError("No or Weak Internet Connection",
                                        "Test Connection failed : Unable to Reach the host kodwell.co.uk in 300 ms",
                                        E_HOST_NOT_REACHABLE_300);
                        }
                    } catch (IOException e) {
                        //e.printStackTrace();
                        if(isDebug)
                            Log.e("NW MNGR", "IO Exception " + e.toString());

                        if(networkInterface!=null)
                            networkInterface.onError("Internal Error! Checking Connection strength Failed",e.toString(),E_HOST_IO_EXCEPTION);
                    }

                    if (pa_progressDialog.isShowing()) {
                        pa_progressDialog.dismiss();
                    }

                }
            }).start();

        }
    }



    public static String getCountryCode(@NonNull Context context) throws PxNetworkException {
        return getCountryCode(context,false);
    }


    public static String getCountryCode(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if(telephonyManager==null){
            if(isDebug)
                Log.e(LOG_TITLE,"Failed to Get Telephony Service. Null Value returned");

            throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
        }

        return telephonyManager.getSimCountryIso();
    }

    public static List<PxSignalStrength> getSignalStrengthDbm(@NonNull Context context) throws PxNetworkException {
        return getSignalStrengthDbm(context,false);
    }

    public static List<PxSignalStrength> getSignalStrengthDbm(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        try {
            List<PxSignalStrength> pxSignalStrengthList = new ArrayList<>();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if(telephonyManager==null){

                if(isDebug)
                    Log.e(LOG_TITLE,"Telephony Manager Init Failed. Null Value returned");

                throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
            }

            List<CellInfo> cellInfos = mGetCellInfo(context,telephonyManager);   //This will give info of all sims present inside your mobile

            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {

                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();

                            PxSignalStrength pxSignalStrengthWcdma = new PxSignalStrength();
                            pxSignalStrengthWcdma.type = "WCDMA";
                            pxSignalStrengthWcdma.strength = "" + cellSignalStrengthWcdma.getDbm();
                            pxSignalStrengthList.add(pxSignalStrengthWcdma);

                            if(isDebug)
                                Log.e(LOG_TITLE, "WCDMA : " + cellSignalStrengthWcdma.toString());

                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();

                            PxSignalStrength pxSignalStrengthGsm = new PxSignalStrength();
                            pxSignalStrengthGsm.type = "GSM";
                            pxSignalStrengthGsm.strength = "" + cellSignalStrengthGsm.getDbm();
                            pxSignalStrengthList.add(pxSignalStrengthGsm);

                            if(isDebug)
                                Log.e(LOG_TITLE, "GSM : " + cellSignalStrengthGsm.toString());


                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();


                            PxSignalStrength pxSignalStrengthLte = new PxSignalStrength();
                            pxSignalStrengthLte.type = "LTE";
                            pxSignalStrengthLte.strength = "" + cellSignalStrengthLte.getDbm();
                            pxSignalStrengthList.add(pxSignalStrengthLte);

                            if(isDebug)
                                Log.e(LOG_TITLE, "LTE : " + cellSignalStrengthLte.toString());


                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                            CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();

                            PxSignalStrength pxSignalStrengthCdma = new PxSignalStrength();
                            pxSignalStrengthCdma.type = "CDMA";
                            pxSignalStrengthCdma.strength = "" + cellSignalStrengthCdma.getDbm();

                            pxSignalStrengthList.add(pxSignalStrengthCdma);

                            if(isDebug)
                                Log.e(LOG_TITLE, "CDMA : " + cellSignalStrengthCdma.toString());

                        }
                    }
                }

                return (pxSignalStrengthList.size() > 0) ? pxSignalStrengthList : null;

            } else {
                if(isDebug)
                    Log.e(LOG_TITLE, "No Cell Network found");

                throw new PxNetworkException("Null Value for CellInfo. Check If Location Permission is Granted",E_NO_CELL_INFO);
            }
        } catch (Exception e) {
            if(isDebug)
                Log.e(LOG_TITLE, "Unexpected Error has occurred");

            throw new PxNetworkException(e.toString(),E_UNKNOWN);
        }

    }

    public static List<PxSignalStrength> getSignalStrengthLevel(@NonNull Context context) throws PxNetworkException {
        return getSignalStrengthLevel(context,false);
     }

    public static List<PxSignalStrength> getSignalStrengthLevel(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        try {
            List<PxSignalStrength> pxSignalStrengthList = new ArrayList<>();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if(telephonyManager==null){

                if(isDebug)
                    Log.e(LOG_TITLE,"Telephony Manager Init Failed. Null Value returned");

                throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
            }

            List<CellInfo> cellInfos = mGetCellInfo(context,telephonyManager);   //This will give info of all sims present inside your mobile

            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {

                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();

                            PxSignalStrength pxSignalStrengthWcdma = new PxSignalStrength();
                            pxSignalStrengthWcdma.type = "WCDMA";
                            pxSignalStrengthWcdma.strength = "" + cellSignalStrengthWcdma.getLevel();
                            pxSignalStrengthList.add(pxSignalStrengthWcdma);

                            if(isDebug)
                                Log.e(LOG_TITLE, "WCDMA Cell network found: ");



                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();

                            PxSignalStrength pxSignalStrengthGsm = new PxSignalStrength();
                            pxSignalStrengthGsm.type = "GSM";
                            pxSignalStrengthGsm.strength = "" + cellSignalStrengthGsm.getLevel();
                            pxSignalStrengthList.add(pxSignalStrengthGsm);

                            if(isDebug)
                                Log.e(LOG_TITLE, "GSM Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();


                            PxSignalStrength pxSignalStrengthLte = new PxSignalStrength();
                            pxSignalStrengthLte.type = "LTE";
                            pxSignalStrengthLte.strength = "" + cellSignalStrengthLte.getLevel();
                            pxSignalStrengthList.add(pxSignalStrengthLte);

                            if(isDebug)
                                Log.e(LOG_TITLE, "LTE Cell network found: ");

                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                            CellSignalStrengthCdma cellSignalStrengthLte = cellInfoCdma.getCellSignalStrength();


                            PxSignalStrength pxSignalStrengthCdma = new PxSignalStrength();
                            pxSignalStrengthCdma.type = "CDMA";
                            pxSignalStrengthCdma.strength = "" + cellSignalStrengthLte.getLevel();
                            pxSignalStrengthList.add(pxSignalStrengthCdma);

                            if(isDebug)
                                Log.e(LOG_TITLE, "CDMA Cell network found: " + i);

                        }
                    }
                }

                return (pxSignalStrengthList.size() > 0) ? pxSignalStrengthList : null;

            } else {
                if(isDebug)
                    Log.e(LOG_TITLE, "No Cell Network found");

                throw new PxNetworkException("Null Value for CellInfo. Check If Location Permission is Granted",E_NO_CELL_INFO);
            }
        } catch (Exception e) {
            throw new PxNetworkException(e.toString(),E_UNKNOWN);
        }
    }

    public static JSONArray getNetworkData(@NonNull Context context) throws PxNetworkException {
        return getNetworkData(context,false);
    }

    public static JSONArray getNetworkData(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        try {
            JSONArray networkDataList = new JSONArray();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if(telephonyManager==null){

                if(isDebug)
                    Log.e(LOG_TITLE,"Telephony Manager Init Failed. Null Value returned");

                throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
            }

            List<CellInfo> cellInfos = mGetCellInfo(context,telephonyManager);   //This will give info of all sims present inside your mobile

            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {

                            CellInfoWcdma cellInfo = (CellInfoWcdma) cellInfos.get(i);
                            CellSignalStrengthWcdma cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityWcdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "WCDMA");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("cid", cellIdentity.getCid());
                            networkIdentity.put("lac", cellIdentity.getLac());
                            networkIdentity.put("psc", cellIdentity.getPsc());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);


                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "WCDMA Cell network found: ");



                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfo = (CellInfoGsm) cellInfos.get(i);
                            CellSignalStrengthGsm cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityGsm cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "GSM");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("cid", cellIdentity.getCid());
                            networkIdentity.put("lac", cellIdentity.getLac());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "GSM Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfo = (CellInfoLte) cellInfos.get(i);
                            CellSignalStrengthLte cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityLte cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "LTE");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("ci", cellIdentity.getCi());
                            networkIdentity.put("pci", cellIdentity.getPci());
                            networkIdentity.put("tac", cellIdentity.getTac());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "LTE Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfo = (CellInfoCdma) cellInfos.get(i);
                            CellSignalStrengthCdma cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityCdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "CDMA");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("latitude", cellIdentity.getLatitude());
                            networkIdentity.put("longitude", cellIdentity.getLongitude());
                            networkIdentity.put("network_id", cellIdentity.getNetworkId());
                            networkIdentity.put("system_id", cellIdentity.getSystemId());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "CDMA Cell network found: " + i);

                        }
                    }
                }

                return (networkDataList.length() > 0) ? networkDataList : null;

            } else {
                if(isDebug)
                    Log.e(LOG_TITLE, "No Cell Network found");
                throw new PxNetworkException("Null Value for CellInfo. Check If Location Permission is Granted",E_NO_CELL_INFO);
            }
        } catch (Exception e) {
            if(isDebug)
                Log.e(LOG_TITLE, "Unknown Error Occurred  "+e.toString());

            throw new PxNetworkException(e.toString(),E_UNKNOWN);
        }
    }

    public static JSONArray getSignalData(@NonNull Context context) throws PxNetworkException {
        return getSignalData(context,false);
    }

    public static JSONArray getSignalData(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        try {
            JSONArray networkDataList = new JSONArray();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if(telephonyManager==null){

                if(isDebug)
                    Log.e(LOG_TITLE,"Telephony Manager Init Failed. Null Value returned");

                throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
            }

            List<CellInfo> cellInfos = mGetCellInfo(context,telephonyManager);   //This will give info of all sims present inside your mobile

            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {

                            CellInfoWcdma cellInfo = (CellInfoWcdma) cellInfos.get(i);
                            CellSignalStrengthWcdma cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityWcdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "WCDMA");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "WCDMA Cell network found: ");



                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfo = (CellInfoGsm) cellInfos.get(i);
                            CellSignalStrengthGsm cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityGsm cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "GSM");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "GSM Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfo = (CellInfoLte) cellInfos.get(i);
                            CellSignalStrengthLte cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityLte cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "LTE");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "LTE Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfo = (CellInfoCdma) cellInfos.get(i);
                            CellSignalStrengthCdma cellSignalStrength = cellInfo.getCellSignalStrength();
                            CellIdentityCdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "CDMA");

                            JSONObject networkStrength = new JSONObject();
                            networkStrength.put("asu", cellSignalStrength.getAsuLevel());
                            networkStrength.put("dbm", cellSignalStrength.getDbm());
                            networkStrength.put("level", cellSignalStrength.getLevel());
                            networkStrength.put("string", cellSignalStrength);
                            networkData.put("signal_info", networkStrength);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "CDMA Cell network found: " + i);

                        }
                    }
                }

                return (networkDataList.length() > 0) ? networkDataList : null;

            } else {

                if(isDebug)
                    Log.e(LOG_TITLE, "No Cell Network found");

                throw new PxNetworkException("Null Value for CellInfo. Check If Location Permission is Granted",E_NO_CELL_INFO);
            }
        } catch (Exception e) {

            if(isDebug)
                Log.e(LOG_TITLE, "Unknown Error Occurred  "+e.toString());

            throw new PxNetworkException(e.toString(),E_UNKNOWN);
        }
    }

    public static JSONArray getCellIdentity(@NonNull Context context) throws PxNetworkException {
        return getCellIdentity(context,false);
    }


    public static JSONArray getCellIdentity(@NonNull Context context, boolean isDebug) throws PxNetworkException {

        try {
            JSONArray networkDataList = new JSONArray();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if(telephonyManager==null){

                if(isDebug)
                    Log.e(LOG_TITLE,"Telephony Manager Init Failed. Null Value returned");

                throw new PxNetworkException("Internal Error! Unable to load telephony Service",E_TELEPHONY_SERVICE_INIT_FAILED);
            }

            List<CellInfo> cellInfos = mGetCellInfo(context,telephonyManager);   //This will give info of all sims present inside your mobile

            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {

                            CellInfoWcdma cellInfo = (CellInfoWcdma) cellInfos.get(i);
                            CellIdentityWcdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "WCDMA");
                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("cid", cellIdentity.getCid());
                            networkIdentity.put("lac", cellIdentity.getLac());
                            networkIdentity.put("psc", cellIdentity.getPsc());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "WCDMA Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfo = (CellInfoGsm) cellInfos.get(i);
                            CellIdentityGsm cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "GSM");

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("cid", cellIdentity.getCid());
                            networkIdentity.put("lac", cellIdentity.getLac());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);
                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "GSM Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfo = (CellInfoLte) cellInfos.get(i);
                            CellIdentityLte cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "LTE");

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("ci", cellIdentity.getCi());
                            networkIdentity.put("pci", cellIdentity.getPci());
                            networkIdentity.put("tac", cellIdentity.getTac());
                            networkIdentity.put("mcc", cellIdentity.getMcc());
                            networkIdentity.put("mnc", cellIdentity.getMnc());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "LTE Cell network found: ");


                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfo = (CellInfoCdma) cellInfos.get(i);
                            CellIdentityCdma cellIdentity = cellInfo.getCellIdentity();

                            JSONObject networkData = new JSONObject();
                            networkData.put("type", "CDMA");

                            JSONObject networkIdentity = new JSONObject();
                            networkIdentity.put("latitude", cellIdentity.getLatitude());
                            networkIdentity.put("longitude", cellIdentity.getLongitude());
                            networkIdentity.put("network_id", cellIdentity.getNetworkId());
                            networkIdentity.put("system_id", cellIdentity.getSystemId());
                            networkIdentity.put("string", cellIdentity);
                            networkData.put("cell_info", networkIdentity);

                            networkDataList.put(networkData);

                            if(isDebug)
                                Log.e(LOG_TITLE, "CDMA Cell network found: " + i);

                        }
                    }
                }

                return (networkDataList.length() > 0) ? networkDataList : null;

            } else {

                if(isDebug)
                    Log.e(LOG_TITLE, "No Cell Network found");

                throw new PxNetworkException("Null Value for CellInfo. Check If Location Permission is Granted",E_UNKNOWN);
            }
        } catch (Exception e) {

            if(isDebug)
                Log.e(LOG_TITLE, "Unknown Error Occurred  "+e.toString());

            throw new PxNetworkException(e.toString(),E_UNKNOWN);
        }
    }



    public static boolean isSIMReady(@NonNull Context context) throws PxNetworkException {

        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if(telMgr == null){
            throw new PxNetworkException("Null Exception: Unable to Initialize Telephony Manager ... ");
        }
        return (telMgr.getSimState() == TelephonyManager.SIM_STATE_READY);

/*
        int simState = telMgr.getSimState();
        switch (simState) {

            case TelephonyManager.SIM_STATE_ABSENT:
                // do something
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                // do something
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                // do something
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                // do something
                break;
            case TelephonyManager.SIM_STATE_READY:
                // do something
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                // do something
                break;
        }*/

    }


    private static List<CellInfo> mGetCellInfo(Context context, TelephonyManager telephonyManager) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        return telephonyManager.getAllCellInfo();
    }

}


