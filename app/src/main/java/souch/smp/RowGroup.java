package souch.smp;

import android.graphics.Color;
import android.widget.TextView;

public class RowGroup extends Row {
    protected String name;
    protected boolean folded;
    protected boolean selected;

    public RowGroup(int pos, int offset, String theName, int typeface) {
        super(pos, offset, typeface);
        name = theName;
        folded = false;
        selected = false;
    }

    public String getName() { return name; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean fold) { folded = fold; }

    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }

    public void setText(TextView text) {
        super.setText(text);
        text.setText(name);
        if (isFolded() && isSelected())
            text.setTextColor(Color.RED);
        else
            text.setTextColor(Color.WHITE);
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    public void setDurationText(TextView text) {
        //super.setText(text);
        if (isFolded()) {
            if (isSelected())
                text.setTextColor(Color.RED);
            else
                text.setTextColor(Color.WHITE);
            text.setText("...");
        }
        else {
            text.setText("");
        }
    }

    public String toString() {
        return "SongGroup pos: " + genuinePos + " level: " + level + " name: " + name;
    }
}
