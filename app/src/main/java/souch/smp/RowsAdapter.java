package souch.smp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class RowsAdapter extends BaseAdapter {
    private Rows rows;
    private LayoutInflater songInf;
    private Main main;


    public RowsAdapter(Context c, Rows theRows, Main mn) {
        rows = theRows;
        songInf = LayoutInflater.from(c);
        main = mn;
    }

    @Override
    public int getCount() {
        return rows.size();
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
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            // map to song layout
            rowView = songInf.inflate(R.layout.song, parent, false);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) rowView.findViewById(R.id.song_title);
            viewHolder.image = (ImageView) rowView.findViewById(R.id.curr_play);
            viewHolder.duration = (TextView) rowView.findViewById(R.id.song_duration);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        Row row = rows.get(position);
        if(row.getClass() == RowSong.class) {
            RowSong rowSong = (RowSong) row;
            rowSong.setText(holder.text);
            rowSong.setDurationText(holder.duration);

            int currIcon = android.R.color.transparent;
            if (rowSong == rows.getCurrSong()) {
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
            RowGroup group = (RowGroup) row;
            group.setText(holder.text);
            group.setDurationText(holder.duration);
            holder.image.setImageResource(android.R.color.transparent);
        }

        return rowView;
    }

}
