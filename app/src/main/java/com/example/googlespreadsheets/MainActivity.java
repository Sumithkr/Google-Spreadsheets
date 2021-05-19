package com.example.googlespreadsheets;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.SignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.signin.SignInOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    public GoogleSignInOptions signInOptions;
    public GoogleSignInClient client;
    public GoogleSignInAccount account;
    public com.google.api.services.drive.Drive mService;
    public GoogleAccountCredential credential;
    public static final int REQUEST_AUTHORIZATION = 3, RC_SIGN_IN = 1;
    public static final int REQUEST_ACCOUNT_PICKER = 4, PERMISSIONS_REQUEST_GET_ACCOUNTS = 1;
    ProgressDialog dialog;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE_METADATA_READONLY};
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    String pageToken = null;
    ListView SPREADSHEETS_NAMES;
    TextView SPREADSHEET_TEXTVIEW;
    List<String> fileInfo = new ArrayList<String>();
    List<String> fileID = new ArrayList<String>();

    @Override
    protected void onResume() {
        super.onResume();
        isWriteStoragePermissionGranted();
        getAccountsPermission();
        refreshResults();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SPREADSHEETS_NAMES = findViewById(R.id.listview);
        SPREADSHEET_TEXTVIEW= findViewById(R.id.textView);
        dialog = new ProgressDialog(MainActivity.this);

        SPREADSHEETS_NAMES.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Toast.makeText(getApplicationContext(), fileInfo.get(position), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getBaseContext(), PostData.class);
                intent.putExtra("ID", fileID.get(position));
                intent.putExtra("Email", account.getEmail());
                startActivity(intent);

            }
        });

    }

    public void getAccountsPermission(){

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.GET_ACCOUNTS)) {

                Log.e("Accounts", "Permission Granted");

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, PERMISSIONS_REQUEST_GET_ACCOUNTS);
            }
        }

    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("TAG","Permission is granted2");
                return true;
            } else {

                Log.e("TAG","Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.e("TAG","Permission is granted2");
            return true;
        }
    }

    public void requestSignIn(){

        try{

            signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope(DriveScopes.DRIVE_FILE)).build();

            client = GoogleSignIn.getClient(this, signInOptions);

            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

            if(account != null) {

                printBasic();

                SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                credential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                        .setBackOff(new ExponentialBackOff())
                        .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, account.getEmail()));


                mService = new com.google.api.services.drive.Drive.Builder(
                        transport, jsonFactory, credential)
                        .setApplicationName("Google Spreadsheets")
                        .build();

            }else {

                Intent signInIntent = client.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);

            }

            //startActivityForResult(client.getSignInIntent(), 400);

        }catch(Exception ex){
            Log.e("Signing In", ex.getMessage());
        }

    }

    private void printBasic() {
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.e("TAG", "latest sign in: "
                    + "\n\tPhoto url:" + account.getPhotoUrl()
                    + "\n\tEmail:" + account.getEmail()
                    + "\n\tDisplay name:" + account.getDisplayName()
                    + "\n\tFamily(last) name:" + account.getFamilyName()
                    + "\n\tGiven(first) name:" + account.getGivenName()
                    + "\n\tId:" + account.getId()
                    + "\n\tIdToken:" + account.getIdToken()
            );
        } else {
            Log.e("TAG", "basic info is null");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Log.e("TAG", "Successful");


        } else {

            Log.e("TAG", "failed, user denied OR no network OR jks SHA1 not configure yet at play console android project");

        }

        switch (requestCode) {

            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                Log.w("gd", "in account picker");
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.e("gd", "in cancelled");
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }


    }


    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void refreshResults() {
        new GoogleDriveAsync(MainActivity.this).execute();
    }

    public class GoogleDriveAsync extends AsyncTask<Void, Void, Void> {

        private final MainActivity activity;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            requestSignIn();
            dialog.setMessage("Getting your sheets");
            dialog.show();

        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                getDataFromApi();

            } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                Log.e("Play Services", "GPS unavailable");

            } catch (UserRecoverableAuthIOException userRecoverableException) {
                Log.e("Recoverable Auth", "user recoverable");
                activity.startActivityForResult(
                        userRecoverableException.getIntent(),
                        MainActivity.REQUEST_AUTHORIZATION);

            } catch (Exception e) {
                Log.e("gd", e.getMessage()+"----");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            try {
                ListviewAdapter adapter = new ListviewAdapter(MainActivity.this, fileInfo);
                SPREADSHEETS_NAMES = findViewById(R.id.listview);
                SPREADSHEETS_NAMES.setAdapter(adapter);
            } catch (Exception exception) {
                Log.e("Error", exception.toString());
            }

        }

        GoogleDriveAsync(MainActivity activity) {
            this.activity = activity;
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.

            FileList result = activity.mService.files().list()
                    .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
                    .execute();

            List<File> files = result.getFiles();

            if (files != null) {
                for (File file : files) {
                    fileID.add(file.getId());
                    fileInfo.add(file.getName());
                    Log.e("Here is your file",  fileInfo.toString());
                }
            }

            return fileInfo;

        }
    }

    public void handleSignInIntent(Intent data){

        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {

                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(MainActivity.this,
                                Collections.singleton(DriveScopes.DRIVE_FILE));

                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        Drive googleDriveService = new Drive.Builder(

                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential).setApplicationName("Google Spreadsheets").build();

                        Thread GettingGoogleSheets= new Thread(){

                            public void run(){

                                do {

                                    try {

                                        FileList result = googleDriveService.files().list()
                                                .setQ("mimeType='image/jpeg'")
                                                .setSpaces("drive")
                                                .setFields("nextPageToken, files(id, name)")
                                                .setFields(pageToken)
                                                .execute();

                                        for (File file : result.getFiles()) {

                                            Log.e("Found File:", file.getName());

                                        }
                                        pageToken = result.getNextPageToken();


                                    }catch(Exception ex){

                                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();

                                    }

                                }while (pageToken != null);

                            }

                        };

                        GettingGoogleSheets.start();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(getApplicationContext(), "Don't make it a habbit", Toast.LENGTH_LONG).show();

                    }
                });

    }
}