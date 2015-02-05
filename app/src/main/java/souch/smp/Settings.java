package souch.smp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class Settings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;

    // todo: improve preference default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        CheckBoxPreference prefEnableShake = (CheckBoxPreference) findPreference(PrefKeys.ENABLE_SHAKE.name());
        EditTextPreference prefShakeThreshold = (EditTextPreference) findPreference(PrefKeys.SHAKE_THRESHOLD.name());
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            prefShakeThreshold.setSummary(sp.getString(PrefKeys.SHAKE_THRESHOLD.name(),
                    getString(R.string.settings_default_shake_threshold)));
        }
        else {
            prefEnableShake.setEnabled(false);
            prefShakeThreshold.setEnabled(false);
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.settings_no_accelerometer),
                    Toast.LENGTH_LONG).show();
        }
        this.onContentChanged();
    }

    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Settings", "onServiceConnected");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Settings", "onServiceDisconnected");
            serviceBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        unbindService(musicConnection);
        serviceBound = false;
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!serviceBound)
            return;
        Log.d("MusicService", "onSharedPreferenceChanged: " + key);

        if(key.equals(PrefKeys.ENABLE_SHAKE.name())) {
            musicSrv.setEnableShake(sharedPreferences.getBoolean(PrefKeys.ENABLE_SHAKE.name(), false));
        }
        else if(key.equals(PrefKeys.SHAKE_THRESHOLD.name())) {
                final String strThreshold = sharedPreferences.getString(PrefKeys.SHAKE_THRESHOLD.name(), getString(R.string.settings_default_shake_threshold));
                musicSrv.setShakeThreshold(Float.valueOf(strThreshold) / 10.0f);

                EditTextPreference pref = (EditTextPreference) findPreference(key);
                pref.setSummary(strThreshold);
                this.onContentChanged();
        }
    }
}
