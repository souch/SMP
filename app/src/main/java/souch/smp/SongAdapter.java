package souch.smp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInf;
    private Main main;


    public SongAdapter(Context c, ArrayList<Song> theSongs, Main mn){
        songs = theSongs;
        songInf = LayoutInflater.from(c);
        main = mn;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String convertToMinute(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.valueOf(minutes) + (seconds < 10 ? ":0" : ":") + String.valueOf(seconds);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        RelativeLayout itemView = (RelativeLayout) songInf.inflate(R.layout.song, parent, false);
        //get title and artist views
        TextView songView = (TextView) itemView.findViewById(R.id.song_title);
        TextView artistView = (TextView) itemView.findViewById(R.id.song_artist);
        TextView durationView = (TextView) itemView.findViewById(R.id.song_duration);
        ImageView currPlay = (ImageView) itemView.findViewById(R.id.curr_play);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist() + " - " + currSong.getAlbum());
        durationView.setText(convertToMinute(currSong.getDuration()));

        int currIcon = R.drawable.ic_transparent;
        if(position == main.getSong()) {
            if(main.getPlaying())
                currIcon = R.drawable.ic_curr_play;
            else
                currIcon = R.drawable.ic_curr_pause;
        }
        currPlay.setImageResource(currIcon);
        // useful only for the tests
        currPlay.setTag(currIcon);

        //set position as tag (not useful for now)
        //itemView.setTag(position);
        return itemView;
    }


}
