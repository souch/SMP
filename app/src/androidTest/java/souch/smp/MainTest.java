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

    public MainTest() {
        super(Main.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        main = getActivity();
        solo = new Solo(getInstrumentation(), main);
        Log.d("MainTest", "====================================");
    }


    // simple test of playing the first song
    public void test1PlayFirstSong() throws Exception {
        checkEnoughSong();

        //changeNbSong(1);
        int linePos = 1;
        solo.scrollToTop();
        solo.clickInList(linePos);
        // gives the whole thing 2 second to start
        SystemClock.sleep(2000);
        Assert.assertTrue(getMusicSrv().isPlaying());
    }

    // the the curr play icon is shown at the right pos
    public void test2CurrPlay() throws Exception {
        checkEnoughSong();

        int linePos = 2;
        solo.scrollToTop();
        solo.clickInList(linePos);
        // gives the whole thing 2 second to start
        SystemClock.sleep(2000);
        Assert.assertTrue(getMusicSrv().isPlaying());

        // check that the curr icon is well set
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
        //Assert.assertTrue(((int) currPlay.getTag()) == R.drawable.ic_curr_play);


        /*
        solo.clickOnScreen(5, 50);
        ImageView currPlay = (ImageView) solo.getView(R.id.curr_play);
        Log.d("MainTest", "currPlay.getTag(); " + currPlay.getTag());
        Log.d("MainTest", "currPlay.getTag(); " + currPlay.getId());
        */
        //ListView songView = (ListView) solo.getView(R.id.song_list);
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