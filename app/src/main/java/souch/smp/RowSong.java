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

import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;

public class RowSong extends Row {
    private long id;
    private String title;
    private String artist;
    private String album;
    private int duration;
    private int track;
    // full filename
    private String path;
    // folder of the path
    private String folder;

    public RowSong(int pos, int offset, long songID, String songTitle, String songArtist, String songAlbum,
                   int dur, int songTrack, String songPath, String rootFolder) {
        super(pos, offset, Typeface.NORMAL);
        id = songID;
        title = songTitle;
        artist = songArtist;
        album = songAlbum;
        duration = dur;
        track = songTrack;
        path = songPath;
        if(path != null) {
            folder = (new File(path.replaceFirst(rootFolder, ""))).getParent();
            if (folder == null)
                folder = ".";
        }
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getAlbum(){return album;}
    public int getDuration(){return duration;}
    public int getTrack(){return track;}
    public String getPath(){return path;}
    public String getFolder(){return folder;}

    public void setText(TextView text) {
        super.setText(text);
        text.setText(title);
        text.setTextColor(Color.WHITE);
    }

    public void setDurationText(TextView text) {
        text.setText(secondsToMinutes(getDuration()) + "  ");
        text.setTextColor(Color.WHITE);
        text.setTypeface(null, typeface);
        text.setBackgroundColor(Color.BLACK);
    }

    public String toString() {
        return "Song pos: " + genuinePos + " level: " + level + " ID: " + id + " artist: " + artist +
                " album: " + album + " title: " + title + " " +
                secondsToMinutes(duration) + " track:" + track + " path: " + path;
    }

    static public String secondsToMinutes(int duration){
        long seconds = duration;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.valueOf(minutes) + (seconds < 10 ? ":0" : ":") + String.valueOf(seconds);
    }
}
