package org.redcross.openmapkit.odkcollect;

import android.os.AsyncTask;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.redcross.openmapkit.OpenMapKit;
import org.redcross.openmapkit.Settings;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class FormListDownloader extends AsyncTask<Void, Void, Void> {
    private static final String FORM_LIST_URI = "/forms.json";
    private static final int DEFAULT_TIMEOUT = 40000;
    private String server;
    private String username;
    private String password;
    private OnFormListGottenListener onFormListGottenListener;

    public FormListDownloader(OnFormListGottenListener onFormListGottenListener) {
        server = Settings.singleton().getOSMFromODKServer();
        username = Settings.singleton().getOSMFromODKUsername();
        password = Settings.singleton().getOSMFromODKPassword();

        this.onFormListGottenListener = onFormListGottenListener;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void execute() {

    }

    @Override
    protected Void doInBackground(Void... voids) {String urlString = server + FORM_LIST_URI;
        final StringRequest request = new StringRequest(Request.Method.GET, urlString, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    ArrayList<Form> forms = new ArrayList<>();
                    for(int i = 0; i < array.length(); i++) {
                        JSONObject curForm = array.getJSONObject(i);
                        if(curForm.getBoolean("instances_with_osm")) {
                            forms.add(new Form(curForm.getString("title"), curForm.getInt("formid")));
                        }
                    }

                    if(onFormListGottenListener != null) onFormListGottenListener.onData(forms);
                } catch (JSONException e) {
                    if(onFormListGottenListener != null) onFormListGottenListener.onError(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(onFormListGottenListener != null) onFormListGottenListener.onError(error);
            }
        });

        try {
            Map<String, String> headers = request.getHeaders();
            String authValue = username+":"+password;
            headers.put("Authorization", new String(Base64.encode(authValue.getBytes(), Base64.URL_SAFE)));
        } catch (AuthFailureError e) {
            if(onFormListGottenListener != null) onFormListGottenListener.onError(e);
        }
        request.setRetryPolicy(new DefaultRetryPolicy(DEFAULT_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        try {
            OpenMapKit.VOLLEY_REQUEST_QUEUE.add(request);
        } catch (OutOfMemoryError e) {
            if(onFormListGottenListener != null) onFormListGottenListener.onError(new Exception(e.getMessage()));
        }

        return null;
    }

    public interface OnFormListGottenListener {
        void onData(ArrayList<Form> formsList);
        void onError(Exception e);
    }
}
