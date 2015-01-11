package souch.smp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Main extends Activity {
    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private MutableInteger songPicked;


    //private boolean paused = false;
    private boolean playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        songPicked = new MutableInteger();
        songPicked.value = -1;

        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList, songPicked);
        songView.setAdapter(songAdt);
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(getApplicationContext(),
                        "Click ListItem Number " + position + " id: " + id, Toast.LENGTH_LONG).show();

                songPicked.value = position;
                ImageView currPlay = (ImageView) view.findViewById(R.id.curr_play);
                currPlay.setImageResource(R.drawable.ic_curr_play);
                //Toast.makeText(getApplicationContext(), "iv" + iv, Toast.LENGTH_LONG).show();


                musicSrv.setSong(position);
                musicSrv.playSong();
                if (playbackPaused) {
                    playbackPaused = false;
                }
                updatePlayButton();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    /*
    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
*/

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

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
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum));
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
            musicSrv.go();
            playbackPaused = false;
        }

        updatePlayButton();

        /*
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
        */
    }

    public void playNext(View view){
        musicSrv.playNext();
        playbackPaused = false;
        updatePlayButton();
    }

    public void playPrev(View view){
        musicSrv.playPrev();
        playbackPaused = false;
        updatePlayButton();
    }

    public int getDuration() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getDur();
        else return 0;
    }

    public int getCurrentPosition() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getPosn();
        else return 0;
    }

    public void seekTo(int pos) {
        musicSrv.seek(pos);
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
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}

