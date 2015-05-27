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
import android.content.Intent;
import android.util.Log;

public class Scrobble {
    private Rows rows;
    private Parameters params;
    private Context context;

    // from API spcecification
    public static final int SCROBBLE_START = 0;
    public static final int SCROBBLE_RESUME = 1;
    public static final int SCROBBLE_PAUSE = 2;
    public static final int SCROBBLE_COMPLETE = 3;

    private boolean started;
    private String artist;
    private String album;
    private String track;
    private int duration;


    public Scrobble(Rows rows, Parameters params, Context ctx) {
        this.rows = rows;
        this.params = params;
        this.context = ctx;

        started = false;
    }

    // can be called with SCROBBLE_COMPLETE state twice or more
    public void send(int scrobbleState) {
        if (!params.getScrobble())
            return;

        RowSong rowSong = rows.getCurrSong();
        if (rowSong == null) {
            Log.w("MusicService", "scrobbleSend exit as rowSong is null!");
            return;
        }

        if (scrobbleState == SCROBBLE_START) {
            started = true;
            // save scrobble info for next state
            artist = rowSong.getArtist();
            album = rowSong.getAlbum();
            track = rowSong.getTitle();
            duration = rowSong.getDuration();
        } else if (scrobbleState == SCROBBLE_COMPLETE) {
            // send complete only if SCROBBLE_START was send before
            if (!started)
                return;

            started = false;
        }

        Intent bCast;
        // from https://github.com/tgwizard/sls/blob/master/Developer%27s%20API.md
        // do not enable Simple Last.FM Droid broadcast as Simple Last.FM Droid handles Scrobble Droid broadcast
        /*
        bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
        bCast.putExtra("state", scrobbleState);
        bCast.putExtra("app-name", context.getResources().getString(R.string.app_name));
        bCast.putExtra("app-package", context.getPackageName());
        bCast.putExtra("artist", artist);
        bCast.putExtra("album", album);
        bCast.putExtra("track", track);
        bCast.putExtra("duration", duration);
        bCast.putExtra("source", "P");
        context.sendBroadcast(bCast);
        */

        // from https://code.google.com/p/scrobbledroid/wiki/DeveloperAPI
        bCast = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
        bCast.putExtra("playing", scrobbleState == SCROBBLE_START || scrobbleState == SCROBBLE_RESUME);
        bCast.putExtra("artist", artist);
        bCast.putExtra("album", album);
        bCast.putExtra("track", track);
        bCast.putExtra("secs", duration);
        bCast.putExtra("source", "P");
        context.sendBroadcast(bCast);

        Log.d("MusicService", "scrobbleSend " + scrobbleState + " : " + artist + " - " + track);
    }
}
