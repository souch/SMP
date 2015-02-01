package souch.smp;

public class Song extends SongItem {
    private long id;
    private String title;
    private String artist;
    private String album;
    private int duration;
    private int track;
    private String path;

    public Song(long songID, String songTitle, String songArtist, String songAlbum, int dur, int songTrack, String songPath, int padding) {
        super(padding);
        id = songID;
        title = songTitle;
        artist = songArtist;
        album = songAlbum;
        duration = dur;
        track = songTrack;
        path = songPath;
    }

    public long getID(){return id;}
    public String getTitle(){return getStrPadding() + title;}
    public String getArtist(){return artist;}
    public String getAlbum(){return album;}
    public int getDuration(){return duration;}
    public int getTrack(){return track;}
    public String getPath(){return path;}


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
