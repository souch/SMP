package souch.smp;

import android.widget.TextView;

public class RowGroup extends Row {
    protected String name;
    protected boolean folded;

    public RowGroup(int pos, int offset, String theName, int typeface) {
        super(pos, offset, typeface);
        name = theName;
        folded = false;
    }

    public String getName() { return name; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean fold) { folded = fold; }

    public boolean isPlaying() { return false; }

    public void setText(TextView text) {
        super.setText(text);
        text.setText(name);
        //text.setTextColor(R.color.apptheme_color);
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    public String toString() {
        return "SongGroup pos: " + genuinePos + " level: " + level + " name: " + name;
    }
}
