package org.redcross.openmapkit.ona;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;
import org.redcross.openmapkit.ExternalStorage;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class FormOSMDownloader extends AsyncTask<Void, Void, Void> {
    private static final String FORM_LIST_URI = "/osm/";
    private static final int DEFAULT_TIMEOUT = 40000;
    private final Form form;
    private String server;
    private String username;
    private String password;
    private JSONObject query;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private int status = -1;
    private boolean downloading = false;
    private final OnFileDownload onFileDownload;

    public FormOSMDownloader(Context context, Form form, OnFileDownload onFileDownload) {
        this.form = form;
        this.onFileDownload = onFileDownload;
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
        String url = server + FORM_LIST_URI + String.valueOf(form.getId()) + ".osm";
        if(query != null) {
            url = url + "?query="+query.toString();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String credentials = username + ":" + password;
        request.addRequestHeader("Authorization", "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.URL_SAFE));
        request.setDestinationInExternalPublicDir(ExternalStorage.getOSMDirRelativeToExternalDir(), form.getId()+".osm");

        if(form.getName() == null || form.getName().trim().length() == 0) {
            request.setTitle(String.valueOf(form.getId()));
        } else {
            request.setTitle(form.getName());
        }

        downloadId = downloadManager.enqueue(request);
        pollDownloadManager();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        downloading = false;

        if(status == DownloadManager.STATUS_SUCCESSFUL
                && onFileDownload != null) onFileDownload.onSuccess(form);
        else if(onFileDownload != null) onFileDownload.onFail(form);
    }

    protected void pollDownloadManager() {
        while (downloading) {
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadId);
            Cursor cursor = downloadManager.query(q);
            cursor.moveToFirst();
            final int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            final double mBytesDownloaded = ((double)bytesDownloaded) / 1000000.0;
            status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status != DownloadManager.STATUS_PENDING && status != DownloadManager.STATUS_RUNNING) {
                downloading = false;
            }

            // throttle the thread
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean cancel() {
        if(downloadId != -1) {
            downloadManager.remove(downloadId);
            return true;
        }

        return false;
    }

    public interface OnFileDownload {
        void onFail(Form form);
        void onSuccess(Form form);
    }
}
