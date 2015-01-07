package souch.smp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Main extends Activity {

    private ArrayList<Song> songList;
    private ListView songView;

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        /*
        ArrayList<HashMap<String, String>> listItem = new ArrayList<HashMap<String, String>>();

        //On déclare la HashMap qui contiendra les informations pour un item
        HashMap<String, String> map;
        map = new HashMap<String, String>();
        map.put("tab", " ");
        map.put("desc", "Folder 1");
        listItem.add(map);

        map = new HashMap<String, String>();
        map.put("tab", "   ");
        map.put("desc", "Yo");
        listItem.add(map);

        map = new HashMap<String, String>();
        map.put("tab", " ");
        map.put("desc", "Folder 2");
        listItem.add(map);

        map = new HashMap<String, String>();
        map.put("tab", "   ");
        map.put("desc", "Yup");
        listItem.add(map);

        for(int i = 0; i < 10000; i++) {
            map = new HashMap<String, String>();
            map.put("tab", "   ");
            map.put("desc", "Yup " + i);
            listItem.add(map);
         }

        SimpleAdapter mSchedule = new SimpleAdapter (this.getBaseContext(), listItem, R.layout.song,
               new String[] {"tab", "desc"}, new int[] {R.id.tab, R.id.desc});

        songList.setAdapter(mSchedule);

        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                //on récupère la HashMap contenant les infos de notre item (titre, description, img)
                HashMap<String, String> map = (HashMap<String, String>) songView.getItemAtPosition(position);
                //on créé une boite de dialogue
                AlertDialog.Builder adb = new AlertDialog.Builder(Main.this);
                //on attribue un titre à notre boite de dialogue
                adb.setTitle("Sélection Item");
                //on insère un message à notre boite de dialogue, et ici on affiche le titre de l'item cliqué
                adb.setMessage("Votre choix : " + map.get("desc"));
                //on indique que l'on veut le bouton ok à notre boite de dialogue
                adb.setPositiveButton("Ok", null);
                //on affiche la boite de dialogue
                adb.show();
            }
        });
        */
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

    public void playPause(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

/*
    public void next(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }
    */
}
