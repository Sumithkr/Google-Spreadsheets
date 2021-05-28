package com.example.googlespreadsheets;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.gson.JsonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class PostData extends AppCompatActivity {

    EditText NAME, AGE, GENDER;
    Button Submit, chooseFile;
    com.google.api.services.drive.Drive mService;
    public Sheets mService_Sheets;
    String id, email;
    private static final int PICKFILE_RESULT_CODE = 1;
    TextView path;
    private Activity context;
    List<Object> values;
    String[] values_String = new String[1000];
    ProgressDialog dialog;
    List<EditText> allEds = new ArrayList<EditText>();

    public GoogleAccountCredential credential;
    public static final int REQUEST_AUTHORIZATION = 3;
    public static final int REQUEST_ACCOUNT_PICKER = 4;
    public PostData activity;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE_METADATA_READONLY, SheetsScopes.SPREADSHEETS_READONLY, SheetsScopes.DRIVE, SheetsScopes.SPREADSHEETS};
    //private static final String[] SCOPES_Sheets = {};
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    //HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
    final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
    JsonFactory Fetchjson = JacksonFactory.getDefaultInstance();

    private static final String APPLICATION_NAME = "Google Spreadsheet";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    //private static final List<String> SCOPES_Sheets = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

    // This variable is storing the path to your credentials.json file

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */

    public PostData() throws GeneralSecurityException, IOException {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_data);

        /*NAME = findViewById(R.id.name);
        AGE = findViewById(R.id.age);
        GENDER = findViewById(R.id.gender);*/
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
                .setSelectedAccount(new Account(email, "com.example.googlespreadsheets"));

        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        try {
            mService_Sheets = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.activity = activity;
        new FetchColumns().execute();

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


    public class FetchColumns extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {

            dialog.setMessage("Fetching Data");
            dialog.show();

        }

        @Override
        protected String doInBackground(String... strings) {

            String range = "A:Z";
            ValueRange response = null;
            try {

                Sheets.Spreadsheets.Values.Get request =
                        mService_Sheets.spreadsheets().values().get(id, range);
                response = request.execute();


            } catch (UserRecoverableAuthIOException e) {
                e.printStackTrace();
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                //Toast.makeText(getApplicationContext(), "Can't Fetch columns of your sheet", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

            values = response.getValues().get(0);
            DynamicallyGenerateEditext();
            return null;

        }

        @Override
        protected void onPostExecute(String result) {

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

        }
    }

    public void DynamicallyGenerateEditext(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    LinearLayout parentLinear = (LinearLayout) findViewById(R.id.parentLinear);
                    LinearLayout l = new LinearLayout(PostData.this);
                    l.setOrientation(LinearLayout.VERTICAL);

                    for (int j = 0; j < values.size(); j++) {
                        EditText et = new EditText(PostData.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(800, 140);
                        lp.setMargins(10, 10, 10, 10);
                        et.setId(j);
                        allEds.add(et);
                        et.setBackgroundResource(R.drawable.editext_back);
                        et.setPadding(20, 0, 0, 0);
                        et.setHint(String.valueOf(values.get(j)));
                        et.setTextColor(getResources().getColor(R.color.white));
                        l.addView(et, lp);
                    }
                    parentLinear.addView(l);

                }catch(Exception e){

                    Log.e("Sheet Mismatch", e.getMessage());
                    Toast.makeText(PostData.this, "Sheet Format Mismatch", Toast.LENGTH_LONG).show();

                }

            }
        });

    }

    public class SendRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){

            dialog.setMessage("Inserting your data");
            dialog.show();

            for(int j=0; j< values.size(); j++)

                values_String[j] = allEds.get(j).getText().toString();


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


                URL url = new URL("https://script.google.com/macros/s/AKfycbwb_97Hh9zrf5rFeQgh8EuPUf6HCnZvMcx_jArqb6R73JEl_oecmKty9jN5cfqIyGO4Mg/exec");
                // https://script.google.com/macros/s/AKfycbyuAu6jWNYMiWt9X5yp63-hypxQPlg5JS8NimN6GEGmdKZcIFh0/exec
                JSONObject postDataParams = new JSONObject();
                JSONArray values_JSON = new JSONArray();

                for(int j=0; j<values.size(); j++)

                    values_JSON.put(values_String[j]);

                postDataParams.put("number", values.size());
                postDataParams.put("id",id);
                postDataParams.put("value", values_JSON);
                postDataParams.put("drive", "https://drive.google.com/file/d/"+file.getId()+"/view?usp=sharing");

                /*for(int k=0; k < values.size(); k++){x`

                    postDataParams.put(String.valueOf(k), values_String[k]);

                }*/

                /*postDataParams.put("name",  NAME.getText().toString());
                postDataParams.put("age", AGE.getText().toString());
                postDataParams.put("gender", GENDER.getText().toString());
                postDataParams.put("drive", "https://drive.google.com/file/d/"+file.getId()+"/view?usp=sharing");
                postDataParams.put("id",id);*/

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
            catch(UserRecoverableAuthIOException e){
                e.printStackTrace();
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return new String("Exception: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
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