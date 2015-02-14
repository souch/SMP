package souch.smp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
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

import java.util.Timer;
import java.util.TimerTask;

public class Main extends Activity {
    private Rows rows;
    private ListView songView;
    private RowsAdapter songAdt;
    ImageButton playButton;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;
    // the app is about to close
    private boolean finishing;

    private Timer timer;
    private final long updateInterval = 500;
    private SeekBar seekbar;
    // tell whether the seekbar is currently touch by a user
    private boolean touchSeekbar;
    private TextView duration;
    private TextView currDuration;

    // true if the user want to disable lockscreen
    private boolean noLock;

    // true if you want to keep the current song played visible
    private boolean followSong;


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

        followSong = false;
    }


    // connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Main", "onServiceConnected");

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // get service
            musicSrv = binder.getService();

            rows = musicSrv.getRows();
            songAdt = new RowsAdapter(Main.this, rows, Main.this);
            songView.setAdapter(songAdt);
            songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    if (!serviceBound)
                        return;
                    //Toast.makeText(getApplicationContext(),
                    //        "Click ListItem Number " + position + " id: " + id, Toast.LENGTH_LONG).show();
                    rows.select(position);
                    if(rows.get(position).getClass() == RowSong.class)
                        musicSrv.playSong();
                    updatePlayButton();
                }
            });
            serviceBound = true;

            musicSrv.stopNotification();

            updatePlayButton();
            scrollToCurrSong();
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
                currDuration.setText(RowSong.secondsToMinutes(seekBar.getProgress()));
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
                Log.d("Main", "onStopTrackingTouch setProgress" + RowSong.secondsToMinutes(seekBar.getProgress()));
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

        restore();
        applyLock();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // updateInfo must be run in activity thread
                runOnUiThread(updateInfo);
            }
        }, 10, updateInterval);

        if (serviceBound)
            musicSrv.stopNotification();  // if service not bound stopNotification is called onServiceConnected
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
        save();

        if (!finishing && serviceBound && musicSrv.playingLaunched())
            musicSrv.startNotification();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Main", "onDestroy");

        if (serviceBound) {
            // stop the service if not playing music
            if(!musicSrv.playingLaunched()) {
                musicSrv.stopService(playIntent);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.app_name) + " destroyed.",
                        Toast.LENGTH_SHORT).show();
            }
            unbindService(musicConnection);
            serviceBound = false;
            musicSrv = null;
        }
    }


    final Runnable updateInfo = new Runnable() {
        public void run() {
            if (!serviceBound)
                return;

            //Log.d("Main", "updateInfo");
            if (musicSrv.getChanged()) {
                Log.d("Main", "updateInfo changed");
                updatePlayButton();
                if(followSong)
                    scrollToCurrSong();
            } else {
                if(musicSrv.playingStopped()) {
                    stopPlayButton();
                }
                else if(!touchSeekbar && musicSrv.getSeekFinished()) {
                    Log.v("Main", "updateInfo setProgress" + RowSong.secondsToMinutes(musicSrv.getCurrentPosition()));
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

            RowSong rowSong = rows.getCurrSong();
            if(rowSong != null) {
                duration.setText(RowSong.secondsToMinutes(rowSong.getDuration()));
                duration.setVisibility(TextView.VISIBLE);
                seekbar.setMax(rowSong.getDuration());
                if (!touchSeekbar && musicSrv.getSeekFinished())
                    seekbar.setProgress(musicSrv.getCurrentPosition());
                seekbar.setVisibility(TextView.VISIBLE);
                currDuration.setText(RowSong.secondsToMinutes(musicSrv.getCurrentPosition()));
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(musicSrv != null) {
            MenuItem itemArtist = menu.findItem(R.id.action_sort_artist);
            MenuItem itemFolder = menu.findItem(R.id.action_sort_folder);
            switch(rows.getFilter()) {
                case ARTIST:
                    itemArtist.setIcon(R.drawable.ic_menu_artist_checked);
                    itemFolder.setIcon(R.drawable.ic_menu_folder);
                    break;
                case FOLDER:
                    itemArtist.setIcon(R.drawable.ic_menu_artist);
                    itemFolder.setIcon(R.drawable.ic_menu_folder_checked);
                    break;
            }

            MenuItem ic_menu_shake = menu.findItem(R.id.action_shake);
            if(musicSrv.getEnableShake()) {
                ic_menu_shake.setIcon(R.drawable.ic_menu_shake_checked);
                ic_menu_shake.setTitle(R.string.action_shake_enabled);
            }
            else {
                ic_menu_shake.setIcon(R.drawable.ic_menu_shake);
                ic_menu_shake.setTitle(R.string.action_shake_disabled);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            case R.id.action_shake:
                if(musicSrv != null) {
                    if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
                        musicSrv.setEnableShake(!musicSrv.getEnableShake());
                        int msgId;
                        if(musicSrv.getEnableShake())
                            msgId = R.string.action_shake_enabled;
                        else
                            msgId = R.string.action_shake_disabled;
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(msgId),
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.settings_no_accelerometer),
                                Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            case R.id.action_sort_artist:
                if(musicSrv != null && rows.getFilter() != Filter.ARTIST) {
                    item.setIcon(R.drawable.ic_menu_artist_checked);
                    Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                    rows.setFilter(Filter.ARTIST);
                    songAdt.notifyDataSetChanged();
                    scrollToCurrSong();
                }
                return true;
            case R.id.action_sort_folder:
                if(musicSrv != null && rows.getFilter() != Filter.FOLDER) {
                    item.setIcon(R.drawable.ic_menu_folder_checked);
                    Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                    rows.setFilter(Filter.FOLDER);
                    songAdt.notifyDataSetChanged();
                    scrollToCurrSong();
                }
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
        if(followSong)
            scrollToCurrSong();
    }

    public void playPrev(View view){
        if(!serviceBound)
            return;

        musicSrv.playPrev();
        updatePlayButton();
        if(followSong)
            scrollToCurrSong();
    }


    public void gotoCurrSong(View view) {
        scrollToCurrSong();
    }

    public void scrollToCurrSong() {
        if(rows.size() == 0)
            return;

        final int firstVisible = songView.getFirstVisiblePosition();
        final int lastVisible = songView.getLastVisiblePosition();
        Log.v("Main", "scrollToCurrSong firstVisible:" + firstVisible + " lastVisible:" + lastVisible);

        final int smoothMaxOffset = 40; // deactivate smooth if too far
        int showAround = 2; // to show a bit of songItems before or after the cur song
        /*
        // for tiny screen that can show less than 5 songItems
        final int totalVisible = lastVisible - firstVisible;
        showAround = showAround > totalVisible/2 ? totalVisible/2 : showAround;
        showAround = showAround < 0 ? 0 : showAround;
        Log.v("Main", "scrollToCurrSong showAround:" + showAround);
        */

        int gotoSong = rows.getCurrPos();

        // how far from top or bottom border the song is
        int offset = 0;
        if(gotoSong > lastVisible)
            offset = gotoSong - lastVisible;
        if(gotoSong < firstVisible)
            offset = firstVisible - gotoSong;

        if(offset > smoothMaxOffset) {
            gotoSong -= showAround;
            if(gotoSong < 0)
                gotoSong = 0;
            // setSelection set position at top of the screen
            songView.setSelection(gotoSong);
        }
        else {
            if(gotoSong + showAround >= lastVisible) {
                gotoSong += showAround;
                if(gotoSong >= rows.size())
                    gotoSong = rows.size() - 1;
            }
            else {
                gotoSong -= showAround;
                if(gotoSong < 0)
                    gotoSong = 0;
            }
            // smoothScrollToPosition only make the position visible
            songView.smoothScrollToPosition(gotoSong);
        }

        Log.d("Main", "scrollToCurrSong position:" + gotoSong);
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


    public MusicService getMusicSrv() {
        return musicSrv;
    }

/*
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d("Main", "Exit app");
                finishing = true;
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
*/

    private void restore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        noLock = settings.getBoolean(PrefKeys.NO_LOCK.name(), false);
        followSong = settings.getBoolean(PrefKeys.FOLLOW_SONG.name(), true);

        Log.d("MusicService", "restorePreferences noLock: " + noLock + " follow: " + followSong);
    }

    private void save() {
        Log.d("MusicService", "save noLock: " + noLock);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PrefKeys.NO_LOCK.name(), noLock);
        editor.commit();
    }
}

