package org.redcross.openmapkit.mspray;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.OSMMapBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by coder on 8/6/15.
 */
public class TargetAreasXmlDownloader extends AsyncTask<String, Integer, String> {
    String filename;

    private ProgressDialog progressDialog;
    private Context context;
    private String[] targetAreas;

    public TargetAreasXmlDownloader(Context context, String[] targetAreas) {
        this.context = context;
        this.targetAreas = targetAreas;
    }

    @Override
    protected void onPreExecute() {
        setupProgressDialog();
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        for (int i = 0; i < sUrl.length; i++) {
            try {
                // Full filepath for selected OSM file
                filename = ExternalStorage.getOSMDir() + targetAreas[i];
                URL url = new URL(sUrl[i]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(filename);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected void setupProgressDialog() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Downloading OSM Data");
        progressDialog.setMessage("Starting download...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }
}
