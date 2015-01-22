package souch.smp;

import java.util.BitSet;

public class MediaPlayerState {
    static public final int Nope = 0;
    static public final int Idle = 1;
    static public final int Initialized = 2;
    static public final int Preparing = 3;
    static public final int Prepared = 4;
    static public final int Started = 5;
    static public final int Paused = 6;
    static public final int PlaybackCompleted = 7;
    static public final int Stopped = 8;
    static public final int End = 9;
    static public final int Error = 10;

    private int state;

    public MediaPlayerState() {
        state = Nope;
    }

    public void setState(int s) {
        state = s;
    }

    public int getState() {
        return state;
    }

    public boolean compare1State(int s) {
        return s == state;
    }

    public boolean compareXState(int [] states) {
        int i;
        for(i = 0; i < states.length ; i++) {
            if(state == states[i])
                return true;
        }
        return false;
    }
}
