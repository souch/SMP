package souch.smp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Activity {
    private ArrayList<Song> songs;
    private ListView songView;
    private SongAdapter songAdt;
    ImageButton playButton;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;
    // the app is about to close
    private boolean finishing;

    private Timer timer;
    private final long updateInterval = 1000;
    private SeekBar seekbar;
    // tell whether the seekbar is currently touch by a user
    private boolean touchSeekbar;
    private TextView duration;
    private TextView currDuration;

    // true if the user want to disable lockscreen
    private boolean noLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Main", "onCreate");
        setContentView(R.layout.activity_main);
        finishing = false;

        songView = (ListView) findViewById(R.id.song_list);

        playButton = (ImageButton) findViewById(R.id.play_button);
        // useful only for testing
        playButton.setTag(R.drawable.ic_action_play);

        playIntent = new Intent(this, MusicService.class);
        startService(playIntent);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        duration = (TextView) findViewById(R.id.duration);
        currDuration = (TextView) findViewById(R.id.curr_duration);
        touchSeekbar = false;
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        restorePreferences();
        applyLock();
    }


    // connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Main", "onServiceConnected");

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // get service
            musicSrv = binder.getService();

            songs = musicSrv.getSongs();
            songAdt = new SongAdapter(Main.this, songs, Main.this);
            songView.setAdapter(songAdt);
            songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    if (!serviceBound)
                        return;
                    //Toast.makeText(getApplicationContext(),
                    //        "Click ListItem Number " + position + " id: " + id, Toast.LENGTH_LONG).show();
                    musicSrv.setSong(position);
                    musicSrv.playSong();
                    updatePlayButton();
                }
            });
            serviceBound = true;

            updatePlayButton();
            scrollToCurrSong(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Main", "onServiceDisconnected");
            serviceBound = false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekbar.getVisibility() == TextView.VISIBLE) {
                currDuration.setText(Song.secondsToMinutes(seekBar.getProgress()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            touchSeekbar = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final int states = PlayerState.Prepared |
                    PlayerState.Started |
                    PlayerState.Paused |
                    PlayerState.PlaybackCompleted;
            if (serviceBound && musicSrv.isInState(states)) {
                Log.d("Main", "onStopTrackingTouch setProgress" + Song.secondsToMinutes(seekBar.getProgress()));
                seekBar.setProgress(seekBar.getProgress());
                // valid state : {Prepared, Started, Paused, PlaybackCompleted}
                musicSrv.seekTo(seekBar.getProgress());
            }

            touchSeekbar = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Main", "onStart");

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // updateInfo must be run in activity thread
                runOnUiThread(updateInfo);
            }
        }, 10, updateInterval);

        if (serviceBound)
            musicSrv.stopNotification();
        // todo: if serviceBound = false -> stopNotification should be called from onServiceConnected?
    }

    /*
    @Override
    protected void onResume(){
        super.onResume();
        Log.d("Main", "onResume");
    }


    @Override
    protected void onPause(){
        super.onPause();
        Log.d("Main", "onPause");
    }
*/

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Main", "onStop");
        timer.cancel();

        if (!finishing && serviceBound && musicSrv.playingLaunched())
            musicSrv.startNotification();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Main", "onDestroy");
        savePreferences();

        if (serviceBound) {
            unbindService(musicConnection);
            serviceBound = false;
            musicSrv = null;
        }

    }

    final Runnable updateInfo = new Runnable() {
        public void run() {
            if (!serviceBound || musicSrv.playingStopped()) {
                stopPlayButton();
                return;
            }

            if (musicSrv.getChanged()) {
                Log.d("Main", "updateInfo");
                updatePlayButton();
            } else {
                if (!musicSrv.playingStopped() && !touchSeekbar && musicSrv.getSeekFinished()) {
                    Log.d("Main", "updateInfo setProgress" + Song.secondsToMinutes(musicSrv.getCurrentPosition()));
                    // getCurrentPosition {Idle, Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted}
                    seekbar.setProgress(musicSrv.getCurrentPosition());
                }
            }
        }
    };


    private void updatePlayButton() {
        if (!serviceBound || musicSrv.playingStopped()) {
            // MediaPlayer has been destroyed or first start
            stopPlayButton();
        } else {
            if (!musicSrv.playingPaused()) {
                playButton.setImageResource(R.drawable.ic_action_pause);
                playButton.setTag(R.drawable.ic_action_pause);
            } else {
                playButton.setImageResource(R.drawable.ic_action_play);
                playButton.setTag(R.drawable.ic_action_play);
            }

            Song currSong = songs.get(musicSrv.getSong());
            duration.setText(Song.secondsToMinutes(currSong.getDuration()));
            duration.setVisibility(TextView.VISIBLE);
            seekbar.setMax(currSong.getDuration());
            if (!touchSeekbar && musicSrv.getSeekFinished())
                seekbar.setProgress(musicSrv.getCurrentPosition());
            seekbar.setVisibility(TextView.VISIBLE);
            currDuration.setText(Song.secondsToMinutes(musicSrv.getCurrentPosition()));
        }

        songAdt.notifyDataSetChanged();
    }


    private void stopPlayButton() {
        duration.setVisibility(TextView.INVISIBLE);
        seekbar.setVisibility(TextView.INVISIBLE);
        currDuration.setText(R.string.app_name);
        playButton.setImageResource(R.drawable.ic_action_play);
        playButton.setTag(R.drawable.ic_action_play);
    }


    public void settings(View view) {
        openOptionsMenu();
    }

    public void rescan() {
        //Broadcast the Media Scanner Intent to trigger it
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                .parse("file://" + Environment.getExternalStorageDirectory())));

        //Just a message
        Toast toast = Toast.makeText(getApplicationContext(),
                "Media Scanner Triggered...", Toast.LENGTH_SHORT);
        toast.show();

        // todo: should be improved with this
        // http://stackoverflow.com/questions/13270789/how-to-run-media-scanner-in-android
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //Intent intent = new Intent(this, Settings.class);
        //startActivity(intent);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_rescan) {
            rescan();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void playOrPause(View view) {
        if(!serviceBound)
            return;

        if (musicSrv.isInState(PlayerState.Started)) {
            // valid state {Started, Paused, PlaybackCompleted}
            // if the player is between idle and prepared state, it will not be paused!
            musicSrv.pause();
        }
        else {
            if (musicSrv.isInState(PlayerState.Paused)) {
                // previously paused. Valid state {Prepared, Started, Paused, PlaybackCompleted}
                musicSrv.start();
            }
            else {
                musicSrv.playSong();
            }
        }

        updatePlayButton();
    }

    public void playNext(View view){
        if(!serviceBound)
            return;

        musicSrv.playNext();
        updatePlayButton();
    }

    public void playPrev(View view){
        if(!serviceBound)
            return;

        musicSrv.playPrev();
        updatePlayButton();
    }

    public void scrollToCurrSong(View view) {
        if(!serviceBound)
            return;

        int gotoSong = musicSrv.getSong() - 3; // -3 to show a bit of songs before the cur song
        if(gotoSong < 0)
            gotoSong = 0;

        Log.d("Main", "scrollToCurrSong:" + gotoSong);
        songView.setSelection(gotoSong);
        //songView.smoothScrollToPosition(gotoSong);
    }

    public void lockUnlock(View view) {
        noLock = !noLock;
        applyLock();
    }

    public void applyLock() {
        ImageButton lockButton = (ImageButton) findViewById(R.id.lock_button);
        if(noLock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            lockButton.setImageResource(R.drawable.ic_action_unlocked);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            lockButton.setImageResource(R.drawable.ic_action_locked);
        }
    }

    public int getSong() {
        if(serviceBound)
            return musicSrv.getSong();
        else
            return -1;
    }

    public boolean isInState(int states) {
        return serviceBound && musicSrv.isInState(states);
    }


    // exit "nicely"
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d("Main", "Exit app");
                finishing = true;
                if(serviceBound) {
                    musicSrv.stopService(playIntent);
                }
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void restorePreferences() {
        SharedPreferences settings = getSharedPreferences("Main", 0);
        noLock = settings.getBoolean("noLock", false);
        Log.d("MusicService", "restorePreferences noLock: " + noLock);
    }

    private void savePreferences() {
        Log.d("MusicService", "savePreferences noLock: " + noLock);

        SharedPreferences settings = getSharedPreferences("Main", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("noLock", noLock);
        editor.commit();
    }
}

