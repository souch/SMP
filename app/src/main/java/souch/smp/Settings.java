package souch.smp;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

    }

    private void getPreferences() {
/*
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        ((TextView)findViewById(R.id.tvLogin)).setText("Nom d'utilisateur : " + preferences.getString("login", ""));

        ((TextView)findViewById(R.id.tvPassword)).setText("Mot de passe : " + preferences.getString("password", ""));

        ((TextView)findViewById(R.id.tvRingtone)).setText("Sonnerie : " + preferences.getString("sonnerie", ""));

        ((TextView)findViewById(R.id.tvVibrate)).setText("Vibreur : " + preferences.getBoolean("vibrate", false));
*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
/*
        if(requestCode == CODE_RETOUR) {
            Toast.makeText(this, "Modifications terminées", Toast.LENGTH_SHORT).show();
            getPreferences();
        }

        super.onActivityResult(requestCode, resultCode, data);
*/
    }
}
