package souch.smp;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private int duration;

    public Song(long songID, String songTitle, String songArtist, String songAlbum, int dur) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        album = songAlbum;
        duration = dur;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getAlbum(){return album;}
    public int getDuration(){return duration;}

    static public String secondsToMinutes(int duration){
        long seconds = duration;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.valueOf(minutes) + (seconds < 10 ? ":0" : ":") + String.valueOf(seconds);
    }
}
