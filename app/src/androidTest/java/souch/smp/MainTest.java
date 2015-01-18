package souch.smp;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.robotium.solo.Solo;

import junit.framework.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;
    private Main main;
    final int minSong = 4;
    // posSong of the prev test
    int prevPosSong;

    public MainTest() {
        super(Main.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        main = getActivity();
        solo = new Solo(getInstrumentation(), main);
        Log.d("MainTest", "====================================");
        prevPosSong = -1;
    }


    // simple test of playing the first song
    public void testPlayOneSong() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        //changeNbSong(1);
        int linePos = 1;
        solo.scrollToTop();
        solo.clickInList(linePos);
        // set the song pos for the next test of loadpref
        prevPosSong = linePos;

        // gives the whole thing 2 second to start
        SystemClock.sleep(2000);
        Assert.assertTrue(getMusicSrv().isPlaying());

        checkCurrPlayOk(linePos);
    }


    // the the curr play icon is shown at the right pos
    public void testCurrPlay() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        int linePos = 3;
        solo.scrollToTop();
        solo.clickInList(linePos);
        prevPosSong = linePos;

        // gives the whole thing 2 second to start
        SystemClock.sleep(2000);
        Assert.assertTrue(getMusicSrv().isPlaying());


        checkCurrPlayOk(linePos);

        /*
        solo.clickOnScreen(5, 50);
        ImageView currPlay = (ImageView) solo.getView(R.id.curr_play);
        Log.d("MainTest", "currPlay.getTag(); " + currPlay.getTag());
        Log.d("MainTest", "currPlay.getTag(); " + currPlay.getId());
        */
        //ListView songView = (ListView) solo.getView(R.id.song_list);
    }

    public void testLoadPref() throws Exception {
        checkEnoughSong();
        checkLoadPref();
    }


    // test that there is enough song for performing other tests
    public void checkEnoughSong() throws Exception {
        int nbSong = getNbSong();
        Log.d("MainTest", "songList size: " + nbSong);
        Assert.assertTrue(nbSong >= minSong);
    }

    private int getNbSong() throws Exception {
        Field field = main.getClass().getDeclaredField("songList");
        field.setAccessible(true);
        ArrayList<Song> songList = (ArrayList<Song>) field.get(main);
        return songList.size();
    }

    private void changeNbSong(int nbSong) throws Exception {
        Assert.assertTrue(nbSong >= 0);
        Assert.assertTrue(nbSong <= minSong);

        Field field = main.getClass().getDeclaredField("songList");
        field.setAccessible(true);
        ArrayList<Song> songList = (ArrayList<Song>) field.get(main);
        Assert.assertTrue(nbSong <= songList.size());

        while(songList.size() > nbSong) {
            songList.remove(songList.size() - 1);
        }
        field.set(main, songList);
    }

    // check that the curr icon is well set
    // linePos start from 1
    private void checkCurrPlayOk(int linePos) {
        Log.d("MainTest", "ic:" + R.drawable.ic_curr_pause + " - play:" + R.drawable.ic_curr_play + " - trans:" + R.drawable.ic_transparent);
        ListView songList = (ListView) solo.getView(R.id.song_list);
        int songPos = linePos > 0 ? linePos - 1 : linePos; // clickInList start from 1, getChildAt from 0
        int i;
        for(i = 0; i < songList.getLastVisiblePosition(); i++) {
            RelativeLayout songItem = (RelativeLayout) songList.getChildAt(i);
            ImageView currPlay = (ImageView) songItem.findViewById(R.id.curr_play);

            Log.d("MainTest", "i: " + i);
            Log.d("MainTest", "currPlay.getTag(): " + currPlay.getTag());
            TextView title = (TextView) songItem.findViewById(R.id.song_title);
            Log.d("MainTest", "title: " + title.getText());

            Assert.assertTrue(((int) currPlay.getTag()) == (i != songPos ? R.drawable.ic_transparent : R.drawable.ic_curr_play));
        }
/*
        TextView title = (TextView) songItem.findViewById(R.id.song_title);
        Log.d("MainTest", "title: " + title.getText());
        Log.d("MainTest", "currPlay.getTag(): " + currPlay.getTag());
        Log.d("MainTest", "ic:" + R.drawable.ic_curr_pause + " - play:" + R.drawable.ic_curr_play + " - trens:" + R.drawable.ic_transparent);
*/
    }

    public void checkLoadPref() throws Exception {
        if(prevPosSong != -1) {
            // check the the song is put at the last pos the app was
            ListView songList = (ListView) solo.getView(R.id.song_list);
            Assert.assertEquals(songList.getSelectedItemPosition(), prevPosSong);
        }
    }

    private MusicService getMusicSrv() throws Exception {
        Field field = main.getClass().getDeclaredField("musicSrv");
        field.setAccessible(true);
        return (MusicService) field.get(main);
    }

    /*
    public void testExit() throws Exception {
        solo.goBack();
    }
*/

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

/*
    public void testOnKeyDown() throws Exception {
        //solo.goBack();
    }
    */
}