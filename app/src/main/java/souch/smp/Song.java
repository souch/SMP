package souch.smp;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long duration;

    public Song(long songID, String songTitle, String songArtist, String songAlbum, long dur) {
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
    public long getDuration(){return duration;}
}
