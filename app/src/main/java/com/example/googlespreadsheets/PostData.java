package com.example.googlespreadsheets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class PostData extends AppCompatActivity {

    EditText NAME, AGE, GENDER;
    Button Submit, chooseFile;
    com.google.api.services.drive.Drive mService;
    String id, email;
    private static final int PICKFILE_RESULT_CODE = 1;
    TextView path;
    private Activity context;
    ProgressDialog dialog;

    public GoogleAccountCredential credential;
    public static final int REQUEST_AUTHORIZATION = 3;
    public static final int REQUEST_ACCOUNT_PICKER = 4;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE_METADATA_READONLY};
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_data);

        NAME = findViewById(R.id.name);
        AGE = findViewById(R.id.age);
        GENDER = findViewById(R.id.gender);
        Submit = findViewById(R.id.submit);
        path = findViewById(R.id.filePath);
        chooseFile = findViewById(R.id.chooseFile);
        dialog = new ProgressDialog(PostData.this);

        id= getIntent().getStringExtra("ID");
        email= getIntent().getStringExtra("Email");

        mService = (com.google.api.services.drive.Drive) getIntent().getSerializableExtra("Service");

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, email));

        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Spreadsheets")
                .build();

        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(path.getText().toString() != null) {

                    new SendRequest().execute();

                }else {

                    Toast.makeText(getApplicationContext(), "Attach a file", Toast.LENGTH_LONG).show();

                }

            }
        });

        chooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FilePicker();

            }
        });

    }

    public class SendRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){

            dialog.setMessage("Inserting your data");
            dialog.show();

        }

        protected String doInBackground(String... arg0) {

            try{

                File fileMetadata = new File();
                fileMetadata.setName("Android Insertion.jpg");
                java.io.File filePath = new java.io.File(path.getText().toString());
                FileContent mediaContent = new FileContent("image/jpeg", filePath);
                File file = mService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                Log.e("File ID: " , file.getId());


                URL url = new URL("https://script.google.com/macros/s/AKfycbytV68BWeNEiPoCguN1ZXfNIOB7h5gpkmdI2QoWPqqI_MHltBMbAeBsPtc03HYJG9gn/exec");
                // https://script.google.com/macros/s/AKfycbyuAu6jWNYMiWt9X5yp63-hypxQPlg5JS8NimN6GEGmdKZcIFh0/exec
                JSONObject postDataParams = new JSONObject();

                postDataParams.put("name",  NAME.getText().toString());
                postDataParams.put("age", AGE.getText().toString());
                postDataParams.put("gender", GENDER.getText().toString());
                postDataParams.put("drive", "https://drive.google.com/file/d/"+file.getId()+"/view?usp=sharing");
                postDataParams.put("id",id);

                Log.e("params",postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                }
                else {
                    return new String("false : "+responseCode);
                }
            }
            catch(Exception e){
                Log.e("Exception", e.getMessage());
                Toast.makeText(getApplicationContext(), "You haven't given appropriate permission", Toast.LENGTH_LONG).show();
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            Toast.makeText(getApplicationContext(), "Data has been inserted successfully", Toast.LENGTH_LONG).show();

        }
    }

    public void FilePicker(){

        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode==-1){

                    Uri fileUri = data.getData();
                    String wholeID = DocumentsContract.getDocumentId(fileUri);
                    String id = wholeID.split(":")[1];
                    String[] column = { MediaStore.Images.Media.DATA };
                    String sel = MediaStore.Images.Media._ID + "=?";
                    Cursor cursor = getContentResolver().
                            query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    column, sel, new String[]{ id }, null);
                    String filePath = "";
                    int columnIndex = cursor.getColumnIndex(column[0]);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(columnIndex);
                    }
                    cursor.close();
                    path.setText(filePath);

                }
                break;
        }

    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }


}