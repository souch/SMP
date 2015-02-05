package souch.smp;

import android.graphics.Typeface;
import android.widget.TextView;

public class SongGroupFolder extends SongGroup {
    public SongGroupFolder(String name, int padding) {
        super(name, padding);
    }

    public void setText(TextView text) {
        super.setText(text);
        text.setTypeface(null, Typeface.BOLD);
    }
}
