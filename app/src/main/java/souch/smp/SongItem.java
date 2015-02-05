package souch.smp;

import android.widget.TextView;

public class SongItem {
    protected int padding;

    public SongItem(int pad) {
        padding = pad;
    }

    public void setPadding(int pad) {
        padding = pad;
    }

    public void setText(TextView text) {
        text.setPadding(padding * 10, 0, 0, 0);
    }
}
