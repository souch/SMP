package souch.smp;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;


public class Row {
    // level from the left
    protected int level;
    // position of the row within the unfolded rows array
    protected int genuinePos;
    protected int typeface;
    protected Row parent;

    public Row(int position, int theLevel, int theTypeface) {
        genuinePos = position;
        level = theLevel;
        typeface = theTypeface;
        parent = null;
    }

    public void setGenuinePos(int position) { genuinePos = position; }
    public int getGenuinePos() { return genuinePos; }

    public Row getParent() { return parent; }
    public void setParent(Row parent) { this.parent = parent; }

    public int getLevel() {
        return level;
    }


    public void setText(TextView text) {
        text.setPadding(getSongPadding(text.getResources()), 0, 0, 0);
        text.setTypeface(null, typeface);
    }

    public void setBackgroundColor(View view) {
        view.setBackgroundColor(Color.BLACK);
    }

    // cache result
    private static int lastPx1 = -1;
    private static int lastPx2 = -1;
    private int getSongPadding(Resources resources) {
        int px;
        switch(level) {
            case 1:
                if(lastPx1 < 0)
                    lastPx1 = convertDpToPixels(10, resources);
                px = lastPx1;
                break;
            case 2:
                if(lastPx2 < 0)
                    lastPx2 = convertDpToPixels(20, resources);
                px = lastPx2;
                break;
            default:
                px = 0;
        }
        return px;
    }

    private int convertDpToPixels(int dp, Resources resources) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                resources.getDisplayMetrics());
    }
}
