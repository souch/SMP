package souch.smp;

import android.graphics.Typeface;
import android.widget.TextView;

public class SongGroupAlbum extends SongGroup {
    public SongGroupAlbum(String name, int padding) {
        super(name, padding);
    }

    public void setText(TextView text) {
        super.setText(text);
        text.setTypeface(null, Typeface.ITALIC);
    }
}

