package com.realtek.nasfun.api;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.BackUpRequest.BackUpGetRequest;
import com.realtek.nasfun.api.BackUpRequest.BackUpGetRequest.Request;
import com.realtek.nasfun.api.BackUpRequest.BackUpSetRequest;
import com.realtek.nasfun.api.BackUpRequest.BackUpSetRequest.Request.Toggle;
import com.realtek.nasfun.api.BackUpResponse.BackUpGetResponse;
import com.realtek.nasfun.api.BackUpResponse.BackUpSetResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by phyllis on 2015/7/20.
 */

public class FileSystemManager {
    private static final String TAG = FileSystemManager.class.getSimpleName();
    //smartphone API
    private final String BACK_UP_PROGRESS = "/api/file_system/backup";
    private Server server;
    public FileSystemManager(Server server) {
        this.server = server;
    }
    private ObjectMapper mapper;

    public FileSystemManager(Server server, boolean isBackUPServiceAvailable) {
        this.server = server;
        this.mapper = new ObjectMapper();
        if(isBackUPServiceAvailable){
            SimpleModule module = new SimpleModule();
            module.addDeserializer(BackUpGetResponse.class, new BackUpResponse.BackUpGetResponseDeserializer());
            mapper.registerModule(module);
        }
    }

    public BackupProInfo getBackUpProgress(){
        // generate rquest
        BackUpGetRequest backUpGetRequest = new BackUpGetRequest();
        BackUpGetRequest.Request request = new BackUpGetRequest.Request();
        backUpGetRequest.setRequest(request);
        // send request
        BackUpGetResponse response  = (BackUpGetResponse)sendJSONRequest(BACK_UP_PROGRESS, backUpGetRequest, BackUpGetResponse.class);
        // to process response
        return getBackUpProgressFromResponse(response);
    }

    private BackupProInfo getBackUpProgressFromResponse(BackUpGetResponse response){
        int status_code = -1;
        int progress = 0;
        BackupProInfo backupProInfo;

        if (response == null){
            Log.w(TAG, " can't get backup progress because response is null");
            backupProInfo = new BackupProInfo(status_code, progress);
        }else{
            Log.d(TAG, " response = "+response);
            status_code = response.getResponse().getStatus_code();
            if(status_code == 0)
                progress = response.getResponse().getData().get(0).getProgress();
            backupProInfo = new BackupProInfo(status_code, progress);
        }

        return backupProInfo;
    }

    public int startBackUp(){
        int status_code = -1;
        // generate rquest
        BackUpSetRequest backUpSetRequest= new BackUpSetRequest();
        BackUpSetRequest.Request request = new BackUpSetRequest.Request();
        ArrayList<Toggle> data = new ArrayList<Toggle>();
        BackUpSetRequest.Request.Toggle toggle = new BackUpSetRequest.Request.Toggle();
        toggle.setEnable("yes");
        data.add(0,toggle);
        request.setData(data);
        backUpSetRequest.setRequest(request);

        // send request
        BackUpSetResponse backUpResponse = (BackUpSetResponse)sendJSONRequest(BACK_UP_PROGRESS, backUpSetRequest, BackUpSetResponse.class);
        Log.d(TAG, "startBackUp(), response = " + backUpResponse);
        if(backUpResponse != null )
            if (backUpResponse.getResponse() != null)
                status_code = backUpResponse.getResponse().getStatus_code();
            else
                Log.e(TAG, "backUpResponse.response = null");
        else
            Log.e(TAG, "backUpResponse = null");
        return status_code;
    }

    public int stopBackUp(){
        int status_code = -1;
        // generate rquest
        BackUpSetRequest backUpSetRequest= new BackUpSetRequest();
        BackUpSetRequest.Request request = new BackUpSetRequest.Request();
        ArrayList<Toggle> data = new ArrayList<Toggle>();
        BackUpSetRequest.Request.Toggle toggle = new BackUpSetRequest.Request.Toggle();
        toggle.setEnable("no");
        data.add(0,toggle);
        request.setData(data);
        backUpSetRequest.setRequest(request);

        // send request
        BackUpSetResponse backUpResponse = (BackUpSetResponse)sendJSONRequest(BACK_UP_PROGRESS, backUpSetRequest, BackUpSetResponse.class);
        Log.d(TAG, "stopBackUp(), response = " + backUpResponse);
        if(backUpResponse != null )
            if (backUpResponse.getResponse() != null)
                status_code = backUpResponse.getResponse().getStatus_code();
            else
                Log.e(TAG, "backUpResponse.response = null");
        else
            Log.e(TAG, "backUpResponse = null");
        return status_code;
    }

    //  It's used to send POST data in JSON format to the
    //  resource uri and get response in JSON format.
    Object sendJSONRequest(String resUri, Object req, Class<?> t){
        String uri = "http://" + server.getHostname() + resUri;
        Log.d(TAG, "sendJSONRequest to uri:"+uri);
        String reqString = null;
        String rspString = null;

        try {
            do{
                reqString = mapper.writeValueAsString(req);
                Log.d(TAG, "reqString  =" +reqString);
                StringEntity strEntity = new StringEntity(reqString, "UTF-8");

                // prepare http post request
                HttpPost httpPost = new HttpPost(uri);
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setEntity(strEntity);
                DefaultHttpClient httpClient = HttpClientManager.getClient();
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity rspEntity = httpResponse.getEntity();
                String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.UTF_8;
                }
                rspString = EntityUtils.toString(rspEntity, inputEncoding);
                Log.d(TAG,"rspString = "+rspString);
                return mapper.readValue(rspString,  t);
            }while(true);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            Log.d(TAG, "Request = " + reqString);
            Log.d(TAG, "Response = " + rspString);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        return null;
    }

}
