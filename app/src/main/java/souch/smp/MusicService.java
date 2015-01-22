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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener {

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    // need for focus
    private boolean wasPlaying;
    // a song has finish to play
    private boolean completed;

    private final IBinder musicBind = new MusicBinder();

    private AudioManager audioManager;

    // not in (idle, end, error) MediaPlayer state
    private boolean initialized;
    // don't use musicSrv.isPlaying() cause it is asynchronous (isPlaying is set to true when
    // .start in onPrepared called)
    private boolean playing;
    // tell if MediaPlayer can be unpaused
    private boolean started;
    // set to false iif seekTo has been called but the seek is still not done
    private boolean seekFinished;

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    public void onCreate() {
        super.onCreate();
        songPosn = 0;

        playing = false;
        started = false;
        completed = false;
        initialized = false;
        seekFinished = true;

        wasPlaying = false;
        player = null;
        audioManager = null;
    }

    // create AudioManager and MediaPlayer at the last moment
    // this func assure they are initialized
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
            player = new MediaPlayer();
            //set player properties
            player.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnSeekCompleteListener(this);
            // commented cause should already be in these states :
            /*
            initialized = false;
            wasPlaying = false;
            playing = false;
            started = false;
            completed = false;
            */
        }
        return player;
    }

    private void releaseAudio() {
        playing = false;
        started = false;
        initialized = false;
        seekFinished = true;
        completed = false;

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
                if (wasPlaying) {
                    getPlayer().start();
                    playing = true;
                }
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
                playing = false;
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
                playing = false;
                break;
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // on My 2.3.6 phone, the phone seems bugged : calling now getCurrentPosition gives
        // last position.
        if(Build.VERSION.SDK_INT <= 10) {
            Timer delaySeekCompleted = new Timer();
            delaySeekCompleted.schedule(new TimerTask() {
                @Override
                public void run() {
                    seekFinished = true;
                }
            }, 1000);
        }
        // on a 4.1 phone no bug : calling getCurrentPosition now gives the new seeked position
        else {
            seekFinished = true;
        }
        Log.d("MusicService", "onSeekComplete setProgress" + Song.secondsToMinutes(getCurrentPosition()));
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

    public void playSong() {
        initialized = false;
        started = false;
        getPlayer().reset();
        playing = true;

        // get song
        Song playSong = songs.get(songPosn);
        songTitle = playSong.getTitle();
        // get id
        long currSong = playSong.getID();
        // set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try{
            getPlayer().setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        initialized = true;
        getPlayer().prepareAsync();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        started = false;
        completed = true;
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // todo: check if this func is ok
        //playing = false;
        initialized = false;
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        started = true;

        Intent notificationIntent = new Intent(this, Main.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(R.drawable.ic_launcher, songTitle, System.currentTimeMillis());
        notification.setLatestEventInfo(this, "SicMu playing", songTitle, pendInt);

        startForeground(NOTIFY_ID, notification);
    }

    // tell if the MediaPlayer has been asked to play
    public boolean getPlaying() {
        return playing;
    }

    // tell if the MediaPlayer can unpause
    public boolean getStarted() {
        return started;
    }

    public boolean getInitialized() {
        return initialized;
    }

    // the MediaPlayer is playing
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean getAndSetCompleted() {
        boolean isCompleted = completed;
        completed = false;
        return isCompleted;
    }

    // get curr position in second
    public int getCurrentPosition(){
        if(player == null)
            return 0;
        return player.getCurrentPosition() / 1000;
    }

    // get song total duration in second
    public int getDuration(){
        if(player == null)
            return 0;
        return player.getDuration() / 1000;
    }

    // move to this song pos in second
    public void seekTo(int posn){
        if(player == null)
            return;

        seekFinished = false;
        player.seekTo(posn * 1000);
    }

    public boolean getSeekFinished() {
        return seekFinished;
    }

    // unpause
    public void go(){
        playing = true;
        getPlayer().start();
    }

    public void pausePlayer(){
        if(player == null)
            return;

        playing = false;
        player.pause();
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
