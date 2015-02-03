package souch.smp;

import android.widget.TextView;

public class SongGroup extends SongItem {
    protected String name;
    protected int endPos;


    public SongGroup(String theName, int padding) {
        super(padding);
        name = theName;
    }

    public void setEndPos(int end) { endPos = end; }
    public int getEndPos() { return endPos; }
    public String getName() { return name; }

    public void setText(TextView text) {
        super.setText(text);
        text.setText(name);
    }
}
