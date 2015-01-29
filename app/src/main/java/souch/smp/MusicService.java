package souch.smp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener, SensorEventListener
{

    // save/load song pos preference name
    final String currSongPref = "currSong";

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    // need for focus
    private boolean wasPlaying;
    // sthg happened and the Main do not know it : a song has finish to play, another app gain focus
    private boolean changed;

    // a notification has been lauched
    private boolean foreground;

    private final IBinder musicBind = new MusicBinder();

    private AudioManager audioManager;

    // current state of the MediaPlayer
    private PlayerState state;

    // set to false if seekTo() has been called but the seek is still not done
    private boolean seekFinished;

    private static final int NOTIFY_ID = 1;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate;
    private boolean enableShake;
    private float shakeThreshold;
    private final int MIN_SHAKE_PERIOD = 600;


    public void onCreate() {
        Log.d("MusicService", "onCreate()");
        super.onCreate();

        state = new PlayerState();

        changed = false;
        seekFinished = true;
        wasPlaying = false;

        player = null;
        audioManager = null;

        songs = new ArrayList<Song>();
        initSongs();
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });

        restorePreferences();

        if(enableShake) {
            startSensor();
        }
    }


    public void initSongs() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor;
        try {
            musicCursor = musicResolver.query(musicUri, null, null, null, null);
        } catch (Exception e) {
            final String msg = "initSongs(): Unable to query musicResolver! No songs available.";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            Log.e("MusicService", msg);
            return;
        }

        if(musicCursor != null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                int thisDuration = musicCursor.getInt(durationColumn);
                songs.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, thisDuration / 1000));
            }
            while (musicCursor.moveToNext());
        }
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
        }
        return player;
    }

    private void releaseAudio() {
        state.setState(PlayerState.Nope);
        seekFinished = true;
        changed = true;
        wasPlaying = false;

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

        stopNotification();
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (wasPlaying) {
                    getPlayer().start();
                    state.setState(PlayerState.Started);
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
                    state.setState(PlayerState.Paused);
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
                    state.setState(PlayerState.Paused);
                    wasPlaying = true;
                }
                else {
                    wasPlaying = false;
                }
                break;
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // on My 2.3.6 phone, the phone seems bugged : calling now getCurrentPosition gives
        // last position. 1 second after it is ok, so wait a bit.
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float accelationSquareRoot = (x*x + y*y + z*z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        if (accelationSquareRoot > shakeThreshold) {
            final long actualTime = event.timestamp;
            if (actualTime - lastUpdate < MIN_SHAKE_PERIOD) {
                return;
            }
            lastUpdate = actualTime;

            Log.d("MusicService", "Device was shuffed. Acceleration: " +
                    String.format("%.1f", accelationSquareRoot) +
                    " x: " + String.format("%.1f", x*x) +
                    " y: " + String.format("%.1f", y*y) +
                    " z: " + String.format("%.1f", z*z));

            // goes to next song
            if(playingLaunched()) {
                playNext();
                changed = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


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
        savePreferences();
        releaseAudio();
        stopSensor();
    }

    private void restorePreferences() {
        SharedPreferences settings = getSharedPreferences("MusicService", 0);
        int savedSong = settings.getInt(currSongPref, 0);
        // the songs must have changed
        if (savedSong >= songs.size())
            savedSong = 0;
        songPosn = savedSong;
        Log.d("MusicService", "restorePreferences load song: " + savedSong);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        enableShake = sp.getBoolean(PrefKeys.ENABLE_SHAKE, true);
        shakeThreshold = Float.valueOf(sp.getString(PrefKeys.SHAKE_THRESHOLD, "30")) / 10.0f;
        Log.d("MusicService", "restorePreferences enable shake: " + enableShake +
                " threshold:" + shakeThreshold);
    }

    private void savePreferences() {
        Log.d("MusicService", "savePreferences save song: " + songPosn);

        SharedPreferences settings = getSharedPreferences("MusicService", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(currSongPref, songPosn);
        editor.commit();
    }

    public void setSong(int songIndex){
        songPosn = songIndex;
    }
    public int getSong() {
        return songPosn;
    }
    public ArrayList<Song> getSongs() {
        return songs;
    }

    public void playSong() {
        getPlayer().reset();
        state.setState(PlayerState.Idle);

        // get id
        long currSong = songs.get(songPosn).getID();
        // set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try{
            getPlayer().setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
            state.setState(PlayerState.Error);
            // todo: improve error handling
            return;
        }
        state.setState(PlayerState.Initialized);
        getPlayer().prepareAsync();
        state.setState(PlayerState.Preparing);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        state.setState(PlayerState.PlaybackCompleted);
        changed = true;
        playNext();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // todo: check if this func is ok
        /*
        mp.reset();
        state.setState(PlayerState.Idle);
        */
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // start playback
        mp.start();
        state.setState(PlayerState.Started);
    }

    public void startNotification() {
        Intent notificationIntent = new Intent(this, Main.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Song playSong = songs.get(songPosn);
        Notification notification = new Notification(R.drawable.ic_launcher,
                playSong.getTitle(),
                System.currentTimeMillis());
        notification.setLatestEventInfo(this, "SicMu playing", playSong.getTitle() +
                " - " + playSong.getArtist() +
                " - " + playSong.getAlbum(), pendInt);

        startForeground(NOTIFY_ID, notification);
        foreground = true;
    }

    public void stopNotification() {
        if(foreground)
            stopForeground(true);
        foreground = false;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean getChanged() {
        boolean hasChanged = changed;
        changed = false;
        return hasChanged;
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
    public void start(){
        getPlayer().start();
        state.setState(PlayerState.Started);
    }

    public void pause(){
        if(player == null)
            return;

        player.pause();
        state.setState(PlayerState.Paused);
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
        if(foreground)
            startNotification();
        playSong();
    }


    public boolean isInState(int states) {
        return state.compare(states);
    }

    // !playingStopped == playingLaunched || playingPaused

    public boolean playingLaunched() {
        final int states = PlayerState.Initialized |
                PlayerState.Idle |
                PlayerState.PlaybackCompleted |
                PlayerState.Prepared |
                PlayerState.Preparing |
                PlayerState.Started;
        return state.compare(states);
    }

    public boolean playingStopped() {
        final int states = PlayerState.Nope |
              PlayerState.Error |
              PlayerState.End;
        return state.compare(states);
    }

    public boolean playingPaused() {
        return state.compare(PlayerState.Paused);
    }

    public void setEnableShake(boolean shake) {
        enableShake = shake;
        if(enableShake)
            startSensor();
        else
            stopSensor();
    }

    public void setShakeThreshold(float threshold) {
        shakeThreshold = threshold;
    }

    // can be called twice
    private void startSensor() {
        if(sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            lastUpdate = System.currentTimeMillis();
        }
    }

    // can be called twice
    private void stopSensor() {
        if(sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
            accelerometer = null;
        }
    }
}
