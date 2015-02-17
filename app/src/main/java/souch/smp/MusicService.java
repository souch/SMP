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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.widget.Toast;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener, SensorEventListener
{
    private MediaPlayer player;
    private Rows rows;

    // need for focus
    private boolean wasPlaying;
    // sthg happened and the Main do not know it: a song has finish to play, another app gain focus, ...
    private boolean changed;

    // useful only for buggy android seek
    private int seekPosBug;

    // a notification has been launched
    private boolean foreground;
    private static final int NOTIFY_ID = 1;

    private final IBinder musicBind = new MusicBinder();

    private AudioManager audioManager;

    // current state of the MediaPlayer
    private PlayerState state;

    // set to false if seekTo() has been called but the seek is still not done
    private boolean seekFinished;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate;
    private boolean enableShake;
    private float shakeThreshold;
    private final int MIN_SHAKE_PERIOD = 1000 * 1000 * 1000;
    private double accelLast;
    private double accelCurrent;
    private double accel;

    public Rows getRows() { return rows; }

    public boolean getChanged() {
        boolean hasChanged = changed;
        changed = false;
        return hasChanged;
    }

    public void setChanged() {
        changed = true;
    }


    /*** SERVICE ***/

    public void onCreate() {
        Log.d("MusicService", "onCreate()");
        super.onCreate();

        state = new PlayerState();

        changed = false;
        seekFinished = true;
        seekPosBug = -1;
        wasPlaying = false;

        player = null;
        audioManager = null;

        restore();

        rows = new Rows(getContentResolver(), this.getApplicationContext());
        rows.init();

        if(enableShake) {
            startSensor();
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
        save();
        rows.save();
        releaseAudio();
        stopSensor();
    }



    /*** PLAYER ***/

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
        Log.d("MusicService", "onSeekComplete setProgress" + RowSong.secondsToMinutes(getCurrentPosition()));
    }

    public void playSong() {
        RowSong rowSong = rows.getCurrSong();
        if (rowSong == null)
            return;

        getPlayer().reset();
        state.setState(PlayerState.Idle);

        // get id
        long currSong = rowSong.getID();
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



    /*** PLAY ACTION ***/

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

    // move to this song genuinePos in second
    public void seekTo(int posn){
        if(player == null)
            return;

        seekFinished = false;
        int gotoPos = posn * 1000;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            seekPosBug = gotoPos;

        player.seekTo(gotoPos);
    }

    public boolean getSeekFinished() {
        return seekFinished;
    }

    // unpause
    public void start() {
        getPlayer().start();
        state.setState(PlayerState.Started);
    }

    public void pause() {
        if(player == null)
            return;

        player.pause();
        state.setState(PlayerState.Paused);
    }

    public void playPrev() {
        rows.moveToPrevSong();

        if(foreground)
            startNotification();

        playSong();
    }

    public void playNext() {
        rows.moveToNextSong();

        if(foreground)
            startNotification();

        playSong();
    }



    /*** STATE ***/

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

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }



    /*** NOTIFICATION ***/

    public void startNotification() {
        RowSong rowSong = rows.getCurrSong();
        if(rowSong == null)
            return;

        Intent notificationIntent = new Intent(this, Main.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(R.drawable.ic_launcher,
                rowSong.getTitle(),
                System.currentTimeMillis());
        notification.setLatestEventInfo(this, "SicMu playing", rowSong.getTitle() +
                " - " + rowSong.getArtist(), pendInt);

        startForeground(NOTIFY_ID, notification);
        foreground = true;
    }

    public void stopNotification() {
        if(foreground)
            stopForeground(true);
        foreground = false;
    }



    /*** PREFERENCES ***/

    private void restore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        enableShake = settings.getBoolean(PrefKeys.ENABLE_SHAKE.name(), false);
        shakeThreshold = Float.valueOf(settings.getString(PrefKeys.SHAKE_THRESHOLD.name(),
                getString(R.string.settings_default_shake_threshold))) / 10.0f;
        Log.d("MusicService", "restore enable shake: " + enableShake +
                " threshold:" + shakeThreshold);
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        Log.d("MusicService", "save enableShake: " + enableShake);
        editor.putBoolean(PrefKeys.ENABLE_SHAKE.name(), enableShake);
        editor.commit();
    }



    /*** SENSORS ***/

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

}
