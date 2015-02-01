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
    private ArrayList<SongItem> songItems;
    private LayoutInflater songInf;
    private Main main;


    public SongAdapter(Context c, ArrayList<SongItem> items, Main mn){
        songItems = items;
        songInf = LayoutInflater.from(c);
        main = mn;
    }

    @Override
    public int getCount() {
        return songItems.size();
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
        SongItem currItem = songItems.get(position);

        // map to song layout
        RelativeLayout itemView = (RelativeLayout) songInf.inflate(R.layout.song, parent, false);

        if(currItem.getClass() == Song.class) {
            Song song = (Song) currItem;
            TextView title = (TextView) itemView.findViewById(R.id.song_title);
            title.setText(song.getTitle());

            TextView duration = (TextView) itemView.findViewById(R.id.song_duration);
            duration.setText(Song.secondsToMinutes(song.getDuration()));

            ImageView currPlay = (ImageView) itemView.findViewById(R.id.curr_play);
            int currIcon = android.R.color.transparent;
            if (currItem == main.getSongItem()) {
                if (main.isInState(PlayerState.Paused))
                    currIcon = R.drawable.ic_curr_pause;
                else
                    currIcon = R.drawable.ic_curr_play;
            }
            currPlay.setImageResource(currIcon);
            // useful only for the tests
            currPlay.setTag(currIcon);
        }
        else {
            SongGroup group = (SongGroup) currItem;
            group.setText((TextView) itemView.findViewById(R.id.song_title));

            TextView duration = (TextView) itemView.findViewById(R.id.song_duration);
            duration.setText("");
        }

        //set position as tag (not useful for now)
        //itemView.setTag(position);
        return itemView;
    }

}
