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
        addPreferencesFromResource(R.xml.preferences);

		mAboutPreferenceKey = getString(R.string.preferences_about);
        getPreferenceScreen().findPreference(mAboutPreferenceKey)
				.setOnPreferenceClickListener(this);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_about:
			return createDialogAbout();

		default:
			return super.onCreateDialog(id);
		}
	}

	public boolean onPreferenceClick(Preference preference) {
		if (mAboutPreferenceKey.equals(preference.getKey())) {
            showDialog(R.id.dialog_about);
			return true;
		}
		return false;
	}

	public void forkMe(View v) {
        dismissDialog(R.id.dialog_about);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getString(R.string.repo_url)));
        startActivity(Intent.createChooser(intent,
                getString(R.string.which_app)));
	}

	private Dialog createDialogAbout() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_about, null);
		dialogBuilder.setView(dialogView);

		try {
			((TextView) dialogView.findViewById(R.id.version)).setText(getString(
							R.string.app_version,
                    getPackageManager().getPackageInfo(
                            getPackageName(),
									PackageManager.GET_META_DATA).versionName));
		} catch (Exception e) {
		}

		dialogBuilder.setCancelable(true);
		dialogBuilder.setPositiveButton(android.R.string.ok, null);

		return dialogBuilder.create();
	}
}