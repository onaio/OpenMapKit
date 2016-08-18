package org.redcross.openmapkit.odkcollect;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.Settings;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class FormOSMDownloader extends AsyncTask<Void, Void, Void> {
    private static final int DEFAULT_TIMEOUT = 40000;
    private final Form form;
    private final String queryValue;
    private String server;
    private String username;
    private String password;
    private String query;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private int status = -1;
    private boolean downloading = false;
    private final OnFileDownload onFileDownload;
    private static ArrayList<Long> ongoingDownloads;

    public FormOSMDownloader(Context context, Form form, OnFileDownload onFileDownload, String queryValue) {
        this.form = form;
        this.onFileDownload = onFileDownload;
        this.queryValue = queryValue;
        server = Settings.singleton().getOSMFromODKServer();
        username = Settings.singleton().getOSMFromODKUsername();
        password = Settings.singleton().getOSMFromODKPassword();
        query = Settings.singleton().getOSMFromODKQuery();

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        downloading = true;

        //remove file from directory
        String destPath = ExternalStorage.getOSMDir() + String.valueOf(form.getId()) + ".osm";
        File destination = new File(destPath);
        if(destination.exists()) {
            destination.delete();
        }

        //start the new download
        String url = server + String.valueOf(form.getId()) + ".osm";
        if(query != null) {
            JSONObject queryObject = new JSONObject();
            try {
                String queryValue = this.queryValue;
                if(queryValue == null) queryValue = "";

                queryObject.put(query, queryValue);
                url = url + "?query="+ URLEncoder.encode(queryObject.toString(), "UTF-8");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        if(username != null && password != null) {
            String credentials = username + ":" + password;
            request.addRequestHeader("Authorization", "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.URL_SAFE));
        }
        request.setDestinationInExternalPublicDir(ExternalStorage.getOSMDirRelativeToExternalDir(), form.getId()+".osm");

        if(form.getName() == null || form.getName().trim().length() == 0) {
            request.setTitle(String.valueOf(form.getId()));
        } else {
            request.setTitle(form.getName());
        }

        downloadId = downloadManager.enqueue(request);
        if(ongoingDownloads == null) ongoingDownloads = new ArrayList<>();
        ongoingDownloads.add(downloadId);

        pollDownloadManager();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        downloading = false;
        if(ongoingDownloads.contains(downloadId)) {//this is not a download that has been force canceled because a new batch of downloads started
            if (status == DownloadManager.STATUS_SUCCESSFUL
                    && onFileDownload != null){
                ongoingDownloads.remove(downloadId);
                onFileDownload.onSuccess(form);
            }
            else if (onFileDownload != null){
                removeDownload(downloadManager, downloadId);
                onFileDownload.onFail(form);
            }
        }
    }

    protected void pollDownloadManager() {
        while (downloading) {
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadId);
            Cursor cursor = downloadManager.query(q);
            if(cursor.moveToFirst()) {
                final int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                final double mBytesDownloaded = ((double) bytesDownloaded) / 1000000.0;
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_PENDING && status != DownloadManager.STATUS_RUNNING) {
                    downloading = false;
                } else {
                    // throttle the thread
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {//means download was removed
                downloading = false;
            }
        }
    }

    public static void clearOngoingDownloads(Context context) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if(ongoingDownloads != null) {
            for(Long curId : ongoingDownloads) {
                removeDownload(downloadManager, curId);
            }
        }
    }

    private static void removeDownload(DownloadManager downloadManager, long downloadId) {
        if(ongoingDownloads != null) {
            ongoingDownloads.remove(downloadId);
        }
        downloadManager.remove(downloadId);
    }

    public boolean cancel() {
        if(downloadId != -1) {
            removeDownload(downloadManager, downloadId);
            return true;
        }

        return false;
    }

    public interface OnFileDownload {
        void onFail(Form form);
        void onSuccess(Form form);
    }
}
