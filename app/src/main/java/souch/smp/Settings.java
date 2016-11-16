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
import android.media.MediaScannerConnection;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;

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

        Preference donate = findPreference(getResources().getString(R.string.settings_donate_key));
        donate.setOnPreferenceClickListener(this);

        setUnfoldSubgroup();
        setUnfoldThresholdSummary();

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
        else if(key.equals(PrefKeys.TEXT_SIZE_CHOOSED.name())) {
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
        }
        else if(key.equals(PrefKeys.UNFOLD_SUBGROUP.name())) {
            setUnfoldSubgroup();
        }
        else if(key.equals(PrefKeys.UNFOLD_SUBGROUP_THRESHOLD.name())) {
            setUnfoldThresholdSummary();
        }
        else if(key.equals(PrefKeys.ROOT_FOLDER.name())) {
            final String rootFolder = params.getRootFolder();
            findPreference(key).setSummary(rootFolder);
            if(!(new File(rootFolder)).exists()) {
                Formatter formatter = new Formatter();
                formatter.format(getResources().getString(R.string.settings_root_folder_summary),
                        rootFolder);
                Toast.makeText(getApplicationContext(),
                                formatter.toString(),
                                Toast.LENGTH_LONG).show();
            }
            boolean reinited = musicSrv.getRows().setRootFolder(rootFolder);
            if(reinited)
                musicSrv.setChanged();
        }
    }

    private void setUnfoldSubgroup() {
        findPreference(PrefKeys.UNFOLD_SUBGROUP_THRESHOLD.name()).setEnabled(!params.getUnfoldSubGroup());
    }

    private void setUnfoldThresholdSummary() {
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.settings_unfold_subgroup_threshold_summary),
                params.getUnfoldSubGroupThreshold());
        findPreference(PrefKeys.UNFOLD_SUBGROUP_THRESHOLD.name()).setSummary(formatter.toString());
    }

    private void setFoldSummary() {
        int idx = params.getDefaultFold();
        ListPreference prefFold = (ListPreference) findPreference(PrefKeys.DEFAULT_FOLD.name());
        String[] foldEntries = getResources().getStringArray(R.array.settings_fold_entries);
        if (idx >= foldEntries.length)
            idx = foldEntries.length - 1;
        if (idx >= 0)
            prefFold.setSummary(foldEntries[idx]);
    }

    private void showDonateWebsite() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(getString(R.string.settings_donate_www)));
        this.startActivity(webIntent);
    }

    private void setChoosedTextSizeSummary() {
        int r;
        if (!params.getChoosedTextSize())
            r = R.string.settings_text_size_regular;
        else
            r = R.string.settings_text_size_big;
        findPreference(PrefKeys.TEXT_SIZE_CHOOSED.name()).setSummary(getResources().getString(r));
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
        } else if (preference.getKey().equals(getResources().getString(R.string.settings_donate_key))) {
            showDonateWebsite();
        }
        return false;
    }

    public void rescan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scanMediaFiles();
        }
        else {
            if (android.os.Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED))
                // Broadcast the Media Scanner Intent to trigger it
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://" + Environment.getExternalStorageDirectory())));

            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.settings_rescan_triggered), Toast.LENGTH_SHORT).show();
        }
    }

    private void scanMediaFiles() {
        // http://stackoverflow.com/questions/13270789/how-to-run-media-scanner-in-android
        Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.settings_rescan_triggered),
                Toast.LENGTH_LONG).show();

        Collection<File> dirsToScan = getStorages();

        for (File dir: dirsToScan) {
            Toast.makeText(getApplicationContext(),
                    (new Formatter()).format(getResources()
                            .getString(R.string.settings_rescan_storage), dir)
                            .toString(),
                    Toast.LENGTH_LONG).show();
        }

        ArrayList<File> filesToScan = new ArrayList<>();

        // add Music folder in first to speedup music folder discovery
        for (File dir: dirsToScan) {
            listFiles(new File(dir, "Music"), filesToScan);
            Log.d("Settings", "fileToScan: " + (new File(dir, "Music")).getAbsolutePath());
        }
        scanMediaFiles(filesToScan);

        // add whole storage at the end
        filesToScan.clear();
        for (File dir: dirsToScan) {
            listFiles(dir, filesToScan);
        }
        scanMediaFiles(filesToScan);

        Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.settings_rescan_finished),
                Toast.LENGTH_SHORT).show();
    }

    private void scanMediaFiles(Collection<File> filesToScan) {
        String[] filesToScanArray = new String[filesToScan.size()];
        int i = 0;
        for (File file : filesToScan) {
            filesToScanArray[i] = file.getAbsolutePath();
            //if (filesToScanArray[i].contains("emulated/0"))
            //    Log.d("Settings", "fileToScan: " + filesToScanArray[i]);
            i++;
        }

        if (filesToScanArray.length != 0) {
            MediaScannerConnection.scanFile(this, filesToScanArray, null, null);
        } else {
            Log.e("Settings", "Media scan requested when nothing to scan");
        }
    }


    public Collection<File> getStorages() {
        HashSet<File> dirsToScan = new HashSet<>();

        dirsToScan.add(Environment.getExternalStorageDirectory());

        // hack. Don't know if it work well on other devices!
        String userPathToRemove = "Android/data/souch.smp/files";
        for (File dir: getBaseContext().getExternalFilesDirs(null)) {
            if (dir.getAbsolutePath().endsWith(userPathToRemove)) {
                dirsToScan.add(dir.getParentFile().getParentFile().getParentFile().getParentFile());
            }
        }

        for (File dir: dirsToScan) {
            Log.d("Settings", "userDir: " + dir.getAbsolutePath());
        }
        return dirsToScan;
    }

    public void listFiles(File directory, ArrayList<File> files) {
        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listFiles(file, files);
            }
        }
    }

}
