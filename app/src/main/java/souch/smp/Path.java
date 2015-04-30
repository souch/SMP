package souch.smp;

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


}
