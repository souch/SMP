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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;

// todo refactor Rows in order to mock settings.

public class RowsTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
        Log.d("RowsTest", "====================================");
    }

    public void testEmpty() throws Exception {
        Log.d("RowsTest", "== testEmpty ==");
        Rows rows = initRows(null, Filter.FOLDER);
        checkRowSize(rows, 0);
    }

    public void testNullCursorGetString() throws Exception {
        Log.d("RowsTest", "== testNullCursorGetString ==");
        //String[][] data = new String[][]{{"1", "Salut", null, "head", "80000", "2", null, "1"}};
        String[][] data = new String[][]{{null, null, null, null, null, null, null, "1"}};
        Rows rows;
        rows = initRows(data, Filter.FOLDER);
        checkRowSize(rows, 3);
        checkRowsText(rows, new String[]{ ".", Rows.defaultStr, Rows.defaultStr});
        rows = initRows(data, Filter.ARTIST);
        checkRowSize(rows, 3);
        checkRowsText(rows, new String[]{ Rows.defaultStr, Rows.defaultStr, Rows.defaultStr});
    }

    public void testOneSong() throws Exception {
        Log.d("RowsTest", "== testOneSong ==");
        String[][] data = new String[][]{{"1", "Salut", "Tortoise", "head", "80000", "2", "/mnt", "1"}};
        Rows rows;
        rows = initRows(data, Filter.FOLDER);
        checkRowSize(rows, 3);
        checkRowsText(rows, new String[]{ ".", "Salut", "head"});
        rows = initRows(data, Filter.ARTIST);
        checkRowsText(rows, new String[]{ "Salut", "Tortoise", "head"});
        checkRowSize(rows, 3);
    }

    public void test2DifferentSong() throws Exception {
        Log.d("RowsTest", "== test2DifferentSongs ==");
        String[][] data = new String[][]{
                {"1", "Artist1", "Album2", "title", "80000", "1", "/mnt/sdcard/yo", "1"},
                {"2", "Artist2", "album1", "title1", "80000", "2", "/mnt/sdcard/yo", "1"}
        };
        Rows rows;
        rows = initRows(data, Filter.FOLDER);
        checkRowSize(rows, 5);
        checkRowsText(rows, new String[]{ "/mnt/sdcard", "Artist1", "title", "Artist2", "title1"});
        rows = initRows(data, Filter.ARTIST);
        //Settings.getFoldPref(settings)
        checkRowSize(rows, 6);
        checkRowsText(rows, new String[]{ "Artist1", "Album2", "title", "Artist2", "album1", "title1"});
    }

    public void testMergeNameCase() throws Exception {
        Log.d("RowsTest", "== testMergeNameCase ==");

        // same artist except case
        String[][] data = new String[][]{
                {"1", "Artist1", "Album2", "title1", "80000", "1", "/mnt/sdcard/t1", "1"},
                {"2", "artist1", "album2", "title2", "80000", "2", "/mnt/sdcard/t2", "1"}};
        Rows rows;
        rows = initRows(data, Filter.FOLDER);
        checkRowSize(rows, 4);
        checkRowsText(rows, new String[]{ "/mnt/sdcard", "Artist1", "title1", "title2"});
        rows = initRows(data, Filter.ARTIST);
        checkRowSize(rows, 4);
        checkRowsText(rows, new String[]{ "Artist1", "Album2", "title1", "title2"});
    }

    public void testBiglist() throws Exception {
        Log.d("RowsTest", "== testMergeNameCase ==");

        // same artist except case
        String[][] data = new String[][]{
                {"1", "Artist1", "Album2", "title1", "80000", "1", "/mnt/rock/t1/1", "1"},
                {"2", "Artist1", "Album2", "title2", "80000", "1", "/mnt/rock/t1/2", "1"},
                {"3", "Artist1", "Album2", "title1", "80000", "1", "/mnt/rock/tt1/1", "1"},
                {"4", "Artist2", "Album2", "tit1", "80000", "1", "/mnt/another/t2/1", "1"},
                {"5", "Artist2", "Album2", "tit2", "80000", "1", "/mnt/another/t2/2", "1"},
                {"6", "Artist3", "Album2", "tit3", "80000", "1", "/mnt/another/t2/3", "1"},
                {"7", "artist4", "album2", "title2", "80000", "2", "2", "1"},
                {"8", "artist5", "album1", "title1", "80000", "2", "/app/rock/super/genial/1", "1"}};
        Rows rows;
        rows = initRows(data, Filter.FOLDER);
        checkRowSize(rows, 19);
        checkRowsText(rows, new String[]{
                ".",
                    "artist4",
                        "title2",
                "/app/rock/super/genial",
                    "artist5",
                        "title1",
                "/mnt/another/t2",
                    "Artist2",
                        "tit1",
                        "tit2",
                    "Artist3",
                        "tit3",
                "/mnt/rock/t1",
                    "Artist1",
                        "title1",
                        "title2",
                "/mnt/rock/tt1",
                    "Artist1",
                        "title1",
        });

        rows = initRows(data, Filter.ARTIST);
        checkRowSize(rows, 18);
        checkRowsText(rows, new String[]{
                "Artist1",
                    "Album2",
                        "title1",
                        "title2",
                        "title1",
                "Artist2",
                    "Album2",
                        "tit1",
                        "tit2",
                "Artist3",
                    "Album2",
                        "tit3",
                "artist4",
                    "album2",
                        "title2",
                "artist5",
                    "album1",
                        "title1"

        });
    }

    private Rows initRows(String[][] data, Filter filter) {
        MockContentResolver resolver = new MockContentResolver();
        TestContentProvider provider = new TestContentProvider(getContext(), data);
        resolver.addProvider(MediaStore.AUTHORITY, provider);
        Rows rows = new Rows(resolver, new ParametersStub());
        rows.setFilter(filter);
        rows.init();
        return rows;
    }

    private void checkRowSize(Rows rows, int size) throws Exception {
        ArrayList<Row> arrayRowUnfold = (ArrayList<Row>) getField(rows, "rowsUnfolded");
        if (arrayRowUnfold.size() != size) {
            String msg = "assert arrayRowUnfold size failed. expected: " + size + " actual: " + arrayRowUnfold.size();
            Log.d("RowsTest", msg);
            printRowArray(arrayRowUnfold);
            throw new Exception(msg);
        }
/*
        ArrayList<Row> arrayRow = (ArrayList<Row>) getField(rows, "rows");
        if(arrayRow.size() != size) {
            Log.d("RowsTest", "assert arrayRow size failed. wanted: " + size + " actual: " + arrayRow.size());
            printRowArray(arrayRow);
            throw new Exception("assert rows size failed");
        }*/
    }

    private void checkRowsText(Rows rows, String[] names) throws Exception {
        for (int i = 0; i < names.length; i++)
            checkRow(rows, i, names[i]);
    }

    private void checkRow(Rows rows, int idx, String name) throws Exception {
        ArrayList<Row> arrayRowUnfold = (ArrayList<Row>) getField(rows, "rowsUnfolded");
        if (idx >= arrayRowUnfold.size()) {
            String msg = "assert  idx is greater that arrayRowUnfold size failed. expected idx: " +
                    idx + " actual: " + arrayRowUnfold.size();
            Log.d("RowsTest", msg);
            printRowArray(arrayRowUnfold);
            throw new Exception(msg);
        }
        Row row = arrayRowUnfold.get(idx);
        if (row.getClass() == RowGroup.class && ! ((RowGroup) row).getName().equals(name)) {
            String msg = "arrayRowUnfold group name check failed. idx: " +
                    idx + " expected name: " + name + " actual name: " + ((RowGroup) row).getName();
            Log.d("RowsTest", msg);
            printRowArray(arrayRowUnfold);
            throw new Exception(msg);
        }
        else if (row.getClass() == RowSong.class && ! ((RowSong) row).getTitle().equals(name)) {
            String msg = "arrayRowUnfold song title check failed. idx: " +
                    idx + " expected name: " + name + " actual name: " + ((RowSong) row).getTitle();
            Log.d("RowsTest", msg);
            printRowArray(arrayRowUnfold);
            throw new Exception(msg);
        }
        else if (row.getClass() != RowSong.class && row.getClass() != RowGroup.class)
            throw new Exception("assert what is this row?.");
    }

    private Object getField(Object o, String fieldName) throws Exception {
        Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(o);
    }

    public void printRowArray(ArrayList<Row> rows) {
        if(rows != null) {
            for (int i = 0; i < rows.size(); i++)
                Log.d("RowsTest", i + " " + rows.get(i).toString());
        }
        else {
            Log.d("RowsTest", "RowArray is null");
        }
    }

    class TestContentProvider extends MockContentProvider {

        private String[][] data;

        public TestContentProvider(Context context, String[][] data) {
            super(context);
            this.data = data;
        }

        @Override
        public Cursor query(Uri uri,
                            String[] projection,
                            String selection,
                            String[] selectionArgs,
                            String sortOrder) {
            String[] columnNames = {MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.Audio.Media.IS_MUSIC
            };
            MatrixCursor matrixCursor = new MatrixCursor(columnNames);
            for(String[] row : data)
                matrixCursor.addRow(row);
            return matrixCursor;
        }
    }

}