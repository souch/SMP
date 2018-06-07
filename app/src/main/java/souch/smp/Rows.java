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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class Rows {

    private Random random;
    private ArrayList<Integer> shuffleSavedPos;

    private ContentResolver musicResolver;
    private Parameters params;

    private Filter filter;

    // id of the song at last exiting
    private long savedID;

    // todo: see if another Collection than ArrayList would give better perf and code simplicity
    private ArrayList<Row> rows;
    private ArrayList<Row> rowsUnfolded;
    // current selected position within rowsUnfolded
    // never assign this directly, instead use setCurrPos
    private int currPos;

    private Resources resources;

    static final public String defaultStr = "<null>";
    private RepeatMode repeatMode;

    public Rows(ContentResolver resolver, Parameters params, Resources resources) {
        this.resources = resources;
        this.params = params;
        musicResolver = resolver;
        currPos = -1;

        random = new Random();
        shuffleSavedPos = new ArrayList<>();

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
    /*
    public void select(int pos) {
        if(rows.get(pos).getClass() == RowSong.class) {
            setCurrPos(rows.get(pos).getGenuinePos());
        }
        else {
            invertFold(pos);
        }
    }
    */

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

    public void moveToRandomSong() {
        if (rowsUnfolded.size() <= 0)
            return;

        if (repeatMode == RepeatMode.REPEAT_GROUP) {
            int firstSongPos = getFirstSongPosInGroup(currPos);
            int lastSongPos = getLastSongPosInGroup(currPos);
            if (lastSongPos <= firstSongPos)
                return;
            // save the random song chosen
            shuffleSavedPos.add(currPos);

            // pick a new pos
            int oldPos = currPos;
            while (oldPos == currPos) {
                // +1 -> next int n is exclusive
                currPos = firstSongPos + random.nextInt((lastSongPos - firstSongPos) + 1);
            }
        }
        else {
            // save the random song chosen
            shuffleSavedPos.add(currPos);

            int pos;
            do {
                pos = random.nextInt(rowsUnfolded.size());
            } while (pos == currPos || rowsUnfolded.get(pos).getClass() != RowSong.class);

            setGroupSelectedState(currPos, false);

            currPos = pos;

            setGroupSelectedState(currPos, true);
        }
    }

    // return the pos of the last song belonging to the given songPos group
    int getLastSongPosInGroup(int songPos) {
        Row currParent = rowsUnfolded.get(songPos).parent;
        songPos++;
        // if next row is the end of the list or a group or a different group, we reached another group
        while (songPos < rowsUnfolded.size() &&
                rowsUnfolded.get(songPos).getClass() == RowSong.class &&
                rowsUnfolded.get(songPos).parent == currParent) {
            songPos++;
        }
        return songPos - 1;
    }

    // return the pos of the first song belonging to the given songPos group
    int getFirstSongPosInGroup(int songPos) {
        Row currParent = rowsUnfolded.get(songPos).parent;
        songPos--;
        while (songPos > 0 &&
                rowsUnfolded.get(songPos).getClass() == RowSong.class &&
                rowsUnfolded.get(songPos).parent == currParent) {
            songPos--;
        }
        return songPos + 1;
    }

    // go back to previous random song done
    public void moveToRandomSongBack() {
        if (rowsUnfolded.size() <= 0)
            return;

        boolean backOk = false;
        if (shuffleSavedPos.size() > 0) {
            int pos = shuffleSavedPos.remove(shuffleSavedPos.size() - 1);
            // check
            if (pos < rowsUnfolded.size() && rowsUnfolded.get(pos).getClass() == RowSong.class) {
                backOk = true;
                setGroupSelectedState(currPos, false);
                currPos = pos;
                setGroupSelectedState(currPos, true);
            }
        }
        // if no saved pos, fallback to prevsong
        if (!backOk)
            moveToPrevSong();
    }

    public void moveToNextSong() {
        if (repeatMode == RepeatMode.REPEAT_GROUP) {
            int lastSongPos = getLastSongPosInGroup(currPos);
            if (currPos == lastSongPos)
                currPos = getFirstSongPosInGroup(currPos);
            else
                currPos++;
        }
        else {
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
    }

    public void moveToPrevSong() {
        if (repeatMode == RepeatMode.REPEAT_GROUP) {
            int firstSongPos = getFirstSongPosInGroup(currPos);
            if (currPos == firstSongPos)
                currPos = getLastSongPosInGroup(currPos);
            else
                currPos--;
        }
        else {
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
    }

    public void moveToPrevGroup() {
        setGroupSelectedState(currPos, false);

        currPos = getFirstSongPosInGroup(currPos);
        currPos--;

        if (currPos < 0)
            currPos = rowsUnfolded.size() - 1;

        while (currPos >= 0 && rowsUnfolded.get(currPos).getClass() != RowSong.class) {
            currPos--;
            if (currPos < 0)
                currPos = rowsUnfolded.size() - 1;
        }

        if (currPos < 0)
            currPos = rowsUnfolded.size() - 1;

        currPos = getFirstSongPosInGroup(currPos);

        setGroupSelectedState(currPos, true);
    }

    public void moveToNextGroup() {
        setGroupSelectedState(currPos, false);

        currPos = getLastSongPosInGroup(currPos);
        currPos++;

        // if last song go to beginning
        if (currPos == rowsUnfolded.size()) {
            currPos = 0;
        }

        // skip RowGroups
        while (currPos < rowsUnfolded.size() &&
                rowsUnfolded.get(currPos).getClass() != RowSong.class)
            currPos++;

        if (currPos == rowsUnfolded.size()) {
            currPos = -1;
        }

        setGroupSelectedState(currPos, true);
    }

    // fold everything
    public void fold() {
        // todo: better to recopy first level from unfolded?
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
        if (pos < 0 || pos >= rows.size()) {
            return;
        }
        if(rows.get(pos).getClass() != RowGroup.class) {
            Log.w("Rows", "invertFold called on class that is not SongGroup!");
            return;
        }
        RowGroup group = (RowGroup) rows.get(pos);

        if(group.isFolded()) {
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


    // @desc unfold only the group(s) that contains pos.
    //
    // @return true if at least one group has been unfold
    public boolean unfoldCurrPos() {
        boolean changed = false;
        int pos = getCurrPos();
        if (pos < 0 || pos >= rows.size())
            return changed;

        Row row = rows.get(pos);
        if (row != null && row.getClass() == RowGroup.class) {
            RowGroup group = (RowGroup) row;
            if (group.isFolded()) {
                unfold(group, pos);
                unfoldCurrPos();
                changed = true;
            }
        }
        return changed;
    }

    private boolean hasOneSubGroup(RowGroup group, int pos) {
        if (group.getLevel() != 0)
            return true;

        int nbSubGroup = 0;
        Row row;
        for (int i = 1;
             group.getGenuinePos() + i < rowsUnfolded.size() &&
                     (row = rowsUnfolded.get(group.getGenuinePos() + i)).getLevel() > group.getLevel();
             i++) {
            if (row.getClass() == RowGroup.class) {
                nbSubGroup++;
                if (nbSubGroup > 1)
                    return false;
            }
        }

        return true;
    }

    // @desc unfold a group following settings
    //
    // group and pos must correspond in the foldable rows
    // group must be folded
    private void unfold(RowGroup group, int pos) {
        if (filter == Filter.TREE) {
            unfoldTree(group, pos);
            return;
        }

        // add every missing rows
        Row row;
        final int autoUnfoldThreshold = params.getUnfoldSubGroupThreshold();
        if (params.getUnfoldSubGroup() ||
                group.getLevel() != 0 ||
                group.nbRowSong() < autoUnfoldThreshold ||
                hasOneSubGroup(group, pos)) {
            // unfold everything
            for (int i = 1;
                 group.getGenuinePos() + i < rowsUnfolded.size() &&
                         (row = rowsUnfolded.get(group.getGenuinePos() + i)).getLevel() > group.getLevel();
                 i++) {
                // unfold if previously folded
                if (row.getClass() == RowGroup.class)
                    ((RowGroup) row).setFolded(false);

                rows.add(pos + i, row);
                //Log.d("Rows", "Item added pos: " + pos + i + " row: " + songItem);
            }
        }
        else {
            // unfold only first subgroup
            for (int i = 1, j = 1;
                 group.getGenuinePos() + i < rowsUnfolded.size() &&
                         (row = rowsUnfolded.get(group.getGenuinePos() + i)).getLevel() > group.getLevel();
                 i++) {
                if (row.getClass() == RowGroup.class) {
                    ((RowGroup) row).setFolded(true);
                    rows.add(pos + j++, row);
                }
            }
        }
        group.setFolded(false);
    }

    private void unfoldTree(RowGroup group, int pos) {
        Row row;
        // unfold only next level
        for (int i = 1, j = 1;
             group.getGenuinePos() + i < rowsUnfolded.size() &&
                     (row = rowsUnfolded.get(group.getGenuinePos() + i)).getLevel() > group.getLevel();
             i++) {
            if (row.getLevel() == group.getLevel() + 1) {
                if (row.getClass() == RowGroup.class) {
                    ((RowGroup) row).setFolded(true);
                }
                rows.add(pos + j++, row);
            }
        }
        group.setFolded(false);
    }


    public boolean isLastRow(int pos) {
        return pos == rows.size() - 1;
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
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR
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
            case TREE:
            case FOLDER:
                // presort it even if it will be restorted by tree and folder, in order to have a
                // title sort if there is no ID3 track
                sortOrder = MediaStore.Audio.Media.ARTIST +
                        ", " + MediaStore.Audio.Media.ALBUM +
                        ", " + MediaStore.Audio.Media.TRACK +
                        ", " + MediaStore.Audio.Media.TITLE;
                // did not find a way to sort by folder through query
                break;
            default:
                return;
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
            case TREE:
                initByTree(musicCursor);
                break;
            default:
                return;
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

        switch (params.getDefaultFold()) {
            case 0:
                // fold
                initRowsFolded();
                break;
            default:
                // unfolded
                // shallow copy
                rows = (ArrayList<Row>) rowsUnfolded.clone();
        }

        // to comment in release mode:
        /*
        int idx;
        for(idx = 0; idx < rowsUnfolded.size(); idx++)
            Log.d("Rows", "songItem " + idx + " added: " + rowsUnfolded.get(idx).toString());
        */
        Log.d("Rows", "======> songItems initialized in " + (System.currentTimeMillis() - startTime) + "ms");
        Log.d("Rows", "songPos: " + currPos);
    }

    private void initRowsFolded() {
        for(Row row : rowsUnfolded) {
            if(row.getClass() == RowGroup.class && row.getLevel() == 0) {
                rows.add(row);
                ((RowGroup) row).setFolded(true);
            }
        }
    }

    private void initByArtist(Cursor musicCursor) {
        RowGroup.rowType = Filter.ARTIST;
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            int albumIdCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int yearCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);

            int RowGroupArtistBackground = 0;
            int RowGroupArtistAlbumBackground = 0;
            if (resources != null) {
                RowGroupArtistBackground = resources.getColor(R.color.RowGroupArtistBackground);
                RowGroupArtistAlbumBackground = resources.getColor(R.color.RowGroupArtistAlbumBackground);
            }

            RowGroup prevArtistGroup = null;
            RowGroup prevAlbumGroup = null;
            do {
                long id = musicCursor.getLong(idCol);
                String title = getDefaultStrIfNull(musicCursor.getString(titleCol));
                String artist = getDefaultStrIfNull(musicCursor.getString(artistCol));
                String album = getDefaultStrIfNull(musicCursor.getString(albumCol));
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);
                long albumId = musicCursor.getLong(albumIdCol);
                int year = musicCursor.getInt(yearCol);

                if (prevArtistGroup == null || artist.compareToIgnoreCase(prevArtistGroup.getName()) != 0) {
                    RowGroup artistGroup = new RowGroup(rowsUnfolded.size(), 0, artist,
                            Typeface.BOLD, RowGroupArtistBackground);
                    rowsUnfolded.add(artistGroup);
                    prevArtistGroup = artistGroup;
                    prevAlbumGroup = null;
                }

                if (prevAlbumGroup == null || album.compareToIgnoreCase(prevAlbumGroup.getName()) != 0) {
                    RowGroup albumGroup = new RowGroup(rowsUnfolded.size(), 1, album,
                            Typeface.ITALIC, RowGroupArtistAlbumBackground);
                    albumGroup.setParent(prevArtistGroup);
                    rowsUnfolded.add(albumGroup);
                    prevAlbumGroup = albumGroup;
                }

                RowSong rowSong = new RowSong(rowsUnfolded.size(), 2, id, title, artist, album,
                        duration / 1000, track, null, albumId, year);
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
        RowGroup.rowType = Filter.FOLDER;
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int pathCol = musicCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            int albumIdCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int yearCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);

            do {
                long id = musicCursor.getLong(idCol);
                String title = getDefaultStrIfNull(musicCursor.getString(titleCol));
                String artist = getDefaultStrIfNull(musicCursor.getString(artistCol));
                String album = getDefaultStrIfNull(musicCursor.getString(albumCol));
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);
                String path = getDefaultStrIfNull(musicCursor.getString(pathCol));
                long albumId = musicCursor.getLong(albumIdCol);
                int year = musicCursor.getInt(yearCol);

                RowSong rowSong = new RowSong(-1, 2, id, title, artist, album, duration / 1000, track, path, albumId, year);
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

        int RowGroupFolderBackground = 0;
        int RowGroupArtistBackground = 0;
        if (resources != null) {
            RowGroupFolderBackground = resources.getColor(R.color.RowGroupFolderBackground);
            RowGroupArtistBackground = resources.getColor(R.color.RowGroupFolderArtistBackground);
        }

        // add group
        RowGroup prevFolderGroup = null;
        RowGroup prevArtistGroup = null;

        for (int idx = 0; idx < rowsUnfolded.size(); idx++) {
            RowSong rowSong = (RowSong) rowsUnfolded.get(idx);

            String curFolder = rowSong.getFolder();
            if (prevFolderGroup == null || curFolder.compareToIgnoreCase(prevFolderGroup.getName()) != 0) {
                RowGroup folderGroup = new RowGroup(idx, 0, curFolder,
                        Typeface.BOLD, RowGroupFolderBackground);
                rowsUnfolded.add(idx, folderGroup);
                idx++;
                prevFolderGroup = folderGroup;
                prevArtistGroup = null;
            }

            String curArtist = rowSong.getArtist();
            if (prevArtistGroup == null || curArtist.compareToIgnoreCase(prevArtistGroup.getName()) != 0) {
                RowGroup artistGroup = new RowGroup(idx, 1, curArtist,
                        Typeface.BOLD, RowGroupArtistBackground);
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

    private void initByTree(Cursor musicCursor) {
        RowGroup.rowType = Filter.TREE;
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int pathCol = musicCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            int albumIdCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int yearCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);

            do {
                long id = musicCursor.getLong(idCol);
                String title = getDefaultStrIfNull(musicCursor.getString(titleCol));
                String artist = getDefaultStrIfNull(musicCursor.getString(artistCol));
                String album = getDefaultStrIfNull(musicCursor.getString(albumCol));
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);
                String path = getDefaultStrIfNull(musicCursor.getString(pathCol));
                long albumId = musicCursor.getLong(albumIdCol);
                int year = musicCursor.getInt(yearCol);

                final int pos = -1, level = 2;
                RowSong rowSong = new RowSong(pos, level, id, title, artist, album, duration / 1000,
                        track, path, albumId, year);
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
//                int cmp = a.getFolder().compareToIgnoreCase(b.getFolder());
                int cmp = Path.compareToIgnoreCaseShorterFolderLast(a.getFolder(), b.getFolder());
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

        int RowGroupFolderBackground = 0;
        if (resources != null)
            RowGroupFolderBackground = resources.getColor(R.color.RowGroupTreeBackground);

        // add groups
        ArrayList<RowGroup> prevGroups = new ArrayList<>();
        for (int idx = 0; idx < rowsUnfolded.size(); idx++) {
            RowSong rowSong = (RowSong) rowsUnfolded.get(idx);
            // get folder list of current row
            ArrayList<String> folders = Path.tokenizeFolder(rowSong.getFolder());

            //// get the nearest common group parent
            // search from the bottom the last previous group that is the same with the current group
            // /toto/tata/youp, /to/tata/gruick -> firstDiff = 0
            // /toto/tata/youp, /toto/titi/gruick -> firstDiff = 1
            // /toto/tata/youp, /toto/tata/gruick -> firstDiff = 2
            int commonLevel = 0;
            while (commonLevel < prevGroups.size() &&
                    commonLevel < folders.size() &&
                    prevGroups.get(commonLevel).getName().equalsIgnoreCase(folders.get(commonLevel)))
                commonLevel++;
            // get corresponding RowGroup
            RowGroup commonGroup;
            if (commonLevel == 0)
                // everything is different: no parent
                commonGroup = null;
            else
                commonGroup = prevGroups.get(commonLevel - 1);

            //// add every groups that are missing
            RowGroup parentGroup = commonGroup;
            for (int level = commonLevel; level < folders.size(); level++) {
                RowGroup aGroup = new RowGroup(idx, level, folders.get(level),
                        Typeface.BOLD, RowGroupFolderBackground);
                aGroup.setParent(parentGroup);
                parentGroup = aGroup;
                rowsUnfolded.add(idx, aGroup);
                idx++;
            }

            //// recompute group list for next row
            prevGroups.clear();
            RowGroup groupIdx = parentGroup;
            while (groupIdx != null) {
                // update group
                groupIdx.incNbRowSong();

                prevGroups.add(0, groupIdx);
                groupIdx = (RowGroup) groupIdx.getParent();
            }

            //// update RowSong
            rowSong.setLevel(folders.size());
            rowSong.setGenuinePos(idx);
            rowSong.setParent(parentGroup);
            if (rowSong.getID() == savedID)
                currPos = idx;
        }

        setGroupSelectedState(currPos, true);
    }


    private String getDefaultStrIfNull(String str) { return str != null ? str : defaultStr; }

    private void restore() {
        savedID = params.getSongID();
        filter = params.getFilter();
        repeatMode = params.getRepeatMode();
        Path.rootFolders = params.getRootFolders();
    }

    public void save() {
        updateSavedId();
        params.setSongID(savedID);
        params.setFilter(filter);
        params.setRepeatMode(repeatMode);
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

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public boolean setRootFolders(String rootFolders) {
        boolean reinited = false;

        if (!Path.rootFolders.equals(rootFolders)) {
            Path.rootFolders = rootFolders;
            if (filter == Filter.FOLDER || filter == Filter.TREE) {
                // reinit everything is a bit heavy: nevermind, rootFolders will not be changed often
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

//    public void deleteCurSong(Context context) {
//        RowSong rowSong = getCurrSong();
//        if(rowSong != null)
//            rowSong.delete(context);
//    }
}
