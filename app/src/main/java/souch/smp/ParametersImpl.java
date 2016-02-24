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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class ParametersImpl implements Parameters {
    private Context context;
    public ParametersImpl(Context context) {
        this.context = context;
    }

    static public String getDefaultMusicDir() {
        String musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
        if (!musicDir.endsWith(File.separator))
            musicDir += File.separator;
        return musicDir;
    }

    private SharedPreferences getPref() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    private SharedPreferences.Editor getEditor() {
        return getPref().edit();
    }

    public boolean getNoLock() {
        return getPref().getBoolean(PrefKeys.NO_LOCK.name(), false);
    }
    public void setNoLock(boolean noLock) {
        getEditor().putBoolean(PrefKeys.NO_LOCK.name(), noLock).commit();
    }

    public boolean getFollowSong() {
        return getPref().getBoolean(PrefKeys.FOLLOW_SONG.name(), true);
    }

    public void setChooseTextSize(boolean big) {
        getEditor().putBoolean(PrefKeys.TEXT_SIZE_CHOOSED.name(), big).commit();
    }
    public boolean getChoosedTextSize() {
        return getPref().getBoolean(PrefKeys.TEXT_SIZE_CHOOSED.name(),
                Boolean.valueOf(context.getString(R.string.settings_text_size_choosed_default)));
    }
    public int getBigTextSize() {
        return Integer.valueOf(getPref().getString(PrefKeys.TEXT_SIZE_BIG.name(),
                context.getString(R.string.settings_text_size_big_default)));
    }
    public int getNormalTextSize() {
        return Integer.valueOf(getPref().getString(PrefKeys.TEXT_SIZE_NORMAL.name(),
                context.getString(R.string.settings_text_size_regular_default)));
    }
    public float getTextSizeRatio() {
        return Float.valueOf(getPref().getString(PrefKeys.TEXT_SIZE_RATIO.name(),
                context.getString(R.string.settings_text_size_ratio_default)));
    }


    public long getSongID() {
        return getPref().getLong(PrefKeys.SONG_ID.name(), -1);
    }
    public void setSongID(long songID) {
        getEditor().putLong(PrefKeys.SONG_ID.name(), songID).commit();
    }

    public boolean getSaveSongPos() {
        return getPref().getBoolean(PrefKeys.SAVE_SONG_POS.name(), false);
    }

    public int getSongPos() {
        return getPref().getInt(PrefKeys.SONG_POS.name(), -1);
    }
    public void setSongPos(int songPos) {
        getEditor().putInt(PrefKeys.SONG_POS.name(), songPos).commit();
    }


    public Filter getFilter() {
        return Filter.valueOf(getPref().getString(PrefKeys.FILTER.name(), Filter.TREE.name()));
    }
    public void setFilter(Filter filter) {
        getEditor().putString(PrefKeys.FILTER.name(), filter.name()).commit();
    }

    public String getRootFolder() {
        return getPref().getString(PrefKeys.ROOT_FOLDER.name(), getDefaultMusicDir());
    }


    public int getDefaultFold() {
        return Integer.valueOf(getPref().getString(PrefKeys.DEFAULT_FOLD.name(), "0"));
    }

    public boolean getUnfoldSubGroup() {
        return getPref().getBoolean(PrefKeys.UNFOLD_SUBGROUP.name(), false);
    }

    public int getUnfoldSubGroupThreshold() {
        return Integer.valueOf(getPref().getString(PrefKeys.UNFOLD_SUBGROUP_THRESHOLD.name(),
                context.getString(R.string.settings_unfold_subgroup_threshold_default)));
    }

    public boolean getEnableShake() {
        return getPref().getBoolean(PrefKeys.ENABLE_SHAKE.name(), false);
    }

    public void setEnableShake(boolean shakeEnabled) {
        getEditor().putBoolean(PrefKeys.ENABLE_SHAKE.name(), shakeEnabled).commit();
    }

    public float getShakeThreshold() {
        return Float.valueOf(getPref().getString(PrefKeys.SHAKE_THRESHOLD.name(),
                context.getString(R.string.settings_default_shake_threshold)));
    }

    public boolean getMediaButtonStartAppShake() {
        return getPref().getBoolean(PrefKeys.MEDIA_BUTTON_START_APP.name(), true);
    }

    public boolean getVibrate() {
        return getPref().getBoolean(PrefKeys.VIBRATE.name(), true);
    }

    public boolean getShuffle() {
        return getPref().getBoolean(PrefKeys.SHUFFLE.name(), false);
    }

    public boolean getScrobble() {
        return getPref().getBoolean(PrefKeys.SCROBBLE.name(), false);
    }
}
