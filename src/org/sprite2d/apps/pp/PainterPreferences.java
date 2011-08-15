package org.sprite2d.apps.pp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.util.Linkify;
import android.widget.TextView;

public class PainterPreferences extends PreferenceActivity implements
		OnPreferenceClickListener {

	private String mAboutPreferenceKey;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);

		this.mAboutPreferenceKey = this.getString(R.string.preferences_about);
		this.getPreferenceScreen().findPreference(this.mAboutPreferenceKey)
				.setOnPreferenceClickListener(this);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {			
		case R.id.dialog_about:
			return this.createDialogAbout();			
		
		default:
			return super.onCreateDialog(id);
			
		}
	}

	public boolean onPreferenceClick(Preference preference) {
		if (this.mAboutPreferenceKey.equals(preference.getKey())) {
			this.showDialog(R.id.dialog_about);
			return true;
		}

		return false;
	}
	
	private Dialog createDialogAbout() {
		final TextView message = new TextView(this);

		message.setAutoLinkMask(Linkify.ALL);
		message.setText(R.string.about_text);
		message.setPadding(20, 10, 20, 10);

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle(R.string.app_name);
		alert.setView(message);
		alert.setPositiveButton(android.R.string.ok, null);

		return alert.create();
	}

}
