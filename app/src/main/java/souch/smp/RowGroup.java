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
import android.util.TypedValue;
import android.widget.TextView;

public class RowGroup extends Row {
    protected String name;
    protected boolean folded;
    protected boolean selected;
    private int color;
    private int nbRowSong;
    protected static int textSize = 18;

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

    public void setView(RowViewHolder holder, Main main, int position) {
        super.setView(holder, main, position);

        holder.layout.getLayoutParams().height = convertDpToPixels((int) (textSize * 1.5),
                holder.layout.getResources());

        setText(holder.text);
        setDuration(holder.duration);
        holder.image.setImageResource(android.R.color.transparent);

        holder.layout.setBackgroundColor(color);
    }

    static int normalTextColor = Color.argb(0xFF, 0xf0, 0xf0, 0xf0);
    private void setText(TextView text) {
        text.setText(name);
        if (isFolded() && isSelected())
            text.setTextColor(Color.RED);
        else
            text.setTextColor(normalTextColor);

        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        //text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    private void setDuration(TextView duration) {
        String rightSpace = getStringOffset();
        //super.setText(text);
        if (isFolded()) {
            if (isSelected())
                duration.setTextColor(Color.RED);
            else
                duration.setTextColor(normalTextColor);
            duration.setText(nbRowSong + " |" + rightSpace);
        }
        else {
            duration.setText("/" + rightSpace);
            duration.setTextColor(normalTextColor);
        }

        duration.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        duration.setTypeface(null, typeface == Typeface.ITALIC ? Typeface.NORMAL : typeface);
        /*
        duration.setBackgroundColor(Color.argb(0x88, 0x30, 0x30, 0x30));
        duration.setId(position);
        duration.setOnClickListener(new View.OnClickListener() {
            public void onClick(View durationView) {
                Log.d("Main", "durationView.getId(): " + durationView.getId());
                durationView.setBackgroundColor(Color.argb(0x88, 0x65, 0x65, 0x65));

                class InvertFold implements Runnable {
                    View view;
                    InvertFold(View view) { this.view = view; }
                    public void run() {
                        main.invertFold(view.getId());
                        // todo: reset highlight color for a few ms after invertFold?
                    }
                }
                durationView.postDelayed(new InvertFold(durationView), 200);
            }
        });
        */
    }

    private String getStringOffset() {
        String offset = "", s = " ";
        for(int i = level ; i > 0 ; i--) {
            offset += s;
        }
        return offset;
    }

    public String toString() {
        return "SongGroup pos: " + genuinePos + " level: " + level + " name: " + name;
    }
}
