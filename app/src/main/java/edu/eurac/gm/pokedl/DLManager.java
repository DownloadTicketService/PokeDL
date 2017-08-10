package edu.eurac.gm.pokedl;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.Base64;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.File;

import cz.msebera.android.httpclient.Header;

public class DLManager {

    public static void startCheckCredentials(final String servername, final String serverurl, final String username, final String password, final MainActivity callingactivity){
        AsyncHttpClient client = new AsyncHttpClient();
        final String xauthstr = username + ":" + password;
        String xauthstr64 = Base64.encodeToString(xauthstr.getBytes(),Base64.NO_WRAP);
        String xauthstr64h = "Basic "+xauthstr64;
        client.addHeader("Authorization",xauthstr64h);
        client.addHeader("X-Authorization",xauthstr64h);
        client.get(serverurl+"/info",new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String str = new String("");
                try {
                    str = new String(response, "UTF-8");

                }catch(Exception e){
                }
                try {
                    JSONObject jObject = new JSONObject(str);
                    callingactivity.credentialsVerified(servername,serverurl,username,password);

                }catch(Exception ex){
                    callingactivity.credentialsInvalid(0);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                //System.out.println(statusCode);
                callingactivity.credentialsInvalid(statusCode);
            }

            @Override
            public void onRetry(int retryNo) {
            }

            public void onFinish() {
            }
        });
    }

    public static void uploadFiles(final String serverurl, final String username, final String password, File[] fileArray, final RapidShare callingactivity){
        String xauthstr = username + ":" + password;
        String xauthstr64 = Base64.encodeToString(xauthstr.getBytes(),Base64.NO_WRAP);
        String xauthstr64h = "Basic "+xauthstr64;
        RequestParams params = new RequestParams();
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization",xauthstr64h);
        client.addHeader("X-Authorization",xauthstr64h);
        try {
            if(fileArray.length > 1){
                params.put("file[]", fileArray);
            }else {
                params.put("file", fileArray[0]);
            }
        }catch(Exception e){
            //System.out.println("ERR");
        }
        params.put("msg","{}");

        client.post(serverurl+"/newticket",params,new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                String str = new String("");
                try {
                    str = new String(response, "UTF-8");

                }catch(Exception e){
                    //System.out.println(e.getMessage());
                }
                try {
                    JSONObject jObject = new JSONObject(str);
                    String url = jObject.getString("url");
                    callingactivity.uploadCompleted(url);

                }catch(Exception ex){
                    callingactivity.uploadFailed(0);
                }
            }

            public void onProgress(long bytesWritten, long totalSize){
                float ratio = ((float)bytesWritten)/((float)totalSize);
                int pert = (int) (1000*ratio);
                callingactivity.uploadProgress(pert);
            }


            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                String str;
                callingactivity.uploadFailed(statusCode);
            }

            @Override
            public void onRetry(int retryNo) {
            }

            public void onFinish() {
            }
        });
    }
}
