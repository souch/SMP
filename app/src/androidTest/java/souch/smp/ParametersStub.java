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

public class ParametersStub implements Parameters {
    public ParametersStub() {}

    public boolean getNoLock() { return true; }
    public void setNoLock(boolean noLock) {}

    public boolean getFollowSong() {
        return true;
    }

    public void setChooseTextSize(boolean big) {}
    public boolean getChoosedTextSize() {
        return true;
    }
    public int getBigTextSize() {
        return 17;
    }
    public int getNormalTextSize() {
        return 15;
    }
    public float getTextSizeRatio() {
        return 1.1f;
    }


    public long getSongID() {
        return -1;
    }
    public void setSongID(long songID) {}

    public boolean getSaveSongPos() {
        return false;
    }

    public int getSongPos() {
        return -1;
    }
    public void setSongPos(int songPos) {}

    public Filter getFilter() {
        return Filter.FOLDER;
    }
    public void setFilter(Filter filter) {}

    public String getRootFolder() {
        return ParametersImpl.getDefaultMusicDir();
    }


    public int getDefaultFold() {
        return 0;
    }

    public boolean getUnfoldSubGroup() {
        return true;
    }

    public int getUnfoldSubGroupThreshold() {
        return 20;
    }

    public boolean getEnableShake() {
        return false;
    }

    public void setEnableShake(boolean shakeEnabled) {}

    public float getShakeThreshold() {
        return 100;
    }

    public boolean getMediaButtonStartAppShake() { return true; }

    public boolean getVibrate() { return true; }

    public boolean getShuffle() { return false; }

    public boolean getScrobble() { return false; }
}
