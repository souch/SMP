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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Rows {

    private ContentResolver musicResolver;
    private Context context;

    private Filter filter;
    private String rootFolder;

    // id of the song at last exiting
    private long savedID;

    // todo: see if another Collection than ArrayList would give better perf and code simplicity
    private ArrayList<Row> rows;
    private ArrayList<Row> rowsUnfolded;
    // current selected position within rowsUnfolded
    // never assign this directly, instead use setCurrPos
    private int currPos;

    public Rows(ContentResolver resolver, Context theContext) {
        musicResolver = resolver;
        context = theContext;
        currPos = -1;

        rowsUnfolded = new ArrayList<>();
        rows = new ArrayList<>();

        restore();
        init();
    }

    // size of the foldable array
    public int size() {
        return rows.size();
    }

    // the user choose a row
    public void select(int pos) {
        if(rows.get(pos).getClass() == RowSong.class) {
            setCurrPos(rows.get(pos).getGenuinePos());
        }
        else {
            invertFold(pos);
        }
    }

    // select first song encountered from pos
    public void selectNearestSong(int pos) {
        Row row = rows.get(pos);
        while (row.getClass() != RowSong.class)
            row = rowsUnfolded.get(row.getGenuinePos() + 1);
        setCurrPos(row.getGenuinePos());
    }

    // get row from the foldable array
    public Row get(int pos) {
        Row row = null;
        if (pos >= 0 && pos < rows.size())
            row = rows.get(pos);
        return row;
    }

    // get the song currently selected (playing or paused) from the unfoldable array
    public RowSong getCurrSong() {
        Row row = null;
        if (currPos >= 0 && currPos < rowsUnfolded.size()) {
            row = rowsUnfolded.get(currPos);
            if (row.getClass() != RowSong.class)
                row = null;
        }
        return (RowSong) row;
    }

    // get the currently selected row (group or song) from the foldable array
    public int getCurrPos() {
        int pos = -1;
        Row song = getCurrSong();
        int i;
        for (i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row == song ||
                    (row.getClass() == RowGroup.class &&
                            ((RowGroup) row).isSelected() &&
                            ((RowGroup) row).isFolded()))
                break;
        }
        if (i < rows.size())
            pos = i;
        return pos;
    }

    private void setCurrPos(int pos) {
        setGroupSelectedState(currPos, false);
        currPos = pos;
        setGroupSelectedState(currPos, true);
    }

    private void setGroupSelectedState(int pos, boolean selected) {
        if (pos >= 0 && pos < rowsUnfolded.size()) {
            RowGroup group = (RowGroup) rowsUnfolded.get(pos).getParent();
            while (group != null) {
                group.setSelected(selected);
                group = (RowGroup) group.getParent();
            }
        }
    }

    public void moveToNextSong() {
        setGroupSelectedState(currPos, false);

        currPos++;
        if (currPos >= rowsUnfolded.size())
            currPos = 0;

        while (currPos < rowsUnfolded.size() &&
                rowsUnfolded.get(currPos).getClass() != RowSong.class)
            currPos++;

        if (currPos == rowsUnfolded.size())
            currPos = -1;

        setGroupSelectedState(currPos, true);
    }

    public void moveToPrevSong() {
        setGroupSelectedState(currPos, false);

        currPos--;
        if (currPos < 0)
            currPos = rowsUnfolded.size() - 1;

        while (currPos >= 0 && rowsUnfolded.get(currPos).getClass() != RowSong.class) {
            currPos--;
            if (currPos < 0)
                currPos = rowsUnfolded.size() - 1;
        }

        setGroupSelectedState(currPos, true);
    }

    // fold everything
    public void fold() {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.getClass() == RowGroup.class)
                fold((RowGroup) rows.get(i), i);
        }
    }

    // unfold everything
    public void unfold() {
        rows = (ArrayList<Row>) rowsUnfolded.clone();
        for(Row row : rows)
            if (row.getClass() == RowGroup.class)
                ((RowGroup) row).setFolded(false);
    }

    public void invertFold(int pos) {
        if(rows.get(pos).getClass() != RowGroup.class) {
            Log.w("Rows", "invertFold called on class that is not SongGroup!");
            return;
        }
        RowGroup group = (RowGroup) rows.get(pos);

        if(group.isFolded()) {
            // avoid duplication
            fold(group, pos);

            unfold(group, pos);
        }
        else {
            fold(group, pos);
        }
    }

    // group and pos must correspond in the foldable rows
    private void fold(RowGroup group, int pos) {
        pos++;
        // remove every following rows that has a higher level
        while(pos < rows.size() && rows.get(pos).getLevel() > group.getLevel()) {
            //Log.d("Rows", "Item removed pos: " + pos + " row: " + songItems.get(pos));
            rows.remove(pos);
        }
        group.setFolded(true);
    }

     // group and pos must correspond in the foldable rows
    private void unfold(RowGroup group, int pos) {
        // add every missing rows
        Row row;
        for (int i = 1;
             group.getGenuinePos() + i < rowsUnfolded.size() &&
                     (row = rowsUnfolded.get(group.getGenuinePos() + i)).getLevel() > group.getLevel() ;
             i++) {
            // todo: keeps the sub group folded?
            // unfold if previously folded
            if(row.getClass() == RowGroup.class)
                ((RowGroup) row).setFolded(false);

            rows.add(pos + i, row);
            //Log.d("Rows", "Item added pos: " + pos + i + " row: " + songItem);
        }
        group.setFolded(false);
    }



    public void init() {
        rowsUnfolded.clear();
        rows.clear();

        long startTime = System.currentTimeMillis();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor;
        String[] projection = new String[] {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.MediaColumns.DATA,
                MediaStore.Audio.Media.TRACK
        };
        String where = MediaStore.Audio.Media.IS_MUSIC + "=1";

        String sortOrder = null;
        switch(filter) {
            case ARTIST:
                sortOrder = MediaStore.Audio.Media.ARTIST +
                        ", " + MediaStore.Audio.Media.ALBUM +
                        ", " + MediaStore.Audio.Media.TRACK +
                        ", " + MediaStore.Audio.Media.TITLE;
                break;
            case FOLDER:
                // did not find a way to sort by folder through query
                break;
        }
        try {
            musicCursor = musicResolver.query(musicUri, projection, where, null, sortOrder);
        } catch (Exception e) {
            final String msg = "No songItems found!";
            //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            Log.e("MusicService", msg);
            return;
        }

        switch(filter) {
            case ARTIST:
                initByArtist(musicCursor);
                break;
            case FOLDER:
                initByPath(musicCursor);
                break;
        }

        if(musicCursor != null)
            musicCursor.close();

        // if no songPos saved : search the first song
        if(currPos == -1) {
            int idx;
            for(idx = 0; idx < rowsUnfolded.size(); idx++) {
                if (rowsUnfolded.get(idx).getClass() == RowSong.class) {
                    setCurrPos(idx);
                    break;
                }
            }
        }

        // shallow copy
        rows = (ArrayList<Row>) rowsUnfolded.clone();

        // to comment in release mode:
        /*
        int idx;
        for(idx = 0; idx < rowsUnfolded.size(); idx++)
            Log.d("Rows", "songItem " + idx + " added: " + rowsUnfolded.get(idx).toString());
        */
        Log.d("Rows", "songItems initialized in " + (System.currentTimeMillis() - startTime) + "ms");
        Log.d("Rows", "songPos: " + currPos);
    }

    private void initByArtist(Cursor musicCursor) {
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            RowGroup prevArtistGroup = null;
            RowGroup prevAlbumGroup = null;
            do {
                long id = musicCursor.getLong(idCol);
                String title = musicCursor.getString(titleCol);
                String artist = musicCursor.getString(artistCol);
                String album = musicCursor.getString(albumCol);
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);

                if (prevArtistGroup == null || artist.compareToIgnoreCase(prevArtistGroup.getName()) != 0) {
                    RowGroup artistGroup = new RowGroup(rowsUnfolded.size(), 0, artist,
                            Typeface.BOLD, Color.argb(0x88, 0x35, 0x35, 0x35));
                    rowsUnfolded.add(artistGroup);
                    prevArtistGroup = artistGroup;
                    prevAlbumGroup = null;
                }

                if (prevAlbumGroup == null || album.compareToIgnoreCase(prevAlbumGroup.getName()) != 0) {
                    RowGroup albumGroup = new RowGroup(rowsUnfolded.size(), 1, album,
                            Typeface.ITALIC, Color.argb(0x88, 0x0, 0x0, 0x0));
                    albumGroup.setParent(prevArtistGroup);
                    rowsUnfolded.add(albumGroup);
                    prevAlbumGroup = albumGroup;
                }

                RowSong rowSong = new RowSong(rowsUnfolded.size(), 2, id, title, artist, album,
                        duration / 1000, track, null, rootFolder);
                rowSong.setParent(prevAlbumGroup);

                if(id == savedID)
                    currPos = rowsUnfolded.size();

                rowsUnfolded.add(rowSong);
                prevArtistGroup.incNbRowSong();
                prevAlbumGroup.incNbRowSong();
            }
            while (musicCursor.moveToNext());
            setGroupSelectedState(currPos, true);
        }
    }


    private void initByPath(Cursor musicCursor) {
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int pathCol = musicCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            do {
                long id = musicCursor.getLong(idCol);
                String title = musicCursor.getString(titleCol);
                String artist = musicCursor.getString(artistCol);
                String album = musicCursor.getString(albumCol);
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);
                String path = musicCursor.getString(pathCol);

                RowSong rowSong = new RowSong(-1, 2, id, title, artist, album, duration / 1000, track, path,
                        rootFolder);
                rowsUnfolded.add(rowSong);
                //Log.d("Rows", "song added: " + rowSong.toString());
            }
            while (musicCursor.moveToNext());
        }

        // sort
        Collections.sort(rowsUnfolded, new Comparator<Row>() {
            public int compare(Row first, Row second) {
                // only Song has been added so far, so unchecked cast is ok
                RowSong a = (RowSong) first;
                RowSong b = (RowSong) second;
                int cmp = a.getFolder().compareToIgnoreCase(b.getFolder());
                if (cmp == 0) {
                    cmp = a.getArtist().compareToIgnoreCase(b.getArtist());
                    if (cmp == 0) {
                        cmp = a.getAlbum().compareToIgnoreCase(b.getAlbum());
                        if (cmp == 0) {
                            cmp = a.getTrack() - b.getTrack();
                        }
                    }
                }
                return cmp;
            }
        });

        // add group
        RowGroup prevFolderGroup = null;
        RowGroup prevArtistGroup = null;

        for (int idx = 0; idx < rowsUnfolded.size(); idx++) {
            RowSong rowSong = (RowSong) rowsUnfolded.get(idx);

            String curFolder = rowSong.getFolder();
            if (prevFolderGroup == null || curFolder.compareToIgnoreCase(prevFolderGroup.getName()) != 0) {
                RowGroup folderGroup = new RowGroup(idx, 0, curFolder,
                        Typeface.BOLD, Color.argb(0x88, 0x35, 0x35, 0x35));
                rowsUnfolded.add(idx, folderGroup);
                idx++;
                prevFolderGroup = folderGroup;
                prevArtistGroup = null;
            }

            String curArtist = rowSong.getArtist();
            if (prevArtistGroup == null || curArtist.compareToIgnoreCase(prevArtistGroup.getName()) != 0) {
                RowGroup artistGroup = new RowGroup(idx, 1, curArtist,
                        Typeface.BOLD, Color.argb(0x88, 0x0, 0x0, 0x0));
                artistGroup.setParent(prevFolderGroup);
                rowsUnfolded.add(idx, artistGroup);
                idx++;
                prevArtistGroup = artistGroup;
            }

            if (rowSong.getID() == savedID)
                currPos = idx;

            rowSong.setGenuinePos(idx);
            rowSong.setParent(prevArtistGroup);

            prevFolderGroup.incNbRowSong();
            prevArtistGroup.incNbRowSong();
        }
        setGroupSelectedState(currPos, true);
    }


    private void restore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        savedID = settings.getLong(PrefKeys.SONG_ID.name(), -1);
        filter = Filter.valueOf(settings.getString(PrefKeys.FILTER.name(), Filter.FOLDER.name()));
        Log.d("Rows", "restore savedID: " + savedID);

        rootFolder = settings.getString(PrefKeys.ROOT_FOLDER.name(), Settings.getDefaultMusicDir());
    }

    public void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        updateSavedId();
        Log.d("Rows", "save savedID: " + savedID);
        editor.putLong(PrefKeys.SONG_ID.name(), savedID);
        editor.putString(PrefKeys.FILTER.name(), filter.name());
        editor.commit();
    }

    
    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        if (this.filter != filter) {
            this.filter = filter;
            // todo: handle the current playing song finish during reinitSongs()...
            updateSavedId();
            init();
        }
    }

    public boolean setRootFolder(String rootFolder) {
        boolean reinited = false;

        if (!this.rootFolder.equals(rootFolder)) {
            this.rootFolder = rootFolder;
            if (filter == Filter.FOLDER) {
                // reinit everything is a bit heavy: nevermind, rootFolder will not be changed often
                updateSavedId();
                init();
                reinited = true;
            }
        }

        return  reinited;
    }

    private void updateSavedId() {
        RowSong rowSong = getCurrSong();
        if(rowSong != null)
            savedID = rowSong.getID();
    }
}
