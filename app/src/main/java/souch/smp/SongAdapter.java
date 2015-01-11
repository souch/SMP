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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        //RelativeLayout songLay = (RelativeLayout) songInf.inflate(R.layout.song, parent, false);
        RelativeLayout itemView = (RelativeLayout) songInf.inflate(R.layout.song, parent, false);
        //get title and artist views
        TextView songView = (TextView) itemView.findViewById(R.id.song_title);
        TextView artistView = (TextView) itemView.findViewById(R.id.song_artist);
        ImageView currPlay = (ImageView) itemView.findViewById(R.id.curr_play);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist() + " - " + currSong.getAlbum());

        int currIcon = R.drawable.ic_transparent;
        if(position == main.getSong()) {
            if(main.getPlaybackPaused())
                currIcon = R.drawable.ic_curr_pause;
            else
                currIcon = R.drawable.ic_curr_play;
        }
        currPlay.setImageResource(currIcon);

        //set position as tag
        itemView.setTag(position);
        return itemView;
    }


}
