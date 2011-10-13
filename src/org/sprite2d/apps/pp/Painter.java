package org.sprite2d.apps.pp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Base application logic
 *
 * @author Arthur Bikmullin (devolonter)
 * @version 1.17
 *
 */
public class Painter extends Activity {

	public static final int BACKUP_OPENED_ONLY_FROM_OTHER = 10;
	public static final int BACKUP_OPENED_ALWAYS = 20;
	public static final int BACKUP_OPENED_NEVER = 100;

	public static final int BEFORE_EXIT_SUBMIT = 10;
	public static final int BEFORE_EXIT_SAVE = 20;
	public static final int BEFORE_EXIT_NO_ACTION = 100;

	public static final int SHORTCUTS_VOLUME_BRUSH_SIZE = 10;
	public static final int SHORTCUTS_VOLUME_UNDO_REDO = 20;

	public static final int ACTION_SAVE_AND_EXIT = 1;
	public static final int ACTION_SAVE_AND_RETURN = 2;
	public static final int ACTION_SAVE_AND_SHARE = 3;
	public static final int ACTION_SAVE_AND_ROTATE = 4;
	public static final int ACTION_SAVE_AND_OPEN = 5;

	public static final int REQUEST_OPEN = 1;

	private static final String SETTINGS_STORAGE = "settings";

	public static final String PICTURE_MIME = "image/png";
	public static final String PICTURE_PREFIX = "picture_";
	public static final String PICTURE_EXT = ".png";

	private PainterCanvas mCanvas;
	private SeekBar mBrushSize;
	private SeekBar mBrushBlurRadius;
	private Spinner mBrushBlurStyle;
	private LinearLayout mPresetsBar;
	private LinearLayout mPropertiesBar;
	private RelativeLayout mSettingsLayout;

	private PainterSettings mSettings;
	private boolean mIsNewFile = true;

	private boolean mOpenLastFile = true;

	private int mVolumeButtonsShortcuts;

	private class SaveTask extends AsyncTask<Void, Void, String> {
		private ProgressDialog dialog = ProgressDialog.show(Painter.this,
                getString(R.string.saving_title),
                getString(R.string.saving_to_sd), true);

		protected String doInBackground(Void... none) {
            mCanvas.getThread().freeze();
			String pictureName = getUniquePictureName(getSaveDir());
            saveBitmap(pictureName);
            mSettings.preset = mCanvas.getCurrentPreset();
            saveSettings();
			return pictureName;
		}

		protected void onPostExecute(String pictureName) {
			Uri uri = Uri.fromFile(new File(pictureName));
            sendBroadcast(new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

			dialog.hide();
            mCanvas.getThread().activate();
		}
	}

	private class SetWallpaperTask extends AsyncTask<Void, Void, Boolean> {

		private ProgressDialog mDialog = ProgressDialog.show(Painter.this,
                getString(R.string.wallpaper_title),
                getString(R.string.aply_wallpaper), true);

		protected Boolean doInBackground(Void... none) {
			WallpaperManager wallpaperManager = WallpaperManager
					.getInstance(Painter.this);
			Display display = getWindowManager().getDefaultDisplay();

			int wallpaperWidth = display.getWidth() * 2;
			int wallpaperHeight = display.getHeight();

			Bitmap currentBitmap = mCanvas.getThread().getBitmap();

			Bitmap wallpaperBitmap = Bitmap.createBitmap(wallpaperWidth,
					wallpaperHeight, Bitmap.Config.ARGB_8888);
			// wait bitmap
			while (wallpaperBitmap == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return false;
				}
			}

			Canvas wallpaperCanvas = new Canvas(wallpaperBitmap);

			wallpaperCanvas.drawColor(mCanvas.getThread()
					.getBackgroundColor());
			wallpaperCanvas.drawBitmap(currentBitmap,
					(wallpaperWidth - currentBitmap.getWidth()) / 2,
					(wallpaperHeight - currentBitmap.getHeight()) / 2, null);

