package souch.smp;

public class SongItem {
    protected int padding;

    public String getStrPadding() {
        return new String(new char[padding]).replace('\0', ' ');
    }

    public SongItem(int pad) {
        padding = pad;
    }
}
