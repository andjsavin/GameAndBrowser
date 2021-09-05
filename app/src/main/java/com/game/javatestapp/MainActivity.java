package com.game.javatestapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import com.facebook.FacebookSdk;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onesignal.OneSignal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    /*-- CUSTOMIZE --*/
    /*-- you can customize these options for your convenience --*/
    private static String file_type     = "*/*";    // file types to be allowed for upload
    private boolean multiple_files      = true;         // allowing multiple file upload

    private boolean checkInternet;
    private String checkLocale;
    /*-- MAIN VARIABLES --*/
    WebView webView;

    private static final String TAG = MainActivity.class.getSimpleName();

    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri> file_data;       // data/header received after file selection
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location

    private final static int file_req_code = 1;

    //httpclient requester
    public class GetExample {
        final OkHttpClient client = new OkHttpClient();

        String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            }
        }
    }
    //get IP from ipify.org and location from ipstack.com
    public String getIP() throws IOException {
        String apiKey = "80e72e43af3b4d45eea34946c15d29ab";
        GetExample example = new GetExample();
        String response = example.run("http://api.ipstack.com/" + example.run("https://api.ipify.org") + "?access_key=80e72e43af3b4d45eea34946c15d29ab");
        return response;
    }

    class AsyncRequest extends AsyncTask<String, String, String>{
        private Exception exception;
        @Override
        protected String doInBackground(String ...arg) {
            try {
                return getIP();
            } catch (Exception e) {
                this.exception = e;
                return "";
            }
        }

        @Override
        protected void onPostExecute(String v) {
        }
    }
    private boolean checkFirstRun() {
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();

        final String PREFS_NAME = "MyPrefsFile";
        final String PREF_VERSION_CODE_KEY = "version_code";
        final String PREF_LOCALE = "Locale";
        final String PREF_INTERNET = "Connected";
        final int DOESNT_EXIST = -1;
        // Get current version code
        int currentVersionCode = BuildConfig.VERSION_CODE;
        // Get saved version code
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);
        // Check for first run or upgrade
        if (currentVersionCode == savedVersionCode) {
            checkInternet = prefs.getBoolean(PREF_INTERNET, checkInternet);
            return false;

        }
        checkInternet = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).apply();
        prefs.edit().putBoolean(PREF_INTERNET, checkInternet).apply();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;

            /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/
            if (resultCode == Activity.RESULT_CANCELED) {
                file_path.onReceiveValue(null);
                return;
            }

            /*-- continue if response is positive --*/
            if(resultCode== Activity.RESULT_OK){
                if(null == file_path){
                    return;
                }
                ClipData clipData;
                String stringData;

                try {
                    clipData = intent.getClipData();
                    stringData = intent.getDataString();
                }catch (Exception e){
                    clipData = null;
                    stringData = null;
                }
                if (clipData == null && stringData == null && cam_file_data != null) {
                    results = new Uri[]{Uri.parse(cam_file_data)};
                }else{
                    if (clipData != null) { // checking if multiple files selected or not
                        final int numSelectedFiles = clipData.getItemCount();
                        results = new Uri[numSelectedFiles];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else {
                        try {
                            Bitmap cam_photo = (Bitmap) intent.getExtras().get("data");
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            cam_photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            stringData = MediaStore.Images.Media.insertImage(this.getContentResolver(), cam_photo, null, null);
                        }catch (Exception ignored){}
                        results = new Uri[]{Uri.parse(stringData)};
                    }
                }
            }

            file_path.onReceiveValue(results);
            file_path = null;
        }else{
            if(requestCode == file_req_code){
                if(null == file_data) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                file_data.onReceiveValue(result);
                file_data = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //check if it's first time
        boolean first = checkFirstRun();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkInternet && first) {
            AsyncRequest req = new AsyncRequest();
            req.execute();
            try {
                checkLocale = req.get();
            } catch (ExecutionException e) {
                checkLocale = "";
            } catch (InterruptedException e) {
                checkLocale = "";
            }
            //parse JSON string to json
            JsonObject geo = new JsonParser().parse(checkLocale).getAsJsonObject();
            //get coutnry_code from geo if internet is up
            checkLocale = geo.get("country_code").toString().replace("\"", "");
            //else get from device locale
        } else {
            checkLocale = this.getResources().getConfiguration().locale.getCountry();
        }
        //if connection is up and country is UA or PL
        if ((checkInternet) && (checkLocale.equals("UA") || checkLocale.equals("PL"))) {
            webView = findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(true);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            } else {
                CookieManager.getInstance().setAcceptCookie(true);
            }
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.getSettings().setAllowFileAccess(true);
            webView.setWebViewClient(new Callback());
            webView.loadUrl("html5test.com");
            //file uploader
            webView.setWebChromeClient(new WebChromeClient() {
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                    if(file_permission() && Build.VERSION.SDK_INT >= 21) {
                        file_path = filePathCallback;
                        Intent takePictureIntent = null;
                        Intent takeVideoIntent = null;

                        boolean includeVideo = false;
                        boolean includePhoto = false;

                        /*-- checking the accept parameter to determine which intent(s) to include --*/
                        paramCheck:
                        for (String acceptTypes : fileChooserParams.getAcceptTypes()) {
                            String[] splitTypes = acceptTypes.split(", ?+"); // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                            for (String acceptType : splitTypes) {
                                switch (acceptType) {
                                    case "*/*":
                                        includePhoto = true;
                                        includeVideo = true;
                                        break paramCheck;
                                    case "image/*":
                                        includePhoto = true;
                                        break;
                                    case "video/*":
                                        includeVideo = true;
                                        break;
                                }
                            }
                        }

                        if (fileChooserParams.getAcceptTypes().length == 0) {   //no `accept` parameter was specified, allow both photo and video
                            includePhoto = true;
                            includeVideo = true;
                        }

                        if (includePhoto) {
                            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                                File photoFile = null;
                                try {
                                    photoFile = create_image();
                                    takePictureIntent.putExtra("PhotoPath", cam_file_data);
                                } catch (IOException ex) {
                                    Log.e(TAG, "Image file creation failed", ex);
                                }
                                if (photoFile != null) {
                                    cam_file_data = "file:" + photoFile.getAbsolutePath();
                                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                } else {
                                    cam_file_data = null;
                                    takePictureIntent = null;
                                }
                            }
                        }

                        if (includeVideo) {
                            takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            if (takeVideoIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                                File videoFile = null;
                                try {
                                    videoFile = create_video();
                                } catch (IOException ex) {
                                    Log.e(TAG, "Video file creation failed", ex);
                                }
                                if (videoFile != null) {
                                    cam_file_data = "file:" + videoFile.getAbsolutePath();
                                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                                } else {
                                    cam_file_data = null;
                                    takeVideoIntent = null;
                                }
                            }
                        }

                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType(file_type);
                        if (multiple_files) {
                            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }

                        Intent[] intentArray;
                        if (takePictureIntent != null && takeVideoIntent != null) {
                            intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
                        } else if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else if (takeVideoIntent != null) {
                            intentArray = new Intent[]{takeVideoIntent};
                        } else {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                        startActivityForResult(chooserIntent, file_req_code);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            //adress bar
            EditText editText = (EditText) findViewById(R.id.urlBar);
            //check when user presses enter
            editText.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        // Perform action on key press

                        webView.loadUrl(editText.getText().toString());
                        return true;
                    }
                    return false;
                }
            });
        }
        //start game if connection is off or country isn't UA or PL
        else {
            startActivity(new Intent(MainActivity.this, GameActivity.class));
        }
    }
    public class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), "Failed loading app!", Toast.LENGTH_SHORT).show();
        }
    }

    /*-- checking and asking for required file permissions --*/
    public boolean file_permission(){
        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        }else{
            return true;
        }
    }

    /*-- creating new image file here --*/
    private File create_image() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    /*-- creating new video file here --*/
    private File create_video() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".3gp", sd_directory);
    }

    /*-- back/down key handling --*/
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
}