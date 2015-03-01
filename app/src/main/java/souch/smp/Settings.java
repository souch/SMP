/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class Settings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener
{
    private Parameters params;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;

    // todo: improve preference default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Settings", "onCreate");
        super.onCreate(savedInstanceState);
        params = new ParametersImpl(this);
        // fixme: everything should be put in onResume?
        addPreferencesFromResource(R.xml.preferences);
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        String thresholdKeys = PrefKeys.SHAKE_THRESHOLD.name();
        EditTextPreference prefShakeThreshold = (EditTextPreference) findPreference(thresholdKeys);
        CheckBoxPreference prefEnableShake = (CheckBoxPreference) findPreference(PrefKeys.ENABLE_SHAKE.name());
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            prefShakeThreshold.setSummary(String.valueOf(params.getShakeThreshold()));
            prefEnableShake.setChecked(params.getEnableShake());
        }
        else {
            prefShakeThreshold.setEnabled(false);
            prefEnableShake.setEnabled(false);
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.settings_no_accelerometer),
                    Toast.LENGTH_LONG).show();
        }

        findPreference(PrefKeys.TEXT_SIZE_NORMAL.name()).setSummary(String.valueOf(params.getNormalTextSize()));
        findPreference(PrefKeys.TEXT_SIZE_BIG.name()).setSummary(String.valueOf(params.getBigTextSize()));
        findPreference(PrefKeys.TEXT_SIZE_RATIO.name()).setSummary(String.valueOf(params.getTextSizeRatio()));
        setChoosedTextSizeSummary();

        Preference rescan = findPreference(getResources().getString(R.string.settings_rescan_key));
        rescan.setOnPreferenceClickListener(this);

        Preference donate = findPreference("donate");
        donate.setOnPreferenceClickListener(this);

        String rootFolderKey = PrefKeys.ROOT_FOLDER.name();
        EditTextPreference prefRootFolder = (EditTextPreference) findPreference(rootFolderKey);
        prefRootFolder.setSummary(params.getRootFolder());
        if(!sharedPreferences.contains(rootFolderKey))
            prefRootFolder.setText(ParametersImpl.getDefaultMusicDir());

        setFoldSummary();

        this.onContentChanged();
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!serviceBound)
            return;
        Log.d("MusicService", "onSharedPreferenceChanged: " + key);

        if(key.equals(PrefKeys.DEFAULT_FOLD.name())) {
            setFoldSummary();
        }
        else if(key.equals(PrefKeys.TEX_SIZE_CHOOSED.name())) {
            setChoosedTextSizeSummary();
            Main.applyTextSize(params);
            // todo a bit dirty? we actually just need to call Adapter.notifyDataSetChanged
            musicSrv.setChanged();
        }
        else if(key.equals(PrefKeys.TEXT_SIZE_NORMAL.name())) {
            findPreference(key).setSummary(String.valueOf(params.getNormalTextSize()));
            Main.applyTextSize(params);
            musicSrv.setChanged();
        }
        else if(key.equals(PrefKeys.TEXT_SIZE_BIG.name())) {
            findPreference(key).setSummary(String.valueOf(params.getBigTextSize()));
            Main.applyTextSize(params);
            musicSrv.setChanged();
        }
        else if(key.equals(PrefKeys.TEXT_SIZE_RATIO.name())) {
            findPreference(key).setSummary(String.valueOf(String.valueOf(params.getTextSizeRatio())));
            Main.applyTextSize(params);
            musicSrv.setChanged();
        }
        else if(key.equals(PrefKeys.ENABLE_SHAKE.name())) {
            musicSrv.setEnableShake(params.getEnableShake());
        }
        else if(key.equals(PrefKeys.SHAKE_THRESHOLD.name())) {
            final float threshold = params.getShakeThreshold();
            musicSrv.setShakeThreshold(threshold);
            findPreference(key).setSummary(String.valueOf(threshold));
            //this.onContentChanged(); // useful?
        }
        else if(key.equals(PrefKeys.ROOT_FOLDER.name())) {
            final String rootFolder = params.getRootFolder();
            findPreference(key).setSummary(rootFolder);
            if(!(new File(rootFolder)).exists())
                Toast.makeText(getApplicationContext(),
                        "! The path '" + rootFolder + "' does not exists on the phone !",
                        Toast.LENGTH_LONG).show();
            boolean reinited = musicSrv.getRows().setRootFolder(rootFolder);
            if(reinited)
                musicSrv.setChanged();
        }
    }


    private void setFoldSummary() {
        int idx = params.getDefaultFold();
        ListPreference prefFold = (ListPreference) findPreference(PrefKeys.DEFAULT_FOLD.name());
        prefFold.setSummary((getResources().getStringArray(R.array.settings_fold_entries))[idx]);
    }

    private void showDonateWebsite() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(getString(R.string.settings_donate_www)));
        this.startActivity(webIntent);
    }

    private void setChoosedTextSizeSummary() {
        int r;
        if (!params.getChoosedTextSize())
            r = R.string.settings_text_size_normal;
        else
            r = R.string.settings_text_size_big;
        findPreference(PrefKeys.TEX_SIZE_CHOOSED.name()).setSummary(getResources().getString(r));
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
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals(getResources().getString(R.string.settings_rescan_key))) {
            rescan();
        } else if (preference.getKey().equals("donate")) {
            showDonateWebsite();
        }
        return false;
    }


    public void rescan() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Toast.makeText(getApplicationContext(),
                "Rescan broadcast disabled from Android KitKat.", Toast.LENGTH_LONG).show();
            return;
        }

        // Broadcast the Media Scanner Intent to trigger it
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                .parse("file://" + Environment.getExternalStorageDirectory())));
        Toast toast = Toast.makeText(getApplicationContext(),
                "Media Scanner Triggered...", Toast.LENGTH_SHORT);
        toast.show();

        // todo: should be improved with this?
        // http://stackoverflow.com/questions/13270789/how-to-run-media-scanner-in-android
        // careful from hitkat 4.4+ :
        // http://stackoverflow.com/questions/24072489/java-lang-securityexception-permission-denial-not-allowed-to-send-broadcast-an
    }
}
