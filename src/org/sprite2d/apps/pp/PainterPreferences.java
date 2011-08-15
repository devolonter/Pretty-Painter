package org.sprite2d.apps.pp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
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

	public void forkMe(View v) {
		this.dismissDialog(R.id.dialog_about);
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri
				.parse(this.getString(R.string.repo_url)));
		this.startActivity(intent);		
	}

	private Dialog createDialogAbout() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = this.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_about, null);
		dialogBuilder.setView(dialogView);

		try {
			((TextView) dialogView.findViewById(R.id.version)).setText(this
					.getString(
							R.string.app_version,
							this.getPackageManager().getPackageInfo(
									this.getPackageName(),
									PackageManager.GET_META_DATA).versionName));
		} catch (Exception e) {
		}

		dialogBuilder.setCancelable(true);
		dialogBuilder.setPositiveButton(android.R.string.ok, null);

		return dialogBuilder.create();
	}

}
