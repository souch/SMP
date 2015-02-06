package souch.smp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class Settings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener
{
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

        EditTextPreference prefShakeThreshold = (EditTextPreference) findPreference(PrefKeys.SHAKE_THRESHOLD.name());
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            prefShakeThreshold.setSummary(sp.getString(PrefKeys.SHAKE_THRESHOLD.name(),
                    getString(R.string.settings_default_shake_threshold)));
        }
        else {
            prefShakeThreshold.setEnabled(false);
        }

        // todo: root_folder ..
        /*Environment.getExternalStorageDirectory().getPath()
        <string name="settings_root_folder">Root folder of folder filter</string>
        <string name="settings_default_root_folder">/mnt/sdcard/music</string>
        */

        Preference rescan = findPreference(getResources().getString(R.string.settings_rescan_key));
        rescan.setOnPreferenceClickListener(this);

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

        if(key.equals(PrefKeys.SHAKE_THRESHOLD.name())) {
                final String strThreshold = sharedPreferences.getString(PrefKeys.SHAKE_THRESHOLD.name(), getString(R.string.settings_default_shake_threshold));
                float threshold = Float.valueOf(strThreshold) / 10.0f;
                musicSrv.setShakeThreshold(threshold);
                Log.d("MusicService", "Set shake threshold to: " + threshold);

                EditTextPreference pref = (EditTextPreference) findPreference(key);
                pref.setSummary(strThreshold);
                this.onContentChanged();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals(getResources().getString(R.string.settings_rescan_key))) {
            rescan();
        }
        return false;
    }

    public void rescan() {
        // Broadcast the Media Scanner Intent to trigger it
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                .parse("file://" + Environment.getExternalStorageDirectory())));
        Toast toast = Toast.makeText(getApplicationContext(),
                "Media Scanner Triggered...", Toast.LENGTH_SHORT);
        toast.show();

        // todo: should be improved with this?
        // http://stackoverflow.com/questions/13270789/how-to-run-media-scanner-in-android
    }
}
