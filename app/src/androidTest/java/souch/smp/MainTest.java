/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.robotium.solo.Solo;

import junit.framework.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;
    private Main main;

    private final int maxAcceptableUserDelay = 2000;
    private final int minSong = 4;
    // posSong of the prev test
    private int prevPosSong;

    public MainTest() {
        super(Main.class);
    }

    // todo: reimplement tests
/*
    public void setUp() throws Exception {
        super.setUp();
        main = getActivity();
        solo = new Solo(getInstrumentation(), main);
        Log.d("MainTest", "====================================");
        prevPosSong = -1;
    }


    // simple test of playing the first song
    public void test1PlayOneSong() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        //changeNbSong(1);
        int linePos = 1;
        solo.scrollToTop();
        solo.clickInList(linePos);

        // gives the whole thing 2 second to start
        checkPlayOk(linePos, true);

        // set the song genuinePos for the next test of loadpref
        prevPosSong = getMusicSrv().getSong();
    }


    // the the curr play icon is shown at the right genuinePos
    public void test2PlayButton() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        int linePos = 3;
        solo.scrollToTop();
        solo.clickInList(linePos);
        // gives the whole thing 2 second to start
        checkPlayOk(linePos, true);

        // pause
        clickOnButton(R.id.play_button);
        checkPlayOk(linePos, false);

        // next should go to next and unpause
        clickOnButton(R.id.next_button);
        linePos++;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.next_button);
        linePos++;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.prev_button);
        linePos--;
        checkPlayOk(linePos, true);

        solo.scrollToTop();
        solo.clickInList(1);
        linePos = 1;
        checkPlayOk(linePos, true);
        // going backward at the top go to the bottom
        clickOnButton(R.id.prev_button);
        linePos = getNbSong() - 1;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.goto_button);
        checkPlayOk(linePos, true);
        ListView songList = (ListView) solo.getView(R.id.song_list);
        Assert.assertTrue(songList.getCount() - 1 == songList.getLastVisiblePosition());

        // going forward at the bottom go to the top
        clickOnButton(R.id.next_button);
        linePos = 0;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.goto_button);
        checkPlayOk(linePos, true);
        Assert.assertTrue(0 == songList.getFirstVisiblePosition());

        // pick a song
        linePos = 4;
        solo.clickInList(linePos);
        checkPlayOk(linePos, true);

        prevPosSong = linePos;
    }

    public void test3NoSongs() throws Exception {
        changeNbSong(0);

        clickOnButton(R.id.play_button);

        clickOnButton(R.id.next_button);

        clickOnButton(R.id.goto_button);

        clickOnButton(R.id.prev_button);

        clickOnButton(R.id.goto_button);
    }

    // todo: see if the listview update curr pause when musicservice goes to next song automatically.

    // todo: seekbar tests

    public void testZLoadPref() throws Exception {
        checkEnoughSong();
        checkLoadPref();
    }

    public void testPlayerState() throws Exception {
        PlayerState ps = new PlayerState();
        Assert.assertTrue(ps.getState() == PlayerState.Nope);
        Assert.assertTrue(ps.compare(PlayerState.Nope));
        ps.setState(PlayerState.Initialized);
        Assert.assertTrue(ps.compare(PlayerState.Nope | PlayerState.Initialized | PlayerState.End));
        Assert.assertFalse(ps.compare(PlayerState.Nope | PlayerState.End));
    }

    private void clickOnButton(int id) {
        // this does not work:
        //solo.clickOnButton(R.id.play_button);
        // this works:
        solo.clickOnView(solo.getView(id));
    }

    // test that there is enough song for performing other tests
    public void checkEnoughSong() throws Exception {
        int nbSong = getNbSong();
        Log.d("MainTest", "songs size: " + nbSong);
        Assert.assertTrue(nbSong >= minSong);
    }

    private int getNbSong() throws Exception {
        Field field = main.getClass().getDeclaredField("songs");
        field.setAccessible(true);
        ArrayList<RowSong> rowSongList = (ArrayList<RowSong>) field.get(main);
        return rowSongList.size();
    }

    // reduce the number of song available
    private void changeNbSong(int nbSong) throws Exception {
        Assert.assertTrue(nbSong >= 0);
        Assert.assertTrue(nbSong <= minSong);

        Field field = main.getClass().getDeclaredField("songs");
        field.setAccessible(true);
        ArrayList<RowSong> rowSongList = (ArrayList<RowSong>) field.get(main);
        Assert.assertTrue(nbSong <= rowSongList.size());

        while(rowSongList.size() > nbSong) {
            rowSongList.remove(rowSongList.size() - 1);
        }
        field.set(main, rowSongList);


        //field = main.getClass().getDeclaredField("songAdt");
        //field.setAccessible(true);
        //SongAdapter songAdt = (SongAdapter) field.get(main);
        //songAdt.notifyDataSetChanged();

    }

    // check that the curr icon is well set
    // linePos start from 1
    private void checkPlayOk(int linePos, boolean isPlaying) throws Exception {
        Log.d("MainTest", "checkPlayOk linePos:" + linePos + " playing: " + isPlaying);
        SystemClock.sleep(maxAcceptableUserDelay);
        Assert.assertTrue(getMusicSrv().isPlaying() == isPlaying);


        // check the play button (play or pause)
        int ic_action = R.drawable.ic_action_pause;
        if(!isPlaying)
            ic_action = R.drawable.ic_action_play;
        // this make the listview scroll forever and make the test fail. don't know why :
        //Assert.assertTrue(((int) solo.getButton(R.id.play_button).getTag()) == ic_action);
        // this works
        Assert.assertTrue(((int) (solo.getView(R.id.play_button)).getTag()) == ic_action);


        // check the image that show the current song (played or paused) in the list
        int songPos = linePos > 0 ? linePos - 1 : linePos; // clickInList start from 1, getChildAt from 0
        //Log.d("MainTest", "ic:" + R.drawable.ic_curr_pause + " - play:" + R.drawable.ic_curr_play + " - trans:" + R.drawable.ic_transparent);
        ListView songList = (ListView) solo.getView(R.id.song_list);
        int i;
        Log.d("MainTest", "songList.getCount():" + songList.getCount() + " songList.firstpos:" + songList.getFirstVisiblePosition() + " lastpos: " + songList.getLastVisiblePosition());
        for(i = songList.getFirstVisiblePosition(); i < songList.getLastVisiblePosition(); i++) {
            RelativeLayout songItem = (RelativeLayout) songList.getChildAt(i);
            // fixme: I don't understand why songItem is null
            if(songItem == null) {
                Log.w("MainTest", "!!! songItem null; i:" + i);
                continue;
            }
            ImageView currPlay = (ImageView) songItem.findViewById(R.id.curr_play);


            int ic_curr = R.drawable.ic_curr_play;
            if(!isPlaying)
                ic_curr = R.drawable.ic_curr_pause;

            //Assert.assertTrue(((int) currPlay.getTag()) == (i != songPos ? R.drawable.ic_transparent : ic_curr));
        }
    }

    public void checkLoadPref() throws Exception {
        if(prevPosSong != -1) {
            // check the the song is put at the last genuinePos the app was
            ListView songList = (ListView) solo.getView(R.id.song_list);
            Assert.assertEquals(songList.getSelectedItemPosition(), prevPosSong);
        }
    }

    private MusicService getMusicSrv() throws Exception {
        Field field = main.getClass().getDeclaredField("musicSrv");
        field.setAccessible(true);
        return (MusicService) field.get(main);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    */
}