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

import java.util.ArrayList;

public class Path {
    public final static char separatorChar = System.getProperty("file.separator", "/").charAt(0);
    public static String rootFolder = "";

    /*
     @param path must not be null
     rootFolder is removed from the beginning of the path
     if rootFolder = "" and path = /mnt/sdcard/toto/tata.mp3 -> return /mnt/sdcard/toto
     if rootFolder = "/mnt/sdcard" and path = /mnt/sdcard/toto/tata.mp3 -> return toto
     if rootFolder = "/mnt/sdcard/" and path = /mnt/sdcard/toto/tata.mp3 -> return toto
    */
    static public String getFolder(String path) {
        String folder;

        // remove rootFolder
        if (rootFolder != null &&
                rootFolder.length() <= path.length() &&
                rootFolder.equals(path.substring(0, rootFolder.length()))) {
            int rootFolderSize = rootFolder.length();
            // remove / remaining at the beginning of path
            if (path.length() > rootFolderSize && path.charAt(rootFolderSize) == separatorChar)
                rootFolderSize++;

            folder = path.substring(rootFolderSize, path.length());
        }
        else {
            folder = path;
        }

        // remove filename
        int index = folder.lastIndexOf(separatorChar);
        if (index == -1) // no folder: remove everything
            index = 0;
        folder = folder.substring(0, index);

        // no folder get the name "."
        if (folder.equals(""))
            folder = ".";

        return folder;
    }

    static public ArrayList<String> cutFolder(String path) {
        ArrayList<String> folders = new ArrayList<>();
        int beg = 0;
        boolean folderFound = false;
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == separatorChar) {
                if (folderFound) {
                    folders.add(path.substring(beg, i));
                    folderFound = false;
                }
                beg = i + 1;
            }
            else {
                folderFound = true;
            }
        }
        // path do not finish by /
        if (folderFound) {
            folders.add(path.substring(beg, path.length()));
        }

        return folders;
    }

}
