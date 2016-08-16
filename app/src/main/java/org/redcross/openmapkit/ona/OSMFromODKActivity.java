package org.redcross.openmapkit.ona;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.redcross.openmapkit.R;

import java.util.ArrayList;
import java.util.HashMap;

public class OSMFromODKActivity extends AppCompatActivity implements FormOSMDownloader.OnFileDownload{
    private AppCompatDialog authDialog;
    private HashMap<Integer, Form> downloadingForms, successfulForms;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osm_from_odk);
        Settings.initialize();

        showAuthDialog();
    }

    private void showAuthDialog() {
        if(authDialog == null) {
            final ContextThemeWrapper themedContext;
            themedContext = new ContextThemeWrapper(this, R.style.CustomDialogStyle);
            authDialog = new AppCompatDialog(themedContext);
            authDialog.setContentView(R.layout.dialog_odk_auth);
            authDialog.setTitle(R.string.server_auth);

            Button cancelB = (Button) authDialog.findViewById(R.id.cancelB);
            cancelB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                    authDialog.dismiss();
                }
            });

            Button okB = (Button) authDialog.findViewById(R.id.okB);
            okB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                    EditText serverET = (EditText) authDialog.findViewById(R.id.serverET);
                    Settings.singleton().setOSMFromODKServer(serverET.getText().toString());
                    EditText usernameET = (EditText) authDialog.findViewById(R.id.usernameET);
                    Settings.singleton().setOSMFromODKUsername(usernameET.getText().toString());
                    EditText passwordET = (EditText) authDialog.findViewById(R.id.passwordET);
                    Settings.singleton().setOSMFromODKPassword(passwordET.getText().toString());

                    authDialog.dismiss();

                    startDownload();
                }
            });
        }

        EditText serverET = (EditText) authDialog.findViewById(R.id.serverET);
        serverET.setText(Settings.singleton().getOSMFromODKServer());
        EditText usernameET = (EditText) authDialog.findViewById(R.id.usernameET);
        usernameET.setText(Settings.singleton().getOSMFromODKUsername());
        EditText passwordET = (EditText) authDialog.findViewById(R.id.passwordET);
        passwordET.setText(Settings.singleton().getOSMFromODKPassword());

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(serverET.getWindowToken(), 0);

        authDialog.show();
    }

    private void startDownload() {
        ArrayList<Form> forms = Settings.singleton().getOSMFromODKForms();
        downloadingForms = new HashMap<>();
        successfulForms = new HashMap<>();
        for(Form curForm : forms) {
            downloadingForms.put(curForm.getId(), curForm);
            new FormOSMDownloader(getApplicationContext(), curForm, this).execute();
        }
    }

    @Override
    public void onFail(final Form form) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("An OSM file failed to download. Do you want to try download it again?");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                downloadingForms.put(form.getId(), form);
                new FormOSMDownloader(OSMFromODKActivity.this.getApplicationContext(), form, OSMFromODKActivity.this).execute();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                downloadingForms.remove(form.getId());
            }
        });
        builder.show();
    }

    @Override
    public void onSuccess(Form form) {
        downloadingForms.remove(form.getId());
        successfulForms.put(form.getId(), form);
    }
}
