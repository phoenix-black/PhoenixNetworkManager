package com.blackphoenix.phoenixnetworkmanager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Praba on 2/4/2018.
 */
public class PxSignalStrength {
    public String type;
    public String strength;

    @Override
    public String toString() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type",""+type);
            jsonObject.put("strength",""+strength);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{\"type\":\""+type +"\","
                    + "\"strength\":\""+strength+"\"}";
        }

    }

}
