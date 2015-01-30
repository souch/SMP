package souch.smp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Settings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        // todo: dirty?
        final String strThreshold = getPreferenceScreen().getSharedPreferences().getString(PrefKeys.SHAKE_THRESHOLD, "30");
        EditTextPreference pref = (EditTextPreference) findPreference(PrefKeys.SHAKE_THRESHOLD);
        pref.setSummary(strThreshold);
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

        switch (key) {
            case PrefKeys.ENABLE_SHAKE:
                musicSrv.setEnableShake(sharedPreferences.getBoolean(PrefKeys.ENABLE_SHAKE, true));
                break;
            case PrefKeys.SHAKE_THRESHOLD:
                final String strThreshold = sharedPreferences.getString(PrefKeys.SHAKE_THRESHOLD, "30");
                musicSrv.setShakeThreshold(Float.valueOf(strThreshold) / 10.0f);

                EditTextPreference pref = (EditTextPreference) findPreference(key);
                pref.setSummary(strThreshold);
                this.onContentChanged();
                break;
        }
    }
}
