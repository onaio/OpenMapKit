package org.redcross.openmapkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import org.redcross.openmapkit.querying.OnQueryCompleteListener;
import org.redcross.openmapkit.querying.QueryHandler;
import org.redcross.openmapkit.querying.UsingOsmFileQueryHandler;

import java.util.ArrayList;

public class QueryActivity extends Activity {
    private ArrayList<QueryHandler> availableHandlers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_query);
        initQueryHandlers();
        query();
    }

    private void initQueryHandlers() {
        availableHandlers = new ArrayList<>();
        availableHandlers.add(new UsingOsmFileQueryHandler(this));
    }

    private void query() {
        QueryHandler selectedQueryHandler = null;
        for(QueryHandler curHandler : availableHandlers) {
            if(curHandler.canHandle(getIntent())) {
                selectedQueryHandler = curHandler;
                break;
            }
        }

        if(selectedQueryHandler != null) {
            selectedQueryHandler.handle(getIntent(), new OnQueryCompleteListener() {
                @Override
                public void onSuccess(Intent intent) {
                    returnResultOk(intent);
                }

                @Override
                public void onError(Exception e) {
                    returnCanceled(new Intent());
                }
            });
        } else {
            returnCanceled(new Intent());
        }
    }

    private void returnResultOk(Intent intent) {
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * This method returns RESULT_CANCELED back to whatever called this activity for results
     */
    private void returnCanceled(Intent intent) {
        setResult(RESULT_CANCELED, intent);
        finish();
    }

}
