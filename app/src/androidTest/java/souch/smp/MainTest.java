package souch.smp;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import junit.framework.Assert;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {

    private Solo solo;

    public MainTest() {
        super(Main.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testPreferenceIsSaved() throws Exception {

        /*
        solo.sendKey(Solo.MENU);
        solo.clickOnText("More");
        solo.clickOnText("Preferences");
        solo.clickOnText("Edit File Extensions");
        Assert.assertTrue(solo.searchText("rtf"));

        solo.clickOnText("txt");
        solo.clearEditText(2);
        solo.enterText(2, "robotium");
        solo.clickOnButton("Save");
        solo.goBack();
        solo.clickOnText("Edit File Extensions");
        Assert.assertTrue(solo.searchText("application/robotium"));
        */

    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }


    public void testOnKeyDown() throws Exception {
        //solo.goBack();
    }
}