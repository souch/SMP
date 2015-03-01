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

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RowViewHolder {
    public RowViewHolder(View view) {
        layout = (RelativeLayout) view.findViewById(R.id.song_layout);
        text = (TextView) view.findViewById(R.id.song_title);
        image = (ImageView) view.findViewById(R.id.curr_play);
        duration = (TextView) view.findViewById(R.id.song_duration);
    }

    public RelativeLayout layout;
    public TextView text;
    public TextView duration;
    public ImageView image;
}
