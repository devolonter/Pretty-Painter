package org.sprite2d.apps.pp;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PainterPreferences extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
	}
}