			try {
				wallpaperManager.setBitmap(wallpaperBitmap);
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		protected void onPostExecute(Boolean success) {
			mDialog.hide();

			if (success) {
				Toast.makeText(Painter.this, R.string.wallpaper_setted,
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(Painter.this, R.string.wallpaper_error,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.main);
		mCanvas = (PainterCanvas) findViewById(R.id.canvas);

        loadSettings();

		mBrushSize = (SeekBar) findViewById(R.id.brush_size);
		mBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					public void onStopTrackingTouch(SeekBar seekBar) {
						if (seekBar.getProgress() > 0) {
                            mCanvas.setPresetSize(seekBar.getProgress());
						}
					}

					public void onStartTrackingTouch(SeekBar seekBar) {
                        resetPresets();
					}

					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (progress > 0) {
							if (fromUser) {
                                mCanvas.setPresetSize(seekBar.getProgress());
							}
						} else {
                            mBrushSize.setProgress(1);
						}
					}
				});

		mBrushBlurRadius = (SeekBar) findViewById(R.id.brush_blur_radius);
		mBrushBlurRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					public void onStopTrackingTouch(SeekBar seekBar) {
                        updateBlurSeek(seekBar.getProgress());

						if (seekBar.getProgress() > 0) {
                            setBlur();
						} else {
                            mCanvas.setPresetBlur(null, 0);
						}
					}

					public void onStartTrackingTouch(SeekBar seekBar) {
                        resetPresets();
					}

					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (fromUser) {
                            updateBlurSeek(progress);
							if (progress > 0) {
                                setBlur();
							} else {
                                mCanvas.setPresetBlur(null, 0);
							}
						}
					}
				});

