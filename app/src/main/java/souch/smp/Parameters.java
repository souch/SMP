/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

public interface Parameters {
    boolean getNoLock();
    void setNoLock(boolean noLock);

    boolean getFollowSong();

    void setChooseTextSize(boolean big);
    boolean getChoosedTextSize();
    int getBigTextSize();
    int getNormalTextSize();
    float getTextSizeRatio();

    long getSongID();
    void setSongID(long songID);

    boolean getSaveSongPos();

    int getSongPos();
    void setSongPos(int songPos);

    Filter getFilter();
    void setFilter(Filter filter);

    RepeatMode getRepeatMode();
    void setRepeatMode(RepeatMode repeatMode);

    String getRootFolders();

    int getDefaultFold();

    boolean getUnfoldSubGroup();
    int getUnfoldSubGroupThreshold();

    boolean getEnableShake();
    void setEnableShake(boolean shakeEnabled);

    float getShakeThreshold();

    boolean getMediaButtonStartAppShake();

    boolean getVibrate();

    boolean getShuffle();

    boolean getScrobble();
}
