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

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

public class RowGroup extends Row {
    protected String name;
    protected boolean folded;
    protected boolean selected;
    private int color;
    private int nbRowSong;

    public RowGroup(int pos, int offset, String name, int typeface, int color) {
        super(pos, offset, typeface);
        this.name = name;
        folded = false;
        selected = false;
        this.color = color;
    }

    public String getName() { return name; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean fold) { folded = fold; }

    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }

    // get number of songs (excluding RowGroup) inside this group
    public int nbRowSong() { return nbRowSong; }
    public void incNbRowSong() { nbRowSong++; }

    public void setText(TextView text) {
        super.setText(text);
        text.setText(name);
        if (isFolded() && isSelected())
            text.setTextColor(Color.RED);
        else
            text.setTextColor(Color.WHITE);
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        //text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    public void setDurationText(TextView text) {
        //super.setText(text);
        if (isFolded()) {
            if (isSelected())
                text.setTextColor(Color.RED);
            else
                text.setTextColor(Color.WHITE);
            text.setText(nbRowSong + " v");
        }
        else {
            text.setText("\u028c");
            text.setTextColor(Color.WHITE);
        }
        text.setTypeface(null, typeface == Typeface.ITALIC ? Typeface.NORMAL : typeface);
        text.setBackgroundColor(Color.argb(0x88, 0x45, 0x45, 0x45));
    }

    public void setBackgroundColor(View view) {
        view.setBackgroundColor(color);
    }

    public String toString() {
        return "SongGroup pos: " + genuinePos + " level: " + level + " name: " + name;
    }
}
