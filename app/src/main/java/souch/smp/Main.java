package souch.smp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Activity {
    private ArrayList<Song> songList;
    private ListView songView;
    private SongAdapter songAdt;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;

    private Timer timer;
    private final long updateInterval = 1000;
    private SeekBar seekbar;
    // tell whether the seekbar is currently touch by a user
    private boolean touchSeekbar;
    private TextView duration;
    private TextView currDuration;


    // save/load song pos preference name
    final String currSongPref = "currSong";

    // connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Main", "onServiceConnected");

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // get service
            musicSrv = binder.getService();
            serviceBound = true;
            // pass list
            musicSrv.setList(songList);
            restorePreferences();
            songAdt.notifyDataSetChanged();
            gotoCurrSong(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Main", "onServiceDisconnected");
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Main", "onCreate");
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);

        // useful only for testing
        ImageButton playButton = (ImageButton) findViewById(R.id.play_button);
        playButton.setTag(R.drawable.ic_action_play);

        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });

        songAdt = new SongAdapter(this, songList, this);
        songView.setAdapter(songAdt);
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if(!serviceBound)
                    return;
                //Toast.makeText(getApplicationContext(),
                //        "Click ListItem Number " + position + " id: " + id, Toast.LENGTH_LONG).show();

                musicSrv.setSong(position);
                musicSrv.playSong();
                updatePlayButton();
                songAdt.notifyDataSetChanged();
            }
        });

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            //startService(playIntent);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }

        duration = (TextView) findViewById(R.id.duration);
        currDuration = (TextView) findViewById(R.id.curr_duration);
        touchSeekbar = false;
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(seekbar.getVisibility() == TextView.VISIBLE) {
                currDuration.setText(Song.secondsToMinutes(seekBar.getProgress()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            touchSeekbar = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int states = PlayerState.Prepared |
                    PlayerState.Started |
                    PlayerState.Paused |
                    PlayerState.PlaybackCompleted;
            if(serviceBound && musicSrv.isInState(states)) {
                Log.d("Main", "onStopTrackingTouch setProgress" + Song.secondsToMinutes(seekBar.getProgress()));
                seekBar.setProgress(seekBar.getProgress());
                // valid state : {Prepared, Started, Paused, PlaybackCompleted}
                musicSrv.seekTo(seekBar.getProgress());
            }

            touchSeekbar = false;
        }
    };

/*
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Main", "onStart");
    }
*/

    @Override
    protected void onResume(){
        super.onResume();
        Log.d("Main", "onResume");

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // updateInfo must be run in activity thread
                runOnUiThread(updateInfo);
            }
        }, updateInterval*2, updateInterval);
    }


    @Override
    protected void onPause(){
        super.onPause();
        Log.d("Main", "onPause");

        savePreferences();
        timer.cancel();
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Main", "onStop");
        savePreferences();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Main", "onDestroy");

        savePreferences();

        if(serviceBound) {
            unbindService(musicConnection);
            serviceBound = false;
            musicSrv = null;
        }
    }

    private void hideSeekBarInfo() {
        duration.setVisibility(TextView.INVISIBLE);
        seekbar.setVisibility(TextView.INVISIBLE);
        currDuration.setText("SicMu Player");
    }

    final Runnable updateInfo = new Runnable() {
        public void run() {
            if(!serviceBound) {
                hideSeekBarInfo();
                return;
            }

            Song currSong = songList.get(musicSrv.getSong());
            int seekableStates = PlayerState.Preparing |
                    PlayerState.Prepared |
                    PlayerState.Started |
                    PlayerState.Paused |
                    PlayerState.PlaybackCompleted;

            // useful when MusicService go to next song
            if(musicSrv.getChanged()) {
                Log.d("Main", "updateInfo");
                songAdt.notifyDataSetChanged();

                if(musicSrv.isInState(seekableStates)) {
                    seekbar.setMax(currSong.getDuration());
                    duration.setText(Song.secondsToMinutes(currSong.getDuration()));
                    duration.setVisibility(TextView.VISIBLE);
                    seekbar.setVisibility(TextView.VISIBLE);
                }

                if(musicSrv.isInState(PlayerState.Nope)) {
                    // the MediaPlayer has been destroyed
                    updatePlayButton();
                }
            }

            // getCurrentPosition {Idle, Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted}
            if(musicSrv.isInState(seekableStates)) {
                if(!touchSeekbar && musicSrv.getSeekFinished()) {
                    Log.d("Main", "updateInfo setProgress" + Song.secondsToMinutes(musicSrv.getCurrentPosition()));
                    seekbar.setProgress(musicSrv.getCurrentPosition());
                }
            }
        }
    };

    private void restorePreferences() {
        SharedPreferences settings = getPreferences(0);
        int savedSong = settings.getInt(currSongPref, 0);
        // the songlist must have changed
        if (savedSong >= songList.size())
            savedSong = 0;
        Log.d("Main", "pref load song: " + savedSong);
        musicSrv.setSong(savedSong);
    }

    private void savePreferences() {
        if (!serviceBound)
            return;

        SharedPreferences settings = getPreferences(0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(currSongPref, musicSrv.getSong());
        editor.commit();

        Log.d("Main", "pref save song: " + musicSrv.getSong());
    }


    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
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
                songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, thisDuration / 1000));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);

        //Intent intent = new Intent(this, Settings.class);
        //startActivity(intent);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updatePlayButton() {
        ImageButton playButton = (ImageButton) findViewById(R.id.play_button);
        if(!musicSrv.isInState(PlayerState.Nope)) {
            if(!musicSrv.isInState(PlayerState.Paused)) {
                playButton.setImageResource(R.drawable.ic_action_pause);
                playButton.setTag(R.drawable.ic_action_pause);
            }
            else {
                playButton.setImageResource(R.drawable.ic_action_play);
                playButton.setTag(R.drawable.ic_action_play);
            }

            int seekableStates = PlayerState.Preparing |
                    PlayerState.Prepared |
                    PlayerState.Started |
                    PlayerState.Paused |
                    PlayerState.PlaybackCompleted;
            boolean seekableState = musicSrv.isInState(seekableStates);
            if(serviceBound && seekableState) {
                Song currSong = songList.get(musicSrv.getSong());
                duration.setText(Song.secondsToMinutes(currSong.getDuration()));
                duration.setVisibility(TextView.VISIBLE);
                seekbar.setMax(currSong.getDuration());
                seekbar.setProgress(musicSrv.getCurrentPosition());
                seekbar.setVisibility(TextView.VISIBLE);
                currDuration.setText(Song.secondsToMinutes(musicSrv.getCurrentPosition()));
            }
        }
        else {
            // MediaPlayer has been destroyed or first start
            playButton.setImageResource(R.drawable.ic_action_play);
            playButton.setTag(R.drawable.ic_action_play);
            hideSeekBarInfo();
        }
    }

    public void playOrPause(View view) {
        if(!serviceBound)
            return;

        if (musicSrv.isInState(PlayerState.Started)) {
            musicSrv.pausePlayer();
        }
        else {
            if (musicSrv.isInState(PlayerState.Paused)) {
                // previously paused
                musicSrv.go();
            }
            else {
                musicSrv.playSong();
            }
        }

        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void playNext(View view){
        if(!serviceBound)
            return;

        musicSrv.playNext();
        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void playPrev(View view){
        if(!serviceBound)
            return;

        musicSrv.playPrev();
        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void gotoCurrSong(View view) {
        if(!serviceBound)
            return;

        int gotoSong = musicSrv.getSong() - 3; // -3 to show a bit of songs before the cur song
        if(gotoSong < 0)
            gotoSong = 0;

        Log.d("Main", "gotoCurrSong:" + gotoSong);
        songView.setSelection(gotoSong);
        //songView.smoothScrollToPosition(gotoSong);
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
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     Intent intent = new Intent(this, Settings.class);
      startActivity(intent);
    */
}

