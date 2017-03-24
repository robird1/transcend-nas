package com.transcend.nas.tutk;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkGetNasLoader extends TutkBasicLoader {
    private String mToken;
    private List<TutkNasNode> nasList;
    private Context mContext;

    public static class TutkNasNode {
        public String nasID;
        public String nasUUID;
        public String nasName;
    }

    public TutkGetNasLoader(Context context, String server, String token) {
        super(context, server);
        mContext = context;
        mToken = token;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl();
        String result = doGetRequest(url, mToken);
        boolean success = doParserResult(result);
        return success;
    }

    @Override
    protected boolean doParserResult(String result) {
        boolean success = doErrorParser(result);
        if (success) {
            nasList = new ArrayList<TutkNasNode>();
            try {
                JSONArray dataArray = new JSONArray(result);
                for (int i = 0; i < dataArray.length(); i++) {
                    TutkNasNode node = new TutkNasNode();
                    node.nasID = dataArray.getJSONObject(i).getString("nasId");
                    node.nasUUID = dataArray.getJSONObject(i).getString("uid");
                    node.nasName = dataArray.getJSONObject(i).getString("name");
                    nasList.add(node);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    @Override
    protected String doGenerateUrl() {
        String url = getServer() + "/nas/list";
        return url;
    }

    public List<TutkNasNode> getNasList(){
        return nasList;
    }

    public ArrayList<HashMap<String, String>> getNasArrayList() {
        ArrayList<HashMap<String, String>> mNASList = new ArrayList<HashMap<String, String>>();
        for(TutkGetNasLoader.TutkNasNode node : nasList) {
            HashMap<String, String> nas = new HashMap<String, String>();
            nas.put("nasId", node.nasID);
            nas.put("nickname", node.nasName);
            nas.put("hostname", node.nasUUID);
            if(mNASList.contains(nas))
                continue;
            mNASList.add(nas);
        }

        return mNASList;
    }

    public String getToken(){
        return mToken;
    }
}
