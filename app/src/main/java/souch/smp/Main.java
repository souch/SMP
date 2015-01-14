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
import android.widget.Toast;

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
    private boolean musicBound = false;

    // SiMu just started, and no song has been started
    boolean justStarted;

    Timer timer;
    final long timerDelayMs = 1000;

    // todo: how currSong is handled is a mess

    // the song pos in list known by the activity
    int currSong;
    // curr song played pos in ms.
    //int currPos;

    final String currSongPref = "currSong";




    //private boolean paused = false;
    private boolean playbackPaused = true;

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Main", "onServiceConnected");

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Main", "onServiceDisconnected");
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Main", "onCreate");
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        justStarted = true;

        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });
        restorePreferences();
        if (currSong >= songList.size())
            currSong = 0;

        songAdt = new SongAdapter(this, songList, this);
        songView.setAdapter(songAdt);
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //Toast.makeText(getApplicationContext(),
                //        "Click ListItem Number " + position + " id: " + id, Toast.LENGTH_LONG).show();

                justStarted = false;
                currSong = position;
                musicSrv.setSong(position);
                musicSrv.playSong();
                playbackPaused = false;
                updatePlayButton();
            }
        });

        gotoCurrSong(null);

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            //startService(playIntent);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

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
        }, timerDelayMs, timerDelayMs);
    }


    @Override
    protected void onPause(){
        super.onPause();
        Log.d("Main", "onPause");

        timer.cancel();
    }

/*
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Main", "onStop");
    }
*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Main", "onDestroy");

        savePreferences();

        if(musicBound) {
            unbindService(musicConnection);
            musicBound = false;
            musicSrv = null;
        }
    }


    final Runnable updateInfo = new Runnable() {
        public void run() {
            // retrieve the song pos of the service
            int songPos = getSong();
            // compare it to what we know from the activity
            if (!justStarted && songPos != -1 && songPos != currSong) {
                currSong = songPos;
                songAdt.notifyDataSetChanged();
            }

            //currPos = getCurrentPosition();
        }
    };

    private void restorePreferences() {
        SharedPreferences settings = getPreferences(0);
        currSong = settings.getInt(currSongPref, 0);

        Log.d("yo", "pref load song: " + currSong);
    }

    private void savePreferences() {
        SharedPreferences settings = getPreferences(0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(currSongPref, getSong());
        editor.commit();

        Log.d("yo", "pref save song: " + getSong());
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
                long thisDuration = musicCursor.getInt(durationColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, thisDuration));
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
        if(playbackPaused)
            playButton.setImageResource(R.drawable.ic_action_play);
        else
            playButton.setImageResource(R.drawable.ic_action_pause);
    }

    public void playOrPause(View view) {
        if (isPlaying()) {
            playbackPaused = true;
            musicSrv.pausePlayer();
        } else {
            if (justStarted) {
                // the app has just been launched
                musicSrv.setSong(currSong);
                musicSrv.playSong();
            }
            else {
                // previously paused
                musicSrv.go();
            }
            justStarted = false;
            playbackPaused = false;
        }

        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void playNext(View view){
        if (justStarted) {
            musicSrv.setSong(currSong);
        }
        musicSrv.playNext();
        currSong = musicSrv.getSong();
        playbackPaused = false;
        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void playPrev(View view){
        if (justStarted) {
            musicSrv.setSong(currSong);
        }
        musicSrv.playPrev();
        currSong = musicSrv.getSong();
        playbackPaused = false;
        updatePlayButton();
        songAdt.notifyDataSetChanged();
    }

    public void gotoCurrSong(View view) {
        int gotoSong = getSong() - 3; // -3 to show a bit of songs before the cur song
        if(gotoSong < 0)
            gotoSong = 0;
        songView.setSelection(gotoSong);
        //songView.smoothScrollToPosition(gotoSong);
    }

    // safe getSong : return guessed song pos if musicSrv down
    public int getSong() {
        if(!justStarted && musicSrv != null && musicBound)
            return musicSrv.getSong();
        else return currSong;
    }

    public boolean getPlaybackPaused() {
        return playbackPaused;
    }

    public int getDuration() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getDuration();
        else return 0;
    }


    public int getCurrentPosition() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getCurrentPosition();
        else return 0;
    }

    public void seekTo(int pos) {
        musicSrv.seekTo(pos);
    }

    public boolean isPlaying() {
        if(musicSrv != null && musicBound)
            return musicSrv.isPlaying();
        return false;
    }


    // exit nicely
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

