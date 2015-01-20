package souch.smp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    // need for focus
    private boolean wasPlaying;

    private final IBinder musicBind = new MusicBinder();

    private AudioManager audioManager;


    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    public void onCreate() {
        //create the service
        super.onCreate();
        //initialize position
        songPosn = 0;

        wasPlaying = false;
        player = null;
        audioManager = null;
    }

    // create audiomanager and Mediaplayer at the last moment
    // assure they are initialized
    private MediaPlayer getPlayer() {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Toast.makeText(getApplicationContext(), "Could not get audio focus! Restart the app!",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (player == null) {
            //create player
            initMusicPlayer();
        }
        return player;
    }

    private void releaseAudio() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
            player = null;
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
            audioManager = null;
        }
        wasPlaying = false;
        stopForeground(true);
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (wasPlaying)
                    getPlayer().start();
                //player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                releaseAudio();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (getPlayer().isPlaying()) {
                    getPlayer().pause();
                    wasPlaying = true;
                }
                else {
                    wasPlaying = false;
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (getPlayer().isPlaying()) {
                    //player.setVolume(0.1f, 0.1f);
                    getPlayer().pause();
                    wasPlaying = true;
                }
                else {
                    wasPlaying = false;
                }

                break;
        }
    }


    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return musicBind;
    }

    @Override
    public void onDestroy() {
        Log.d("MusicService", "onDestroy");
        releaseAudio();
    }

    public void setList(ArrayList<Song> theSongs){
        songs = theSongs;
    }

    public void setSong(int songIndex){
        songPosn = songIndex;
    }
    public int getSong() {
        return songPosn;
    }

    public void initMusicPlayer(){
        player = new MediaPlayer();
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        wasPlaying = false;
    }

    public void playSong() {
        getPlayer().reset();

        //get song
        Song playSong = songs.get(songPosn);
        songTitle = playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try{
            getPlayer().setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        getPlayer().prepareAsync();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Intent notificationIntent = new Intent(this, Main.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(R.drawable.ic_launcher, songTitle, System.currentTimeMillis());
        notification.setLatestEventInfo(this, "SicMu playing", songTitle, pendInt);

        startForeground(NOTIFY_ID, notification);
    }

    public int getCurrentPosition(){
        if(player == null)
            return 0;
        return player.getCurrentPosition();
    }

    public int getDuration(){
        if(player == null)
            return 0;
        return player.getDuration();
    }

    public boolean isPlaying(){
        if(player == null)
            return false;
        return player.isPlaying();
    }

    public void pausePlayer(){
        if(player == null)
            return;

        player.pause();
    }

    public void seekTo(int posn){
        if(player == null)
            return;

        player.seekTo(posn);
    }

    public void go(){
        getPlayer().start();
    }

    public void playPrev(){
        songPosn--;
        if(songPosn < 0)
            songPosn = songs.size() - 1;
        playSong();
    }

    public void playNext(){
        songPosn++;
        if(songPosn >= songs.size())
            songPosn = 0;
        playSong();
    }
}
