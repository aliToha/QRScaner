package com.gagalcoding.qrscaner;

import android.Manifest;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler,
        CameraSelectorDialogFragment.CameraSelectorDialogListener,
        SharedPreferences.OnSharedPreferenceChangeListener{
    private ZXingScannerView mScannerView;
    private int mCameraId;
    private boolean mFlash;
    private boolean mAutoFocus = true;
    SharedPreferences sharedPreferences;
    String BASE_URL,question;
    Boolean SEND_MODE,FRONT_CAMERA;

    private static final String[] PERMISSION_CAMERA =
            new String[]{Manifest.permission.CAMERA};
    PermissionsChecker checker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checker = new PermissionsChecker(this);
        if(checker.lacksPermissions(PERMISSION_CAMERA)) {
            startPermissionActivity(PERMISSION_CAMERA);
        }
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setupSharedPreference(sharedPreferences);
    }

    private void setupSharedPreference(SharedPreferences sharedPreferences) {
        BASE_URL = sharedPreferences.getString(getString(R.string.pref_url_key),getString(R.string.pref_url_default));
        SEND_MODE = sharedPreferences.getBoolean(getString(R.string.pref_send_key),getResources().getBoolean(R.bool.pref_send_default));
        if (SEND_MODE.equals(true)){
            question = "SEND";
        }else{
            question = "OK";
        }
        mFlash = sharedPreferences.getBoolean(getString(R.string.pref_flash_key),getResources().getBoolean(R.bool.pref_flash_default));
        FRONT_CAMERA = sharedPreferences.getBoolean(getString(R.string.pref_frontCamera_key),getResources().getBoolean(R.bool.pref_frontCamera_default));
        if(FRONT_CAMERA.equals(true)){
            mCameraId = 1;

        }else{
            mCameraId = -1;
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera(mCameraId);
        mScannerView.setFlash(mFlash);
        mScannerView.setAutoFocus(mAutoFocus);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(final Result result) {
        Log.v("TAG", result.getText()); // Prints scan results
        Log.v("TAG", result.getBarcodeFormat().toString());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(question, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(SEND_MODE.equals(false)){
                    //do nothing
                    searchData(result.getText());
                }else{
                    sendData(result.getText());
                }
            }
        });
        //builder.setTitle("Scan Result");
        builder.setMessage(result.getText());
        AlertDialog alert1 = builder.create();
        alert1.show();

        mScannerView.resumeCameraPreview(this);
        onPause();
    }

    private void searchData(String text) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY,text);
        startActivity(intent);
    }

    private void sendData(String value) {
        ApiInterface apiInterface = ApiClient.getRetrofit(BASE_URL).create(ApiInterface.class);
        Call<Value> call = apiInterface.addBarcode(value);
        call.enqueue(new Callback<Value>() {
            @Override
            public void onResponse(Call<Value> call, Response<Value> response) {
                if(response.isSuccessful()) {
                    String value = response.body().getValue();
                    String message = response.body().getMessage();
                    if (value.equals("1")) {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        onResume();
                    } else {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        onResume();
                    }
                }else{
                    Log.e("error code",String.valueOf(response.code()));
                    Log.e("error body", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "periksa URL anda", Toast.LENGTH_SHORT).show();
                    onResume();
                }
            }

            @Override
            public void onFailure(Call<Value> call, Throwable t) {
                Log.e("error code",String.valueOf(t));
                Toast.makeText(MainActivity.this, "Cek your internet connection", Toast.LENGTH_SHORT).show();
                onResume();
            }
        });
    }

    private void startPermissionActivity(String[] permissionRead) {
        PermissionsActivity.startActivityForResult(this,0,permissionRead);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.setting :
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;
            case R.id.camera :
                CameraSelectorDialogFragment.newInstance(this,-1);
                Log.d("camera","cmera"+mCameraId);
               // sharedPreferences.edit().putBoolean(getString(R.string.pref_frontCamera_key),false).apply();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraSelected(int cameraId) {
        mCameraId = cameraId;
        mScannerView.startCamera(mCameraId);
        mScannerView.setFlash(mFlash);
        mScannerView.setAutoFocus(mAutoFocus);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_url_key))) {
            String base_url = sharedPreferences.getString(getString(R.string.pref_url_key), "https:/example.com");
            this.BASE_URL = base_url;
        }else if(key.equals(getString(R.string.pref_send_key))){
            Boolean send_mode = sharedPreferences.getBoolean(key,getResources().getBoolean(R.bool.pref_send_default));
            this.SEND_MODE = send_mode;
            if(SEND_MODE.equals(true)){
                this.question = "SEND";
            }else{
                this.question = "OK";
            }
        }else if(key.equals(getString(R.string.pref_flash_key))){
            Boolean flash = sharedPreferences.getBoolean(key,getResources().getBoolean(R.bool.pref_flash_default));
            this.mFlash = flash;
        }else if (key.equals(getString(R.string.pref_frontCamera_key))){
            Boolean front_camera = sharedPreferences.getBoolean(key,getResources().getBoolean(R.bool.pref_frontCamera_default));
            if(front_camera.equals(true)){
                this.mCameraId = 1;
            }else{
                this.mCameraId = -1;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
