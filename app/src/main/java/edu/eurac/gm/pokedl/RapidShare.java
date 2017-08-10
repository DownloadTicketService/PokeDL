package edu.eurac.gm.pokedl;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class RapidShare extends AppCompatActivity {
    private String uploaddescription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if ((Intent.ACTION_SEND_MULTIPLE.equals(action) || Intent.ACTION_SEND.equals(action)) && type != null) {

            ArrayList<Uri> SharedUris;
            uploaddescription = "";
            if(Intent.ACTION_SEND_MULTIPLE.equals(action)){
                SharedUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }else{
                SharedUris = new ArrayList<Uri>();
                SharedUris.add((Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM));
            }

            ArrayList<File> fileArrayList = new ArrayList<>();
            String username,password,defaultserverurl;
            Context ctx = getApplicationContext();
            try {

                CredStoreManager csm = new CredStoreManager(ctx);
                username = csm.fetchDefaultUsername(ctx);
                password = csm.fetchDefaultPassword(ctx);
                defaultserverurl = csm.fetchDefaultServerurl(ctx);

            }catch(Exception e){
                Toast.makeText(ctx, "Couldn't load credentials", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            for(Uri furi : SharedUris) {
                String fpath = SharedFileResolver.getPath(ctx, furi);
                if(fpath != null){
                    fileArrayList.add(new File(fpath));
                    uploaddescription = fpath.substring(fpath.lastIndexOf("/")+1);
                }
            }

            File[] sharedfiles = new File[fileArrayList.size()];
            fileArrayList.toArray(sharedfiles);
            if(fileArrayList.size() > 1){
                uploaddescription = "Multiple files";
            }

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();

            DLManager.uploadFiles(defaultserverurl,username,password,sharedfiles,this);
            finish();
        }
    }

    public void uploadFailed(int statusCode) {
    }

    public void uploadProgress(int pert) {
        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setContentTitle(uploaddescription).setContentText("uploading").setSmallIcon(android.R.drawable.ic_menu_upload).setProgress(1000, pert, false);
        mNotifyManager.notify(16, mBuilder.build());
    }

    public void uploadCompleted(String url) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
        sendIntent.setType("text/plain");
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        sendIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setContentTitle(uploaddescription).setContentText("upload complete, click to (re)share").setSmallIcon(android.R.drawable.ic_menu_upload).setProgress(0,0,false);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyManager.notify(16, mBuilder.build());
        shareUrl(url);
    }

    protected void shareUrl(String url){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
