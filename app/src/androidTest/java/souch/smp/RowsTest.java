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

public class RowsTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
        Log.d("RowsTest", "====================================");
    }

    public void testEmpty() throws Exception {
        Log.d("RowsTest", "== testEmpty ==");
        Rows rows = initRows(null);
        checkRowSize(rows, 0);
    }

    public void testNullCursorGetString() throws Exception {
        Log.d("RowsTest", "== testNullCursorGetString ==");
        //String[][] data = new String[][]{{"1", "Salut", null, "head", "80000", "2", null, "1"}};
        String[][] data = new String[][]{{null, null, null, null, null, null, null, "1"}};
        Rows rows = initRows(data);
        checkRowSize(rows, 3);
    }

    public void testOneSong() throws Exception {
        Log.d("RowsTest", "== testOneSong ==");
        String[][] data = new String[][]{{"1", "Salut", "Tortoise", "head", "80000", "2", "/mnt/sdcard/yo", "1"}};
        Rows rows = initRows(data);
        checkRowSize(rows, 3);
    }

    public void test2DifferentSong() throws Exception {
        Log.d("RowsTest", "== test2DifferentSongs ==");
        String[][] data = new String[][]{
                {"1", "Artist1", "Album2", "title", "80000", "1", "/mnt/sdcard/yo", "1"},
                {"2", "Artist2", "album1", "title1", "80000", "2", "/mnt/sdcard/yo", "1"}
        };
        Rows rows = initRows(data);
        checkRowSize(rows, 5);
    }

    public void testMergeNameCase() throws Exception {
        Log.d("RowsTest", "== testMergeNameCase ==");

        // same artist except case
        String[][] data = new String[][]{
                {"1", "Artist1", "Album2", "title", "80000", "1", "/mnt/sdcard/yo", "1"},
                {"2", "artist1", "album2", "title", "80000", "2", "/mnt/sdcard/yo", "1"}};
        Rows rows = initRows(data);
        checkRowSize(rows, 4);
    }

    private Rows initRows(String[][] data) {
        MockContentResolver resolver = new MockContentResolver();
        TestContentProvider provider = new TestContentProvider(getContext(), data);
        resolver.addProvider(MediaStore.AUTHORITY, provider);
        Rows rows = new Rows(resolver, getContext());
        rows.init();
        return rows;
    }

    private void checkRowSize(Rows rows, int size) throws Exception {
        ArrayList<Row> arrayRowUnfold = (ArrayList<Row>) getField(rows, "rowsUnfolded");
        assertTrue(arrayRowUnfold.size() == size);

        ArrayList<Row> arrayRow = (ArrayList<Row>) getField(rows, "rows");
        assertTrue(arrayRow.size() == size);
    }

    private Object getField(Object o, String fieldName) throws Exception {
        Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(o);
    }

    public void printRow(ArrayList<Row> rows) {
        for(int i = 0; i < rows.size() ; i++)
            Log.d("RowsTest", i + " " + rows.get(i).toString());
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