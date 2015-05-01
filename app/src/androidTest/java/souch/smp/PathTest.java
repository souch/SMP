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

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.ArrayList;

public class PathTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
        Log.d("PathTest", "====================================");
    }

    public void testGetFolderEmpty() throws Exception {
        tryGetFolder("", ".");
    }

    public void testGetFolderUsual() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/toto/tata.mp3", "toto");
    }

    public void testGetFolderUsual2() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/toto/titi/tata.mp3", "toto/titi");
    }

    public void testGetFolderUsualSlashRootFolder() throws Exception {
        Path.rootFolder = "/mnt/sdcard/";
        tryGetFolder("/mnt/sdcard/toto/titi/tata.mp3", "toto/titi");
    }

    public void testGetFolderAllRootFolder() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/tata.mp3", ".");

        Path.rootFolder = "/mnt/sdcard/";
        tryGetFolder("/mnt/sdcard/tata.mp3", ".");
    }

    public void testGetFolderMangleRootFolder() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard.mp3", ".");

        Path.rootFolder = "/mnt/sdcard/";
        tryGetFolder("/mnt/sdcard.mp3", "/mnt");
    }

    private void tryGetFolder(String path, String expectedFolder) throws Exception {
        String folder = Path.getFolder(path);
        if (!folder.equals(expectedFolder)) {
            String msg = "Expected '" + expectedFolder + "' got: '" + folder + "'";
            Log.d("PathTest", msg);
            throw new Exception(msg);
        }
    }


    public void testCutFolderUsual() throws Exception {
        tryCutFolder("/mnt/sdcard/toto", new String[]{"mnt", "sdcard", "toto"});
        tryCutFolder("/mnt/sdcard/toto/", new String[]{"mnt", "sdcard", "toto"});
        tryCutFolder("/mnt/sdcard/toto/o", new String[]{"mnt", "sdcard", "toto", "o"});
    }

    public void testCutFolderOne() throws Exception {
        tryCutFolder("/mnt/", new String[]{"mnt"});
        tryCutFolder("/mnt", new String[]{"mnt"});
    }

    public void testCutFolderStrange() throws Exception {
        tryCutFolder("/toot///yo", new String[]{"toot", "yo"});
        tryCutFolder("/toot///", new String[]{"toot"});
        tryCutFolder("//", new String[]{});
        tryCutFolder("/", new String[]{});
        tryCutFolder("", new String[]{});
    }

    private void tryCutFolder(String folder, String[] expectedFolders) throws Exception {
        ArrayList<String> folders = Path.tokenizeFolder(folder);
        if (folders.size() != expectedFolders.length) {
            String msg = "Expected folder length'" + expectedFolders.length + "' got: '" + folders.size() + "'";
            Log.d("PathTest", msg);
            throw new Exception(msg);
        }
        for(int i = 0; i < folders.size(); i++) {
            if (!folders.get(i).equals(expectedFolders[i])) {
                String msg = "Expected folder " + expectedFolders[i] + "' got: '" + folders.get(i) + "'";
                Log.d("PathTest", msg);
                throw new Exception(msg);
            }
        }
    }
}
