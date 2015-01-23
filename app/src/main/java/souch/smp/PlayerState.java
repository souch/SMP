package souch.smp;

public class PlayerState {
    static public final int Nope        = 0b1;
    static public final int Idle        = 0b10;
    static public final int Initialized = 0b100;
    static public final int Preparing   = 0b1000;
    static public final int Prepared    = 0b10000;
    static public final int Started     = 0b100000;
    static public final int Paused      = 0b1000000;
    static public final int PlaybackCompleted = 0b10000000;
    static public final int Stopped     = 0b100000000;
    static public final int End         = 0b1000000000;
    static public final int Error       = 0b10000000000;

    private int state;

    public PlayerState() {
        state = Nope;
    }

    public void setState(int s) {
        state = s;
    }

    public int getState() {
        return state;
    }

    public boolean compare(int states) {
        return (state & states) != 0;
    }

}
