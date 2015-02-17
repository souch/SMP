/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
            holder.duration.setOnClickListener(null);
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
            holder.image.setImageResource(android.R.color.transparent);
            group.setDurationText(holder.duration);
            holder.duration.setId(position);
            holder.duration.setOnClickListener(new View.OnClickListener() {
                public void onClick(View durationView) {
                    Log.d("Main", "durationView.getId(): " + durationView.getId());
                    durationView.setBackgroundColor(Color.argb(0x88, 0x65, 0x65, 0x65));

                    // todo: rearrange code, this should be elsewhere
                    class InvertFold implements Runnable {
                        View view;
                        InvertFold(View view) { this.view = view; }
                        public void run() {
                            main.invertFold(view.getId());
                            // todo: reset highlight color for a few ms after invertFold?
                        }
                    }
                    durationView.postDelayed(new InvertFold(durationView), 300);
                }
            });
        }
        row.setBackgroundColor(rowView);

        return rowView;
    }
}
