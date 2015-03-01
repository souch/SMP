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

public interface Parameters {
    public boolean getNoLock();
    public void setNoLock(boolean noLock);

    public boolean getFollowSong();

    public void setChooseTextSize(boolean big);
    public boolean getChoosedTextSize();
    public int getBigTextSize();
    public int getNormalTextSize();
    public float getTextSizeRatio();

    public long getSongID();
    public void setSongID(long songID);

    public Filter getFilter();
    public void setFilter(Filter filter);

    public String getRootFolder();

    public int getDefaultFold();

    public boolean getEnableShake();
    public void setEnableShake(boolean shakeEnabled);

    public float getShakeThreshold();
}
