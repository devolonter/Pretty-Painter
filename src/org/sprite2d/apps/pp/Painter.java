package org.sprite2d.apps.pp;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Locale;

import org.sprite2d.apps.pp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.util.Linkify;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * Base application logic
 * 
 * @author Artut Bikmullin (devolonter)
 * @version 1.0 
 *
 */
public class Painter extends Activity {	
	private static final int DIALOG_CLEAR = 1;
	private static final int DIALOG_EXIT = 2;
	private static final int DIALOG_SHARE = 3;	
	private static final int DIALOG_ABOUT = 4;
	private static final int DIALOG_OPEN = 5;
	
	public static final int ACTION_SAVE_AND_EXIT = 1;
	public static final int ACTION_SAVE_AND_RETURN = 2;
	public static final int ACTION_SAVE_AND_SHARE = 3;
	public static final int ACTION_SAVE_AND_ROTATE = 4;
	public static final int ACTION_SAVE_AND_OPEN = 5;
	
	public static final int REQUEST_OPEN = 1;
	
	private static final String SETTINGS_STRORAGE = "settings";
	
	private static final String PICTURE_MIME = "image/png";
	private static final String PICTURE_PREFIX = "picture_";
	private static final String PICTURE_EXT = ".png";
		
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
	
	private class SaveTask extends AsyncTask<Void, Void, String> {
		private ProgressDialog dialog = ProgressDialog.show(
				Painter.this, 
				Painter.this.getString(R.string.saving_title),
				Painter.this.getString(R.string.saving_to_sd), 
				true
		);

		protected String doInBackground(Void... none) {
			Painter.this.mCanvas.getThread().freeze();
			String pictureName = Painter.this.getUniquePictureName(
					Painter.this.getSaveDir()
			);
			Painter.this.saveBitmap(pictureName);
			Painter.this.mSettings.preset = Painter.this.mCanvas.getCurrentPreset();
			Painter.this.saveSettings();				
			return pictureName;
		}

		protected void onPostExecute(String pictureName) {
			Uri uri = Uri.fromFile(new File(pictureName));
			Painter.this.sendBroadcast(new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
			);

			dialog.hide();
			Painter.this.mCanvas.getThread().activate();
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);          
        
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);        
        this.setContentView(R.layout.main);
        
        this.mCanvas = (PainterCanvas) this.findViewById(R.id.canvas); 
        
        this.loadSettings();
        
