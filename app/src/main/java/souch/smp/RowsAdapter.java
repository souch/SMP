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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            // map to song layout
            rowView = songInf.inflate(R.layout.song, parent, false);
            // configure view holder
            RowViewHolder viewHolder = new RowViewHolder(rowView);
            rowView.setTag(viewHolder);
        }

        RowViewHolder holder = (RowViewHolder) rowView.getTag();

        rows.get(position).setView(holder, main, position);

        return rowView;
    }
}
