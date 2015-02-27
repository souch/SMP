package souch.smp;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RowViewHolder {
    public RowViewHolder(View view) {
        layout = (RelativeLayout) view.findViewById(R.id.song_layout);
        text = (TextView) view.findViewById(R.id.song_title);
        image = (ImageView) view.findViewById(R.id.curr_play);
        duration = (TextView) view.findViewById(R.id.song_duration);
    }

    public RelativeLayout layout;
    public TextView text;
    public TextView duration;
    public ImageView image;
}
