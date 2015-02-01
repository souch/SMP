package souch.smp;

import android.graphics.Typeface;
import android.widget.TextView;

public class SongGroupArtist extends SongGroup {
    public SongGroupArtist(String name, int padding) {
        super(name, padding);
    }

    public void setText(TextView text) {
        super.setText(text);
        text.setTypeface(Typeface.DEFAULT_BOLD);
    }
}