		mBrushBlurStyle = (Spinner) findViewById(R.id.brush_blur_style);
		mBrushBlurStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent, View v,
							int position, long id) {
						if (id > 0) {
                            updateBlurSpinner(id);
                            setBlur();
						} else {
                            mBrushBlurRadius.setProgress(0);
                            mCanvas.setPresetBlur(null, 0);
						}
					}

					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		mPresetsBar = (LinearLayout) findViewById(R.id.presets_bar);
		mPresetsBar.setVisibility(View.INVISIBLE);

		mPropertiesBar = (LinearLayout) findViewById(R.id.properties_bar);
		mPropertiesBar.setVisibility(View.INVISIBLE);

		mSettingsLayout = (RelativeLayout) findViewById(R.id.settings_layout);

        updateControls();
        setActivePreset(mCanvas.getCurrentPreset().type);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		String lang = preferences.getString(
                getString(R.string.preferences_language), null);

		if (lang != null) {
			Locale locale = new Locale(lang);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, null);
		}

		mOpenLastFile = preferences.getBoolean(
                getString(R.string.preferences_last_file), true);

		mVolumeButtonsShortcuts = Integer.parseInt(preferences.getString(
                getString(R.string.preferences_volume_shortcuts),
				String.valueOf(SHORTCUTS_VOLUME_BRUSH_SIZE)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_brush:
            enterBrushSetup();
			break;
		case R.id.menu_save:
            savePicture(ACTION_SAVE_AND_RETURN);
			break;
		case R.id.menu_clear:
			if (mCanvas.isChanged()) {
                showDialog(R.id.dialog_clear);
			} else {
                clear();
			}
			break;
		case R.id.menu_share:
            share();
			break;
		case R.id.menu_rotate:
            rotate();
			break;
		case R.id.menu_open:
            open();
			break;
		case R.id.menu_undo:
			mCanvas.undo();
			break;
		case R.id.menu_preferences:
            showPreferences();
			break;
		case R.id.menu_set_wallpaper:
			new SetWallpaperTask().execute();
			break;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem undo = menu.findItem(R.id.menu_undo);
		if (mCanvas.canUndo()) {
			undo.setTitle(R.string.menu_undo);
			undo.setIcon(R.drawable.ic_menu_undo);
			undo.setEnabled(true);
		} else if (mCanvas.canRedo()) {
			undo.setTitle(R.string.menu_redo);
			undo.setIcon(R.drawable.ic_menu_redo);
			undo.setEnabled(true);
		} else {
			undo.setTitle(R.string.menu_undo);
			undo.setIcon(R.drawable.ic_menu_undo);
			undo.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mCanvas.isSetup()) {
                exitBrushSetup();
				return true;
			} else if (mCanvas.isChanged() ||
					(!mIsNewFile && !new File(mSettings.lastPicture).exists())) {

				mSettings.preset = mCanvas.getCurrentPreset();
                saveSettings();

				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(this);

				int beforeExit = Integer.parseInt(preferences.getString(
                        getString(R.string.preferences_before_exit),
						String.valueOf(BEFORE_EXIT_SUBMIT)));

				if (mCanvas.isChanged()
						&& beforeExit == BEFORE_EXIT_SUBMIT) {
                    showDialog(R.id.dialog_exit);
				} else if (beforeExit == BEFORE_EXIT_SAVE) {
                    savePicture(ACTION_SAVE_AND_EXIT);
				} else {
					return super.onKeyDown(keyCode, event);
				}
				return true;
			}
			break;

		case KeyEvent.KEYCODE_MENU:
			if (mCanvas.isSetup()) {
				return true;
			}
			break;

		case KeyEvent.KEYCODE_VOLUME_UP:
			switch (mVolumeButtonsShortcuts) {
			case SHORTCUTS_VOLUME_BRUSH_SIZE:
				mCanvas
						.setPresetSize(mCanvas.getCurrentPreset().size + 1);
				if (mCanvas.isSetup()) {
                    updateControls();
				}
				break;

			case SHORTCUTS_VOLUME_UNDO_REDO:
				if (!mCanvas.isSetup()) {
					if (mCanvas.canRedo()) {
						mCanvas.undo();
					}
				}
				break;
			}

			return true;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			switch (mVolumeButtonsShortcuts) {
			case SHORTCUTS_VOLUME_BRUSH_SIZE:
				mCanvas.setPresetSize(mCanvas.getCurrentPreset().size - 1);
				if (mCanvas.isSetup()) {
                    updateControls();
				}
				break;

			case SHORTCUTS_VOLUME_UNDO_REDO:
				if (!mCanvas.isSetup()) {
					if (mCanvas.canUndo()) {
						mCanvas.undo();
					}
				}
				break;
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_clear:
			return createDialogClear();

		case R.id.dialog_exit:
			return createDialogExit();

		case R.id.dialog_share:
			return createDialogShare();

		case R.id.dialog_open:
			return createDialogOpen();

		default:
			return super.onCreateDialog(id);

		}
	}

	@Override
	protected void onStop() {
		mSettings.preset = mCanvas.getCurrentPreset();
        saveSettings();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case REQUEST_OPEN:
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = intent.getData();
				String path = "";

				if (uri != null) {
					if (uri.toString().toLowerCase().startsWith("content://")) {
						path = "file://" + getRealPathFromURI(uri);
					} else {
						path = uri.toString();
					}
					URI file_uri = URI.create(path);

					if (file_uri != null) {
						File picture = new File(file_uri);

						if (picture.exists()) {
							Bitmap bitmap = null;

							try {
								bitmap = BitmapFactory.decodeFile(picture
										.getAbsolutePath());

								Config bitmapConfig = bitmap.getConfig();
								if (bitmapConfig != Config.ARGB_8888) {
									bitmap = null;
								}
							} catch (Exception e) {
							}

							if (bitmap != null) {
								if (bitmap.getWidth() > bitmap.getHeight()) {
									mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
								} else if (bitmap.getWidth() != bitmap
										.getHeight()) {
									mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
								} else {
									mSettings.orientation = getRequestedOrientation();
								}

								SharedPreferences preferences = PreferenceManager
										.getDefaultSharedPreferences(this);

								int backupOption = Integer
										.parseInt(preferences.getString(
                                                getString(R.string.preferences_backup_openeded_file),
												String.valueOf(BACKUP_OPENED_ONLY_FROM_OTHER)));

								String pictureName = null;

								switch (backupOption) {
								case BACKUP_OPENED_ONLY_FROM_OTHER:
									if (!picture
											.getParentFile()
											.getName()
											.equals(getString(R.string.app_name))) {
										pictureName = FileSystem.copyFile(
												picture.getAbsolutePath(),
                                                getSaveDir()
														+ picture.getName());
									} else {
										pictureName = picture.getAbsolutePath();
									}
									break;

								case BACKUP_OPENED_ALWAYS:
									pictureName = FileSystem.copyFile(
											picture.getAbsolutePath(),
                                            getSaveDir()
													+ picture.getName());
									break;

								case BACKUP_OPENED_NEVER:
									pictureName = picture
											.getAbsolutePath();
									break;
								}

								if (pictureName != null) {
									mSettings.lastPicture = pictureName;

                                    saveSettings();
                                    restart();
								} else {
									Toast.makeText(this,
											R.string.file_not_found,
											Toast.LENGTH_SHORT).show();
								}
							} else {
								Toast.makeText(this, R.string.invalid_file,
										Toast.LENGTH_SHORT).show();
							}
						} else {
							Toast.makeText(this, R.string.file_not_found,
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
			break;
		}
	}

	public void changeBrushColor(View v) {
		new ColorPickerDialog(this,
				new ColorPickerDialog.OnColorChangedListener() {
					public void colorChanged(int color) {
                        mCanvas.setPresetColor(color);
					}
				}, mCanvas.getCurrentPreset().color).show();
	}

	public Bitmap getLastPicture() {
		Bitmap savedBitmap = null;

		if (!mOpenLastFile && mSettings.forceOpenFile) {
			mSettings.lastPicture = null;
			mIsNewFile = true;
			return savedBitmap;
		}

		mSettings.forceOpenFile = false;

		if (mSettings.lastPicture != null) {
			if (new File(mSettings.lastPicture).exists()) {
				savedBitmap = BitmapFactory
						.decodeFile(mSettings.lastPicture);
				mIsNewFile = false;
			} else {
				mSettings.lastPicture = null;
			}
		}

		return savedBitmap;
	}

	public void setPreset(View v) {
		switch (v.getId()) {
		case R.id.preset_pencil:
			mCanvas.setPreset(new BrushPreset(BrushPreset.PENCIL,
					mCanvas.getCurrentPreset().color));
			break;
		case R.id.preset_brush:
			mCanvas.setPreset(new BrushPreset(BrushPreset.BRUSH,
					mCanvas.getCurrentPreset().color));
			break;
		case R.id.preset_marker:
			mCanvas.setPreset(new BrushPreset(BrushPreset.MARKER,
					mCanvas.getCurrentPreset().color));
			break;
		case R.id.preset_pen:
			mCanvas.setPreset(new BrushPreset(BrushPreset.PEN,
					mCanvas.getCurrentPreset().color));
			break;
		}

        resetPresets();
        setActivePreset(v);
        updateControls();
	}

	public void resetPresets() {
		LinearLayout wrapper = (LinearLayout) mPresetsBar.getChildAt(0);
		for (int i = wrapper.getChildCount() - 1; i >= 0; i--) {
			wrapper.getChildAt(i).setBackgroundColor(Color.WHITE);
		}
	}

	public void savePicture(int action) {
		if (!isStorageAvailable()) {
			return;
		}

		final int taskAction = action;

		new SaveTask() {
			protected void onPostExecute(String pictureName) {
                mIsNewFile = false;

				if (taskAction == Painter.ACTION_SAVE_AND_SHARE) {
                    startShareActivity(pictureName);
				}

				if (taskAction == Painter.ACTION_SAVE_AND_OPEN) {
                    startOpenActivity();
				}

				super.onPostExecute(pictureName);

				if (taskAction == Painter.ACTION_SAVE_AND_EXIT) {
                    finish();
				}
				if (taskAction == Painter.ACTION_SAVE_AND_ROTATE) {
                    rotateScreen();
				}
			}
		}.execute();
	}

	private void enterBrushSetup() {
		mSettingsLayout.setVisibility(View.VISIBLE);
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
                mCanvas.setVisibility(View.INVISIBLE);
                setPanelVerticalSlide(mPresetsBar, -1.0f, 0.0f,
						300);
                setPanelVerticalSlide(mPropertiesBar, 1.0f, 0.0f,
						300, true);
                mCanvas.setup(true);
			}
		}, 10);
	}

	private void exitBrushSetup() {
		mSettingsLayout.setBackgroundColor(Color.WHITE);
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
                mCanvas.setVisibility(View.INVISIBLE);
                setPanelVerticalSlide(mPresetsBar, 0.0f, -1.0f,
						300);
                setPanelVerticalSlide(mPropertiesBar, 0.0f, 1.0f,
						300, true);
                mCanvas.setup(false);
			}
		}, 10);
	}

	private Dialog createDialogClear() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.clear_bitmap_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.clear_bitmap_prompt_title);

		alert.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        clear();
					}
				});
		alert.setNegativeButton(R.string.no, null);
		return alert.create();
	}

	private Dialog createDialogExit() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.exit_app_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.exit_app_prompt_title);

		alert.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        savePicture(Painter.ACTION_SAVE_AND_EXIT);
					}
				});
		alert.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        finish();
					}
				});

		return alert.create();
	}

	private Dialog createDialogOpen() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.open_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.open_prompt_title);

		alert.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        savePicture(Painter.ACTION_SAVE_AND_OPEN);
					}
				});
		alert.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        startOpenActivity();
					}
				});

		return alert.create();
	}

	private Dialog createDialogShare() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.share_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.share_prompt_title);

		alert.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        savePicture(Painter.ACTION_SAVE_AND_SHARE);
					}
				});
		alert.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
                        startShareActivity(mSettings.lastPicture);
					}
				});

		return alert.create();
	}

	private void updateControls() {
		mBrushSize.setProgress((int) mCanvas.getCurrentPreset().size);
		if (mCanvas.getCurrentPreset().blurStyle != null) {
			mBrushBlurStyle
					.setSelection(mCanvas.getCurrentPreset().blurStyle
							.ordinal() + 1);
			mBrushBlurRadius
					.setProgress(mCanvas.getCurrentPreset().blurRadius);
		} else {
			mBrushBlurStyle.setSelection(0);
		mBrushBlurRadius.setProgress(0);
		}
	}

	private void setPanelVerticalSlide(LinearLayout layout, float from,
			float to, int duration) {
        setPanelVerticalSlide(layout, from, to, duration, false);
	}

	private void setPanelVerticalSlide(LinearLayout layout, float from,
			float to, int duration, boolean last) {
		Animation animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, from,
				Animation.RELATIVE_TO_SELF, to);

		animation.setDuration(duration);
		animation.setFillAfter(true);
		animation.setInterpolator(this, android.R.anim.decelerate_interpolator);

		final float listenerFrom = Math.abs(from);
		final float listenerTo = Math.abs(to);
		final boolean listenerLast = last;
		final View listenerLayout = layout;

		if (listenerFrom > listenerTo) {
			listenerLayout.setVisibility(View.VISIBLE);
		}

		animation.setAnimationListener(new Animation.AnimationListener() {

			public void onAnimationStart(Animation animation) {
			}

			public void onAnimationRepeat(Animation animation) {
			}

			public void onAnimationEnd(Animation animation) {
				if (listenerFrom < listenerTo) {
					listenerLayout.setVisibility(View.INVISIBLE);
					if (listenerLast) {
                        mCanvas.setVisibility(View.VISIBLE);
						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							public void run() {
                                mSettingsLayout
										.setVisibility(View.GONE);
							}
						}, 10);
					}
				} else {
					if (listenerLast) {
                        mCanvas.setVisibility(View.VISIBLE);
						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							public void run() {
                                mSettingsLayout
										.setBackgroundColor(Color.TRANSPARENT);
							}
						}, 10);
					}
				}
			}
		});

		layout.setAnimation(animation);
	}

	private void share() {
		if (!isStorageAvailable()) {
			return;
		}

		if (mCanvas.isChanged() || mIsNewFile) {
			if (mIsNewFile) {
                savePicture(ACTION_SAVE_AND_SHARE);
			} else {
                showDialog(R.id.dialog_share);
			}
		} else {
            startShareActivity(mSettings.lastPicture);
		}
	}

	private void startShareActivity(String pictureName) {
		Uri uri = Uri.fromFile(new File(pictureName));

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType(PICTURE_MIME);
		i.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(i,
				getString(R.string.share_image_title)));
	}

	private void updateBlurSpinner(long blur_style) {
		if (blur_style > 0 && mBrushBlurRadius.getProgress() < 1) {
			mBrushBlurRadius.setProgress(1);
		}
	}

	private void updateBlurSeek(int progress) {
		if (progress > 0) {
			if (mBrushBlurStyle.getSelectedItemId() < 1) {
				mBrushBlurStyle.setSelection(1);
			}
		} else {
			mBrushBlurStyle.setSelection(0);
		}
	}

	private void setBlur() {
		mCanvas.setPresetBlur(
				(int) mBrushBlurStyle.getSelectedItemId(),
				mBrushBlurRadius.getProgress());
	}

	private void setActivePreset(int preset) {
		if (preset > 0 && preset != BrushPreset.CUSTOM) {
			LinearLayout wrapper = (LinearLayout) mPresetsBar
					.getChildAt(0);
            highlightActivePreset(wrapper.getChildAt(preset - 1));
		}
	}

	private void setActivePreset(View v) {
        highlightActivePreset(v);
	}

	private void highlightActivePreset(View v) {
		v.setBackgroundColor(0xe5e5e5);
	}

	private void clear() {
		mCanvas.getThread().clearBitmap();
		mCanvas.changed(false);
        clearSettings();
		mIsNewFile = true;
        updateControls();
	}

	private void rotate() {
		mSettings.forceOpenFile = true;

		if (!mIsNewFile || mCanvas.isChanged()) {
            savePicture(ACTION_SAVE_AND_ROTATE);
		} else {
            rotateScreen();
		}
	}

	private void rotateScreen() {
		if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            saveSettings();

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            saveSettings();

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	private boolean isStorageAvailable() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Toast.makeText(this, R.string.sd_card_not_writeable,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, R.string.sd_card_not_available,
					Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	private String getUniquePictureName(String path) {
		if (mSettings.lastPicture != null) {
			return mSettings.lastPicture;
		}

		String prefix = PICTURE_PREFIX;
		String ext = PICTURE_EXT;
		String pictureName = "";

		int suffix = 1;
		pictureName = path + prefix + suffix + ext;

		while (new File(pictureName).exists()) {
			pictureName = path + prefix + suffix + ext;
			suffix++;
		}

		mSettings.lastPicture = pictureName;
		return pictureName;
	}

	private void loadSettings() {
		mSettings = new PainterSettings();
		SharedPreferences settings = getSharedPreferences(
				SETTINGS_STORAGE, Context.MODE_PRIVATE);

		mSettings.orientation = settings.getInt(
                getString(R.string.settings_orientation),
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		if (getRequestedOrientation() != mSettings.orientation) {
            setRequestedOrientation(mSettings.orientation);
		}

		mSettings.lastPicture = settings.getString(
                getString(R.string.settings_last_picture), null);

		int type = settings.getInt(
                getString(R.string.settings_brush_type), BrushPreset.PEN);

		if (type == BrushPreset.CUSTOM) {
			mSettings.preset = new BrushPreset(
					settings.getFloat(
                            getString(R.string.settings_brush_size), 2),
					settings.getInt(
                            getString(R.string.settings_brush_color),
							Color.BLACK),
					settings.getInt(
                            getString(R.string.settings_brush_blur_style),
							0),
					settings.getInt(
                            getString(R.string.settings_brush_blur_radius),
							0));
			mSettings.preset.setType(type);
		} else {
			mSettings.preset = new BrushPreset(type, settings.getInt(
                    getString(R.string.settings_brush_color), Color.BLACK));
		}

		mCanvas.setPreset(mSettings.preset);

		mSettings.forceOpenFile = settings.getBoolean(
                getString(R.string.settings_force_open_file), false);
	}

	private String getSaveDir() {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ '/'
				+ getString(R.string.app_name)
				+ '/';

		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}

		return path;
	}

	private void saveBitmap(String pictureName) {
		try {
			mCanvas.saveBitmap(pictureName);
			mCanvas.changed(false);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void saveSettings() {
		SharedPreferences settings = getSharedPreferences(SETTINGS_STORAGE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		try {
			PackageInfo pack = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
			editor.putInt(getString(R.string.settings_version),
					pack.versionCode);
		} catch (NameNotFoundException e) {
		}

		editor.putInt(getString(R.string.settings_orientation),
				mSettings.orientation);
		editor.putString(getString(R.string.settings_last_picture),
				mSettings.lastPicture);
		editor.putFloat(getString(R.string.settings_brush_size),
				mSettings.preset.size);
		editor.putInt(getString(R.string.settings_brush_color),
				mSettings.preset.color);
		editor.putInt(
                getString(R.string.settings_brush_blur_style),
				(mSettings.preset.blurStyle != null) ? mSettings.preset.blurStyle
						.ordinal() + 1 : 0);
		editor.putInt(getString(R.string.settings_brush_blur_radius),
				mSettings.preset.blurRadius);
		editor.putInt(getString(R.string.settings_brush_type),
				mSettings.preset.type);
		editor.putBoolean(getString(R.string.settings_force_open_file),
				mSettings.forceOpenFile);

		editor.commit();
	}

	private void clearSettings() {
		mSettings.lastPicture = null;
        deleteFile(SETTINGS_STORAGE);
	}

	private void open() {
		if (!isStorageAvailable()) {
			return;
		}

		mSettings.forceOpenFile = true;

		if (mCanvas.isChanged()) {
            showDialog(R.id.dialog_open);
		} else {
            startOpenActivity();
		}
	}

	private void startOpenActivity() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setDataAndType(Uri.fromFile(new File(getSaveDir())), PICTURE_MIME);
        startActivityForResult(
				Intent.createChooser(intent,
                        getString(R.string.open_prompt_title)),
				REQUEST_OPEN);
	}

	private void restart() {
		Intent intent = getIntent();
        overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();

		return cursor.getString(column_index).replace(" ", "%20");
	}

	private void showPreferences() {
		Intent intent = new Intent();
		intent.setClass(this, PainterPreferences.class);
		startActivity(intent);
	}
}