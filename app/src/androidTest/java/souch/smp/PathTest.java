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

public class PathTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
        Log.d("PathTest", "====================================");
    }

    public void testEmpty() throws Exception {
        tryGetFolder("", ".");
    }

    public void testUsual() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/toto/tata.mp3", "toto");
    }

    public void testUsual2() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/toto/titi/tata.mp3", "toto/titi");
    }

    public void testUsualSlashRootFolder() throws Exception {
        Path.rootFolder = "/mnt/sdcard/";
        tryGetFolder("/mnt/sdcard/toto/titi/tata.mp3", "toto/titi");
    }

    public void testAllRootFolder() throws Exception {
        Path.rootFolder = "/mnt/sdcard";
        tryGetFolder("/mnt/sdcard/tata.mp3", ".");

        Path.rootFolder = "/mnt/sdcard/";
        tryGetFolder("/mnt/sdcard/tata.mp3", ".");
    }

    public void testMangleRootFolder() throws Exception {
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
}
