package souch.smp;

import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;

public class Song extends SongItem {
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

    public Song(long songID, String songTitle, String songArtist, String songAlbum,
                int dur, int songTrack, String songPath, int padding, String rootFolder) {
        super(padding);
        id = songID;
        title = songTitle;
        artist = songArtist;
        album = songAlbum;
        duration = dur;
        track = songTrack;
        path = songPath;
        if(path != null)
            folder = (new File(path.replaceFirst(rootFolder, ""))).getParent();
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
        text.setTypeface(null, Typeface.NORMAL);
    }

    public String toString() {
        return "ID: " + id + " artist: " + artist + " album: " + album + " title: " + title + " " +
                secondsToMinutes(duration) + " track:" + track + " path: " + path;
    }

    static public String secondsToMinutes(int duration){
        long seconds = duration;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.valueOf(minutes) + (seconds < 10 ? ":0" : ":") + String.valueOf(seconds);
    }
}
