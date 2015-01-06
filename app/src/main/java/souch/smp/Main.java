package souch.smp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class Main extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.activity_main);

        final ListView listView = (ListView) findViewById(R.id.listView);
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

        for(int i = 0; i < 100; i++) {
            map = new HashMap<String, String>();
            map.put("tab", "   ");
            map.put("desc", "Yup " + i);
            listItem.add(map);
         }

        SimpleAdapter mSchedule = new SimpleAdapter (this.getBaseContext(), listItem, R.layout.item_listview,
               new String[] {"tab", "desc"}, new int[] {R.id.tab, R.id.desc});

        listView.setAdapter(mSchedule);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                //on récupère la HashMap contenant les infos de notre item (titre, description, img)
                HashMap<String, String> map = (HashMap<String, String>) listView.getItemAtPosition(position);
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
