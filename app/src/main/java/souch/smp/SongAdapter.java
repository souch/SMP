package souch.smp;

import android.content.Context;
import android.graphics.Typeface;
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

    static class ViewHolder {
        public TextView text;
        public TextView duration;
        public ImageView image;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;
        // reuse views
        if (itemView == null) {
            // map to song layout
            itemView = songInf.inflate(R.layout.song, parent, false);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) itemView.findViewById(R.id.song_title);
            viewHolder.image = (ImageView) itemView.findViewById(R.id.curr_play);
            viewHolder.duration = (TextView) itemView.findViewById(R.id.song_duration);
            itemView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) itemView.getTag();

        SongItem currItem = songItems.get(position);
        if(currItem.getClass() == Song.class) {
            Song song = (Song) currItem;
            song.setText(holder.text);
            holder.duration.setText(Song.secondsToMinutes(song.getDuration()));

            int currIcon = android.R.color.transparent;
            if (currItem == main.getSongItem()) {
                if (main.getMusicSrv().playingLaunched())
                    currIcon = R.drawable.ic_curr_play;
                else
                    currIcon = R.drawable.ic_curr_pause;
            }
            holder.image.setImageResource(currIcon);
            // useful only for the tests
            holder.image.setTag(currIcon);
        }
        else {
            SongGroup group = (SongGroup) currItem;
            group.setText(holder.text);
            holder.duration.setText("");
            holder.image.setImageResource(android.R.color.transparent);
        }

        return itemView;
    }

}
