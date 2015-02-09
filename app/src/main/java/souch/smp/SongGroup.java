package souch.smp;

import android.widget.TextView;

public class SongGroup extends SongItem {
    protected String name;
    protected int endPos;
    protected int typeface;


    public SongGroup(String theName, int theTypeface, int padding) {
        super(padding);
        name = theName;
        typeface = theTypeface;
    }

    public void setEndPos(int end) { endPos = end; }
    public int getEndPos() { return endPos; }
    public String getName() { return name; }

    public void setText(TextView text) {
        super.setText(text);
        text.setText(name);
        text.setTypeface(null, typeface);
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }
}