    	this.mBrushSize = (SeekBar) this.findViewById(R.id.brush_size);
    	this.mBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(seekBar.getProgress() > 0) {
					Painter.this.mCanvas.setPresetSize(seekBar.getProgress());
				}				
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
				Painter.this.resetPresets();				
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(progress > 0) {
					if(fromUser){					
						Painter.this.mCanvas.setPresetSize(seekBar.getProgress());
					}
				}
				else {
					Painter.this.mBrushSize.setProgress(1);
				}
			}
		});
    	
    	this.mBrushBlurRadius = (SeekBar) this.findViewById(R.id.brush_blur_radius);
    	this.mBrushBlurRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
				Painter.this.updateBlurSeek(seekBar.getProgress());
				
				if(seekBar.getProgress() > 0) {
					Painter.this.setBlur();
				}
				else {
					Painter.this.mCanvas.setPresetBlur(null, 0);
				}
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {	
				Painter.this.resetPresets();					
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser) {	
					Painter.this.updateBlurSeek(progress);
					if(progress > 0) {	
						Painter.this.setBlur();
					}
					else {
						Painter.this.mCanvas.setPresetBlur(null, 0);
					}
				}								
			}
		});
    	
    	this.mBrushBlurStyle = (Spinner) this.findViewById(R.id.brush_blur_style);
    	this.mBrushBlurStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		
    		public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {    			
    			if(id > 0) {
    				Painter.this.updateBlurSpinner(id);
    				Painter.this.setBlur();
    			}
    			else {
    				Painter.this.mBrushBlurRadius.setProgress(0);
    				Painter.this.mCanvas.setPresetBlur(null, 0);
    			} 
    		}
    		
    		public void onNothingSelected(AdapterView<?> parent) {}
		});    	
   	
    	this.mPresetsBar = (LinearLayout) this.findViewById(R.id.presets_bar);  
    	this.mPresetsBar.setVisibility(View.INVISIBLE);    	
    	
    	this.mPropertiesBar = (LinearLayout) this.findViewById(R.id.properties_bar);  
    	this.mPropertiesBar.setVisibility(View.INVISIBLE);
    	
    	this.mSettingsLayout = (RelativeLayout) this.findViewById(R.id.settings_layout);    	
    	
        this.updateControls();
        this.setActivePreset(this.mCanvas.getCurrentPreset().type);
    }	
	
	@Override 
	protected void onResume() {
		super.onResume();
		SharedPreferences preferences = 
			PreferenceManager.getDefaultSharedPreferences(this);
		
		String lang = preferences.getString(
				this.getString(R.string.preferences_language), 
				null);
		
		if(lang != null) {
			Locale locale = new Locale(lang);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			this.getBaseContext().getResources().updateConfiguration(config, null);
		}
		
		this.mOpenLastFile = preferences.getBoolean(
				this.getString(R.string.preferences_last_file), 
				true);
	}
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
    }	
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {    		
    		case R.id.menu_brush:   			
    			this.enterBrushSetup(); 
    		    break;    		
    		case R.id.menu_save: 
    			this.savePicture(Painter.ACTION_SAVE_AND_RETURN);
    			break; 
    		case R.id.menu_clear:
    			if(this.mCanvas.isChanged()) {
    				this.showDialog(Painter.DIALOG_CLEAR);    
    			}
    			else {
    				this.clear();
    			}
    			break;
    		case R.id.menu_share: 
    			this.share();
    			break;
    		case R.id.menu_rotate: 
    			this.rotate();
    			break; 
    		case R.id.menu_about: 
    			this.showDialog(Painter.DIALOG_ABOUT);
    			break; 
    		case R.id.menu_open:
    			this.open();
    			break;
    		case R.id.menu_undo:
    			this.mCanvas.undo();
    			break;
    		case R.id.menu_preferences:
    			this.showPreferences();
    			break;
    	}
    	return true; 
    } 
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	MenuItem undo = menu.findItem(R.id.menu_undo);
    	if(this.mCanvas.canUndo()) {
    		undo.setTitle(R.string.menu_undo);
    		undo.setEnabled(true);
    	}
    	else if(this.mCanvas.canRedo()) {
    		undo.setTitle(R.string.menu_redo);
    		undo.setEnabled(true);
    	}
    	else {
    		undo.setTitle(R.string.menu_undo);
    		undo.setEnabled(false);
    	}    	
    	
    	return true;
    }
    
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	switch(keyCode) {
    		case KeyEvent.KEYCODE_BACK: 
    			if(this.mCanvas.isSetup()){
	    			this.exitBrushSetup();
	    		    return true;
    			}
    			else if(this.mCanvas.isChanged() || 
    					(!this.mIsNewFile && !new File(this.mSettings.lastPicture).exists())){
    				
    				this.mSettings.preset = this.mCanvas.getCurrentPreset();
    				this.saveSettings();
    				if(this.mCanvas.isChanged() && !this.mIsNewFile) {
    					this.showDialog(Painter.DIALOG_EXIT);    											
    				}
    				else {
    					this.savePicture(Painter.ACTION_SAVE_AND_EXIT);
    				}    				
    				return true;
    			}
    		    break;
    		case KeyEvent.KEYCODE_MENU: 
    			if(this.mCanvas.isSetup()){
    				return true;
    			}
    			break;
    		case KeyEvent.KEYCODE_VOLUME_UP: 
    			this.mCanvas.setPresetSize(this.mCanvas.getCurrentPreset().size+1);
    			if(this.mCanvas.isSetup()){
    				this.updateControls();
    			}
    			return true;
    		case KeyEvent.KEYCODE_VOLUME_DOWN: 
    			this.mCanvas.setPresetSize(this.mCanvas.getCurrentPreset().size-1);
    			if(this.mCanvas.isSetup()){
    				this.updateControls();
    			}
    			return true;
    	}
        return super.onKeyDown(keyCode, event);
    }
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case Painter.DIALOG_CLEAR: 				
				return this.createDialogClear();				
			case Painter.DIALOG_EXIT: 
				return this.createDialogExit();
			case Painter.DIALOG_SHARE: 
				return this.createDialogShare();
			case Painter.DIALOG_ABOUT: 
				return this.createDialogAbout();
			case Painter.DIALOG_OPEN: 
				return this.createDialogOpen();
		}
		
		return null;
	}
	
	@Override
	protected void onStop() {
		this.mSettings.preset = this.mCanvas.getCurrentPreset();
		this.saveSettings();
		super.onStop();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		switch(requestCode) {
			case Painter.REQUEST_OPEN:
				if (resultCode == Activity.RESULT_OK){
					Uri uri = intent.getData();
					String path = "";
					
				    if (uri != null) {
				    	if(uri.toString().toLowerCase().startsWith("content://")) {
				    		path = "file://"+this.getRealPathFromURI(uri);
				    	}
				    	else {
				    		path = uri.toString().toLowerCase();
				    	}
						URI file_uri = URI.create(path);
						
						if(file_uri != null) {
							File picture = new File(file_uri);
							
							if(picture.exists()){
								Bitmap bitmap = null;								
						
								try {
									bitmap = BitmapFactory.decodeFile(
											picture.getAbsolutePath()
									);	
									
									Config bitmapConfig = bitmap.getConfig();
									if(!bitmapConfig.equals(Bitmap.Config.ARGB_8888)) {
										bitmap = null;
									}
								} catch(Exception e) {}										
								
								if(bitmap != null) {
									if(bitmap.getWidth() > bitmap.getHeight()) {
										this.mSettings.orientation = 
											ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
									}
									else {
										this.mSettings.orientation = 
											ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
									}									
									this.mSettings.lastPicture = 
											picture.getAbsolutePath();		
									
									this.saveSettings();									
									this.restart();	
								}
								else {
									Toast.makeText(this, R.string.invalid_file, 
						    	    		Toast.LENGTH_SHORT).show();
								}
							}
							else {
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
    	new ColorPickerDialog(
    			this, 
    			new ColorPickerDialog.OnColorChangedListener() {					
					public void colorChanged(int color) {
						Painter.this.mCanvas.setPresetColor(color);
					}
				}, 
				Painter.this.mCanvas.getCurrentPreset().color
    	).show();
    } 
    
    public Bitmap getLastPicture() {
    	Bitmap savedBitmap = null;
    	
    	if(!this.mOpenLastFile && !this.mSettings.forceOpenFile) {
    		this.mSettings.lastPicture = null;
    		this.mIsNewFile = true;
    		return savedBitmap;
    	}
    	
    	this.mSettings.forceOpenFile = false;
    	
    	if(this.mSettings.lastPicture != null) {
    		if(new File(this.mSettings.lastPicture).exists()){
				savedBitmap = BitmapFactory.decodeFile(this.mSettings.lastPicture);
				this.mIsNewFile = false;
			}
    		else {
    			this.mSettings.lastPicture = null;    			
    		}
    	}
    	
    	return savedBitmap;
    }
    
    public void setPreset(View v) {
    	switch(v.getId()){
    		case R.id.preset_pencil:
    			this.mCanvas.setPreset(
    					new BrushPreset(
    							BrushPreset.PENCIL, 
    							this.mCanvas.getCurrentPreset().color
    					)
    			);
    			break;
    		case R.id.preset_brush: 
    			this.mCanvas.setPreset(
    					new BrushPreset(
    							BrushPreset.BRUSH, 
    							this.mCanvas.getCurrentPreset().color
    					)
    			);
    			break;
    		case R.id.preset_marker: 
    			this.mCanvas.setPreset(
    					new BrushPreset(
    							BrushPreset.MARKER, 
    							this.mCanvas.getCurrentPreset().color
    					)
    			);
    			break;
    		case R.id.preset_pen: 
    			this.mCanvas.setPreset(
    					new BrushPreset(
    							BrushPreset.PEN, 
    							this.mCanvas.getCurrentPreset().color
    					)
    			);
    			break;
    	}    	
    	
    	this.resetPresets();
    	this.setActivePreset(v);
    	this.updateControls();
    }
    
    public void resetPresets(){
    	LinearLayout wrapper = (LinearLayout) this.mPresetsBar.getChildAt(0);
    	for(int i = wrapper.getChildCount() - 1; i >= 0; i--){
    		wrapper.getChildAt(i).setBackgroundColor(Color.WHITE);
    	}
    }
    
    public void savePicture(int action) {
		if (!this.isStorageAvailable()) {
			return;
		}
		
		final int taskAction = action;
		
		new SaveTask() {
			protected void onPostExecute(String pictureName) {	
				Painter.this.mIsNewFile = false;
				
				if(taskAction == Painter.ACTION_SAVE_AND_SHARE) {
					Painter.this.startShareActivity(pictureName);
				}
				
				if(taskAction == Painter.ACTION_SAVE_AND_OPEN) {
					Painter.this.startOpenActivity();
				}
				
				super.onPostExecute(pictureName);
				
				if(taskAction == Painter.ACTION_SAVE_AND_EXIT)	{
					Painter.this.finish();
				}
				if(taskAction == Painter.ACTION_SAVE_AND_ROTATE) {
					Painter.this.rotateScreen();
				}
			}
		}.execute();
	}
    
    private void enterBrushSetup(){
    	this.mSettingsLayout.setVisibility(View.VISIBLE);     			
		Handler handler = new Handler(); 
	    handler.postDelayed(new Runnable() { 
	         public void run() { 
	        	 Painter.this.mCanvas.setVisibility(View.INVISIBLE);
	        	 Painter.this.setPanelVerticalSlide(mPresetsBar, -1.0f,
	        			 0.0f, 300);
	        	 Painter.this.setPanelVerticalSlide(mPropertiesBar, 1.0f,
	        			 0.0f, 300, true);   
	        	 Painter.this.mCanvas.setup(true);
	         } 
	    }, 10); 
    }
    
    private void exitBrushSetup(){
    	this.mSettingsLayout.setBackgroundColor(Color.WHITE); 
		Handler handler = new Handler(); 
	    handler.postDelayed(new Runnable() { 
	         public void run() {    
	        	 Painter.this.mCanvas.setVisibility(View.INVISIBLE);   
	        	 Painter.this.setPanelVerticalSlide(mPresetsBar, 0.0f,
	        			 -1.0f, 300);
	        	 Painter.this.setPanelVerticalSlide(mPropertiesBar, 0.0f,
	        			 1.0f, 300, true);
	        	 Painter.this.mCanvas.setup(false);
	         } 
	    }, 10);
    }
    
    private Dialog createDialogClear() {
    	AlertDialog.Builder alert =  new AlertDialog.Builder(this);
		alert.setMessage(R.string.clear_bitmap_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.clear_bitmap_prompt_title);
		
		alert.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
			
					public void onClick(DialogInterface dialog, int which) {
						Painter.this.clear();	
					}
		});
		alert.setNegativeButton(R.string.no, null);
		return alert.create();
    }
    
    private Dialog createDialogExit() {
    	AlertDialog.Builder alert =  new AlertDialog.Builder(this);
		alert.setMessage(R.string.exit_app_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.exit_app_prompt_title);
		
		alert.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
			
					public void onClick(DialogInterface dialog, int which) {						
						Painter.this.savePicture(Painter.ACTION_SAVE_AND_EXIT);
					}
		});
		alert.setNegativeButton(R.string.no, 
				new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Painter.this.finish();								
					}
				});
		
		return alert.create();
    }
    
    private Dialog createDialogOpen() {    	
    	AlertDialog.Builder alert =  new AlertDialog.Builder(this);
		alert.setMessage(R.string.open_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.open_prompt_title);
		
		alert.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
			
					public void onClick(DialogInterface dialog, int which) {						
						Painter.this.savePicture(Painter.ACTION_SAVE_AND_OPEN);
					}
		});
		alert.setNegativeButton(R.string.no, 
				new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Painter.this.startOpenActivity();
					}
				});
		
		return alert.create();
    }
    
    private Dialog createDialogShare() {    	
    	AlertDialog.Builder alert =  new AlertDialog.Builder(this);
		alert.setMessage(R.string.share_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.share_prompt_title);
		
		alert.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
			
					public void onClick(DialogInterface dialog, int which) {						
						Painter.this.savePicture(Painter.ACTION_SAVE_AND_SHARE);
					}
		});
		alert.setNegativeButton(R.string.no, 
				new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Painter.this.startShareActivity(
								Painter.this.mSettings.lastPicture
						);							
					}
				});
		
		return alert.create();
    }
    
    private Dialog createDialogAbout() {
    	final TextView message = new TextView(this);    
		
		message.setAutoLinkMask(Linkify.ALL);
		message.setText(R.string.about_text);		
		message.setPadding(20, 10, 20, 10);
		
	  	AlertDialog.Builder alert =  new AlertDialog.Builder(this);		
		alert.setCancelable(false);
		alert.setTitle(R.string.app_name);
		alert.setView(message);
		alert.setPositiveButton(android.R.string.ok, null);
		
		return alert.create();
    }
    
    private void updateControls() {
    	this.mBrushSize.setProgress((int) this.mCanvas.getCurrentPreset().size);    	
    	if(this.mCanvas.getCurrentPreset().blurStyle != null) {
    		this.mBrushBlurStyle.setSelection(
    				this.mCanvas.getCurrentPreset().blurStyle.ordinal()+1
			);
    		this.mBrushBlurRadius.setProgress(
    				this.mCanvas.getCurrentPreset().blurRadius
    		);
    	}
    	else {
    		this.mBrushBlurStyle.setSelection(0);
    		this.mBrushBlurRadius.setProgress(0);
    	}    	
    }
    
    private void setPanelVerticalSlide(LinearLayout layout, float from, float to, int duration) {
    	this.setPanelVerticalSlide(layout, from, to, duration, false);
    }
    
    private void setPanelVerticalSlide(LinearLayout layout, float from, float to, int duration, boolean last) { 
		Animation animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, from,
				Animation.RELATIVE_TO_SELF, to);
    	
		animation.setDuration(duration);
		animation.setFillAfter(true);
		animation.setInterpolator(this, android.R.anim.decelerate_interpolator);
				
		final float  listenerFrom = Math.abs(from);
		final float  listenerTo = Math.abs(to);
		final boolean  listenerLast = last;
		final View  listenerLayout = layout;
		
		if(listenerFrom > listenerTo) {
			listenerLayout.setVisibility(View.VISIBLE);
		}
		
		animation.setAnimationListener(new Animation.AnimationListener() {
			
			public void onAnimationStart(Animation animation) {	}
			
			public void onAnimationRepeat(Animation animation) {}
			
			public void onAnimationEnd(Animation animation) {
				if(listenerFrom < listenerTo) {
					listenerLayout.setVisibility(View.INVISIBLE);	
					if(listenerLast){
						Painter.this.mCanvas.setVisibility(View.VISIBLE);
						Handler handler = new Handler(); 
					    handler.postDelayed(new Runnable() { 
					         public void run() {    
					        	 Painter.this.mSettingsLayout.setVisibility(View.GONE);
					         } 
					    }, 10);						
					}
				}
				else {
					if(listenerLast){
						Painter.this.mCanvas.setVisibility(View.VISIBLE);
						Handler handler = new Handler(); 
					    handler.postDelayed(new Runnable() { 
					         public void run() {    
					        	 Painter.this.mSettingsLayout.setBackgroundColor(Color.TRANSPARENT);
					         } 
					    }, 10);							
					}
				}
			}
		});
		
		layout.setAnimation(animation); 
	}
                
    private void share() {
    	if (!this.isStorageAvailable()) {
			return;
		}
    	
    	if(this.mCanvas.isChanged() || this.mIsNewFile){ 
    		if(this.mIsNewFile){
    			this.savePicture(Painter.ACTION_SAVE_AND_SHARE);
    		}
    		else {
    			this.showDialog(Painter.DIALOG_SHARE);
    		}
    	}
    	else {
    		this.startShareActivity(this.mSettings.lastPicture);
    	}
    }
    
    private void startShareActivity(String pictureName) {
    	Uri uri = Uri.fromFile(new File(pictureName));

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType(Painter.PICTURE_MIME);
		i.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(i,
				getString(R.string.share_image_title)));
    }
    
    private void updateBlurSpinner(long blur_style) {    	
    	if(blur_style > 0 && this.mBrushBlurRadius.getProgress() < 1) {
    		this.mBrushBlurRadius.setProgress(1);
    	}
    }
    
    private void updateBlurSeek(int progress) {
    	if(progress > 0) {
    		if(this.mBrushBlurStyle.getSelectedItemId() < 1){
				this.mBrushBlurStyle.setSelection(1);
			}
    	}
    	else {
    		this.mBrushBlurStyle.setSelection(0);
    	}
    }
    
    private void setBlur() {		
		this.mCanvas.setPresetBlur(
				(int) this.mBrushBlurStyle.getSelectedItemId(), 
				mBrushBlurRadius.getProgress()
		);
    }      
    
    private void setActivePreset(int preset){
    	if(preset > 0 && preset != BrushPreset.CUSTOM){
    		LinearLayout wrapper = (LinearLayout) this.mPresetsBar.getChildAt(0);
    		this.highlightActivePreset(wrapper.getChildAt(preset - 1));
    	}
    }
    
    private void setActivePreset(View v){
    	this.highlightActivePreset(v);
    }
    
    private void highlightActivePreset(View v){
    	v.setBackgroundColor(0xe5e5e5);
    }
    
    private void clear() {
    	this.mCanvas.getThread().clearBitmap();	    	
		this.mCanvas.changed(false);
		this.clearSettings();
		this.mIsNewFile = true;
		this.updateControls();
    }
    
    private void rotate() {  
    	this.mSettings.forceOpenFile = true;
    	
    	if(!this.mIsNewFile || this.mCanvas.isChanged()){
			this.savePicture(Painter.ACTION_SAVE_AND_ROTATE);
		}
		else {
			this.rotateScreen();
		}
    }
    
    private void rotateScreen() {
    	if(this.getRequestedOrientation() == 
			ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
				this.mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				this.saveSettings();
				
				this.setRequestedOrientation(
						ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
				);
		}
		else {
			this.mSettings.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			this.saveSettings();
			
			this.setRequestedOrientation(
					ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			);
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
    	if(this.mSettings.lastPicture != null) {
    		return this.mSettings.lastPicture;
    	}
    	
		String prefix = Painter.PICTURE_PREFIX;
		String ext = Painter.PICTURE_EXT;
		String pictureName = "";

		int suffix = 1;
		pictureName = path + prefix + suffix + ext;
		
		while (new File(pictureName).exists()) {
			pictureName = path + prefix + suffix + ext;
			suffix++;
		}

		this.mSettings.lastPicture = pictureName;
		return pictureName;
	}
    
    private void loadSettings() {
    	this.mSettings = new PainterSettings();    	
    	SharedPreferences settings = this.getSharedPreferences(
    			Painter.SETTINGS_STRORAGE, 
    			Context.MODE_PRIVATE
    	);       	

    	this.mSettings.orientation = settings.getInt(
    			this.getString(R.string.settings_orientation), 
    			ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    	);
    	
    	if(this.getRequestedOrientation() != this.mSettings.orientation) {
			this.setRequestedOrientation(this.mSettings.orientation);		    					
		}
    	
    	this.mSettings.lastPicture = settings.getString(
    			this.getString(R.string.settings_last_picture), 
    			null
    	); 
    	
    	int type = settings.getInt(
    			this.getString(R.string.settings_brush_type), 
    			BrushPreset.PEN
    	);
    	
    	if(type == BrushPreset.CUSTOM) {
	    	this.mSettings.preset = new BrushPreset(
	    			settings.getFloat(
	    					this.getString(R.string.settings_brush_size), 
	    					2
	    			), 
	    			settings.getInt(
	    					this.getString(R.string.settings_brush_color), 
	    					Color.BLACK
	    			), 
	    			settings.getInt(
	    					this.getString(R.string.settings_brush_blur_style), 
	    					0
	    			), 
	    			settings.getInt(
	    					this.getString(R.string.settings_brush_blur_radius), 
	    					0
	    			)
	    	);    	
	    	this.mSettings.preset.setType(type);
    	}
    	else {
    		this.mSettings.preset = new BrushPreset(
    				type, 
    				settings.getInt(
    						this.getString(R.string.settings_brush_color), 
    						Color.BLACK
    				)
    		);
    	}
    	
    	this.mCanvas.setPreset(this.mSettings.preset);
    	
    	this.mSettings.forceOpenFile = settings.getBoolean(
    			this.getString(R.string.settings_force_open_file), 
    			false
    	);
    }    
    
    private String getSaveDir() {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/"+this.getString(R.string.app_name)+"/";

		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}

		return path;
	}
    
    private void saveBitmap(String pictureName) {
		try {
			this.mCanvas.saveBitmap(pictureName);
			this.mCanvas.changed(false);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
    
    private void saveSettings() {   
    	SharedPreferences settings = this.getSharedPreferences(
    			Painter.SETTINGS_STRORAGE, 
    			Context.MODE_PRIVATE
    	);
    	
    	SharedPreferences.Editor editor = settings.edit();
    	
    	try {
	    	PackageInfo pack = this.getPackageManager().getPackageInfo(
	    			this.getPackageName(), 
	    			0
	    	);
	    	editor.putInt(
	    			this.getString(R.string.settings_version), 
	    			pack.versionCode
	    	);
    	}
    	catch (NameNotFoundException e) {}
    	
    	editor.putInt(
    			this.getString(R.string.settings_orientation), 
    			this.mSettings.orientation
    	);
    	editor.putString(
    			this.getString(R.string.settings_last_picture), 
    			this.mSettings.lastPicture
    	);    	
    	editor.putFloat(
    			this.getString(R.string.settings_brush_size), 
    			this.mSettings.preset.size
    	);    	
    	editor.putInt(
    			this.getString(R.string.settings_brush_color),
    			this.mSettings.preset.color
    	);    	
    	editor.putInt(
    			this.getString(R.string.settings_brush_blur_style), 
    			(this.mSettings.preset.blurStyle != null) ? this.mSettings.preset.blurStyle.ordinal()+1 : 0
    	);
    	editor.putInt(
    			this.getString(R.string.settings_brush_blur_radius), 
    			this.mSettings.preset.blurRadius
    	);
    	editor.putInt(
    			this.getString(R.string.settings_brush_type), 
    			this.mSettings.preset.type
    	);  
    	editor.putBoolean(
    			this.getString(R.string.settings_force_open_file), 
    			this.mSettings.forceOpenFile
    	);

    	editor.commit();
    }
    
    
    private void clearSettings() {
    	this.mSettings.lastPicture = null;
    	this.deleteFile(Painter.SETTINGS_STRORAGE);		
    }
    
    private void open() {
    	if(!this.isStorageAvailable()){
    		return;
    	}
    	
    	this.mSettings.forceOpenFile = true;
    	
    	if(this.mCanvas.isChanged()){ 
    		this.showDialog(Painter.DIALOG_OPEN);
    	}
    	else {
    		this.startOpenActivity();
    	}    	
    }  
    
    private void startOpenActivity() {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    	intent.addCategory(Intent.CATEGORY_OPENABLE);
    	intent.setDataAndType(Uri.fromFile(new File(this.getSaveDir())), Painter.PICTURE_MIME);
    	this.startActivityForResult(Intent.createChooser(intent, 
    			this.getString(R.string.open_prompt_title)), 
    			Painter.REQUEST_OPEN
    	);
    }
    
    private void restart() {
    	Intent intent = this.getIntent();
        this.overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        this.finish();

        this.overridePendingTransition(0, 0);
        this.startActivity(intent);
    }
    
    public String getRealPathFromURI(Uri contentUri) {
		String [] proj={MediaStore.Images.Media.DATA};
		Cursor cursor = managedQuery( contentUri,
		                proj,
		                null, 
		                null, 
		                null); 
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		
		return cursor.getString(column_index).replace(" ", "%20");
    }
    
    private void showPreferences() {
    	Intent intent = new Intent();
    	intent.setClass(this, PainterPreferences.class);
    	startActivity(intent);
    }
}