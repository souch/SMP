package souch.smp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener, SensorEventListener
{
    private MediaPlayer player;
    private ArrayList<SongItem> songItems;
    // current position in songItems
    private int songPos;
    // id of the song at last exiting
    private long savedID;
    // need for focus
    private boolean wasPlaying;
    // sthg happened and the Main do not know it: a song has finish to play, another app gain focus, ...
    private boolean changed;

    // useful only for buggy android seek
    private int seekPosBug;

    private Filter filter;

    // a notification has been launched
    private boolean foreground;
    private static final int NOTIFY_ID = 1;

    private final IBinder musicBind = new MusicBinder();

    private AudioManager audioManager;

    // current state of the MediaPlayer
    private PlayerState state;

    // set to false if seekTo() has been called but the seek is still not done
    private boolean seekFinished;

    private String rootFolder;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate;
    private boolean enableShake;
    private float shakeThreshold;
    private final int MIN_SHAKE_PERIOD = 1000 * 1000 * 1000;
    private double accelLast;
    private double accelCurrent;
    private double accel;

    public void onCreate() {
        Log.d("MusicService", "onCreate()");
        super.onCreate();

        state = new PlayerState();

        changed = false;
        seekFinished = true;
        seekPosBug = -1;
        wasPlaying = false;

        filter = Filter.FOLDER;

        player = null;
        audioManager = null;

        songPos = -1;
        restorePreferences();

        songItems = new ArrayList<SongItem>();
        initSongs();


        if(enableShake) {
            startSensor();
        }
    }


    // todo: put all the init songs in a different class?

    // preference must have already been restored
    public void initSongs() {
        long startTime = System.currentTimeMillis();
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor;
        String[] projection = new String[] {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.MediaColumns.DATA,
            MediaStore.Audio.Media.TRACK
        };
        String where = MediaStore.Audio.Media.IS_MUSIC + "=1";

        String sortOrder = null;
        switch(filter) {
            case ARTIST:
                sortOrder = MediaStore.Audio.Media.ARTIST +
                        ", " + MediaStore.Audio.Media.ALBUM +
                        ", " + MediaStore.Audio.Media.TRACK +
                        ", " + MediaStore.Audio.Media.TITLE;
                break;
            case FOLDER:
                // did not find a way to sort by folder through query
                break;
        }
        try {
            musicCursor = musicResolver.query(musicUri, projection, where, null, sortOrder);
        } catch (Exception e) {
            final String msg = "No songItems found!";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            Log.e("MusicService", msg);
            return;
        }

        switch(filter) {
            case ARTIST:
                initSongsByArtist(musicCursor);
                break;
            case FOLDER:
                initSongsByPath(musicCursor);
                break;
        }

        if(musicCursor != null)
            musicCursor.close();


        if(songPos == -1) {
            // no songPos saved : search the first song
            int idx;
            for(idx = 0; idx < songItems.size(); idx++) {
                if (songItems.get(idx).getClass() == Song.class) {
                    songPos = idx;
                    break;
                }
            }
        }

        Log.d("MusicService", "songItems initialized in " + (System.currentTimeMillis() - startTime) + "ms");
        Log.d("MusicService", "songPos: " + songPos);
    }

    public void initSongsByArtist(Cursor musicCursor) {
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            SongGroup prevArtistGroup = null;
            SongGroup prevAlbumGroup = null;
            int lastSongPos = -1;
            do {
                long id = musicCursor.getLong(idCol);
                String title = musicCursor.getString(titleCol);
                String artist = musicCursor.getString(artistCol);
                String album = musicCursor.getString(albumCol);
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);

                if (prevArtistGroup == null || artist.compareToIgnoreCase(prevArtistGroup.getName()) != 0) {
                    SongGroup artistGroup = new SongGroup(artist, Typeface.BOLD, getSongPadding(0));
                    songItems.add(artistGroup);
                    if (prevArtistGroup != null)
                        prevArtistGroup.setEndPos(lastSongPos);
                    prevArtistGroup = artistGroup;
                }

                if (prevAlbumGroup == null || album.compareToIgnoreCase(prevAlbumGroup.getName()) != 0) {
                    SongGroup albumGroup = new SongGroup(album, Typeface.ITALIC, getSongPadding(1));
                    songItems.add(albumGroup);
                    if (prevAlbumGroup != null)
                        prevAlbumGroup.setEndPos(lastSongPos);
                    prevAlbumGroup = albumGroup;
                }

                Song song = new Song(id, title, artist, album, duration / 1000, track, null,
                        getSongPadding(2), rootFolder);

                lastSongPos = songItems.size();
                if(id == savedID)
                    songPos = lastSongPos;
                songItems.add(song);
                Log.d("MusicService", "song added: " + song.toString());
            }
            while (musicCursor.moveToNext());
        }
    }


    public void initSongsByPath(Cursor musicCursor) {
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idCol = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int pathCol = musicCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int trackCol = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            do {
                long id = musicCursor.getLong(idCol);
                String title = musicCursor.getString(titleCol);
                String artist = musicCursor.getString(artistCol);
                String album = musicCursor.getString(albumCol);
                int duration = musicCursor.getInt(durationCol);
                int track = musicCursor.getInt(trackCol);
                String path = musicCursor.getString(pathCol);

                Song song = new Song(id, title, artist, album, duration / 1000, track, path,
                        getSongPadding(2), rootFolder);
                songItems.add(song);
                Log.d("MusicService", "song added: " + song.toString());
            }
            while (musicCursor.moveToNext());
        }

        // sort
        Collections.sort(songItems, new Comparator<SongItem>() {
            public int compare(SongItem first, SongItem second) {
                // only Song has been added so far
                Song a = (Song) first;
                Song b = (Song) second;
                int cmp = a.getFolder().compareToIgnoreCase(b.getFolder());
                if(cmp == 0) {
                    cmp = a.getArtist().compareToIgnoreCase(b.getArtist());
                    if(cmp == 0) {
                        cmp = a.getAlbum().compareToIgnoreCase(b.getAlbum());
                        if(cmp == 0) {
                            cmp = a.getTrack() - b.getTrack();
                        }
                    }
                }
                return cmp;
            }
        });

        // add group
        String prevInit = "!=_"; // Hope Nothing has this name
        String prevFolder = prevInit;
        String prevArtist = prevInit;
        int idx;
        for(idx = 0; idx < songItems.size(); idx++) {
            Song song = (Song) songItems.get(idx);
            String curFolder = song.getFolder();
            if(curFolder.compareToIgnoreCase(prevFolder) != 0) {
                songItems.add(idx, new SongGroup(curFolder, Typeface.BOLD_ITALIC, getSongPadding(0)));
                idx++;
                prevFolder = curFolder;
                prevArtist = prevInit;
            }
            String curArtist = song.getArtist();
            if(curArtist.compareToIgnoreCase(prevArtist) != 0) {
                songItems.add(idx, new SongGroup(curArtist, Typeface.BOLD, getSongPadding(1)));
                idx++;
                prevArtist = curArtist;
            }
            if(song.getID() == savedID)
                 songPos = idx;
        }
    }


    // create AudioManager and MediaPlayer at the last moment
    // this func assure they are initialized
    private MediaPlayer getPlayer() {
        seekPosBug = -1;

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
        // on a 4.1 phone no bug : calling getCurrentPosition now gives the new seeked position
        // on My 2.3.6 phone, the phone seems bugged : calling now getCurrentPosition gives
        // last position. So wait the seekpos goes after the asked seekpos.
        if(seekPosBug != -1) {
            int i = 15;
            while(i-- > 0 && getCurrentPosition() < seekPosBug) {
                SystemClock.sleep(300);
            }
            seekPosBug = -1;
        }

        seekFinished = true;
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

        // algo found here : http://stackoverflow.com/questions/2317428/android-i-want-to-shake-it
        accelLast = accelCurrent;
        accelCurrent = Math.sqrt((double) (x*x + y*y + z*z));
        double delta = accelCurrent - accelLast;
        accel = accel * 0.9f + delta; // perform low-cut filter

        if (accel > shakeThreshold) {
            final long actualTime = event.timestamp;
            if (actualTime - lastUpdate < MIN_SHAKE_PERIOD) {
                return;
            }
            lastUpdate = actualTime;

            Log.d("MusicService", "Device was shuffed. Acceleration: " +
                    String.format("%.1f", accel) +
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        savedID = settings.getLong(PrefKeys.CURR_SONG.name(), -1);
        Log.d("MusicService", "restorePreferences savedId: " + savedID);

        enableShake = settings.getBoolean(PrefKeys.ENABLE_SHAKE.name(), false);
        shakeThreshold = Float.valueOf(settings.getString(PrefKeys.SHAKE_THRESHOLD.name(),
                getString(R.string.settings_default_shake_threshold))) / 10.0f;
        Log.d("MusicService", "restorePreferences enable shake: " + enableShake +
                " threshold:" + shakeThreshold);

        rootFolder = settings.getString(PrefKeys.ROOT_FOLDER.name(), Settings.getDefaultMusicDir());

        filter = Filter.valueOf(settings.getString(PrefKeys.FILTER.name(), Filter.FOLDER.name()));
    }

    private void savePreferences() {
        Log.d("MusicService", "savePreferences save song: " + songPos);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        updateSavedId();
        editor.putLong(PrefKeys.CURR_SONG.name(), savedID);
        editor.putString(PrefKeys.FILTER.name(), filter.name());
        editor.putBoolean(PrefKeys.ENABLE_SHAKE.name(), enableShake);
        editor.commit();
    }

    public void setSongPos(int songIndex){
        songPos = songIndex;
    }

    public int getSongPos() {
        return songPos;
    }

    public ArrayList<SongItem> getSongItems() {
        return songItems;
    }

    public void playSong() {
        if(songItems.size() == 0)
            return;

        SongItem songItem = songItems.get(songPos);
        Song song;
        if(songItem.getClass() == Song.class) {
            song = (Song) songItem;
        }
        else {
            Log.w("MusicService", "playSong try to start playing with a wrong SongItem!");
            return;
        }

        getPlayer().reset();
        state.setState(PlayerState.Idle);

        // get id
        long currSong = song.getID();
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
        SongItem songItem = songItems.get(songPos);
        if(songItem.getClass() == Song.class) {
            Song song = (Song) songItem;
            Intent notificationIntent = new Intent(this, Main.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification(R.drawable.ic_launcher,
                    song.getTitle(),
                    System.currentTimeMillis());
            notification.setLatestEventInfo(this, "SicMu playing", song.getTitle() +
                    " - " + song.getArtist(), pendInt);

            startForeground(NOTIFY_ID, notification);
            foreground = true;
        }
        else {
            Log.w("MusicService", "startNotification try to start a notification with a wrong SongItem!");
        }
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
        int gotoPos = posn * 1000;

        if(Build.VERSION.SDK_INT <= 10)
            seekPosBug = gotoPos;

        player.seekTo(gotoPos);
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
        songPos--;
        if(songPos < 0)
            songPos = songItems.size() - 1;

        while(songItems.get(songPos).getClass() != Song.class) {
            songPos--;
            if(songPos < 0)
                songPos = songItems.size() - 1;
        }

        playSong();
    }

    public void playNext(){
        songPos++;
        if(songPos >= songItems.size())
            songPos = 0;

        while(songItems.get(songPos).getClass() != Song.class)
            songPos++;

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

    public boolean getEnableShake() { return enableShake; }

    public void setShakeThreshold(float threshold) {
        shakeThreshold = threshold;
    }

    // can be called twice
    private void startSensor() {
        if(sensorManager == null) {
            accelLast = SensorManager.GRAVITY_EARTH;
            accel = 0.00f;
            accelCurrent = SensorManager.GRAVITY_EARTH;
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

    public Filter getFilter() {return filter;}
    public void setFilter(Filter theFilter) {
        if(filter != theFilter) {
            filter = theFilter;
            reinitSongs();
        }
    }

    public void reinitSongs() {
        // todo: handle the current playing song finish during reinitSongs()...
        updateSavedId();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        rootFolder = settings.getString(PrefKeys.ROOT_FOLDER.name(), Settings.getDefaultMusicDir());

        songItems.clear();
        initSongs();

        changed = true;
    }

    // cache result
    private static int lastPx1 = -1;
    private static int lastPx2 = -1;
    private int getSongPadding(int shift) {
        int px;
        switch(shift) {
            case 1:
                if(lastPx1 < 0)
                    lastPx1 = convertDpToPixels(10);
                px = lastPx1;
                break;
            case 2:
                if(lastPx2 < 0)
                    lastPx2 = convertDpToPixels(20);
                px = lastPx2;
                break;
            default:
                px = 0;
        }
        return px;
    }

    private int convertDpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    private void updateSavedId() {
        SongItem item = songItems.get(songPos);
        if(item.getClass() == Song.class)
            savedID = ((Song) item).getID();
    }
}
