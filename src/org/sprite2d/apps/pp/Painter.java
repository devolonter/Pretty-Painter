package org.sprite2d.apps.pp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.sprite2d.apps.pp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.Menu;
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
	private static final short MENU_BRUSH = 0x1000;
	private static final short MENU_CLEAR = 0x1001;
	private static final short MENU_SAVE = 0x1002;	
	private static final short MENU_SHARE = 0x1003;
	private static final short MENU_ROTATE = 0x1004;
	private static final short MENU_ABOUT = 0x1005;
	
	private static final int DIALOG_CLEAR = 1;
	private static final int DIALOG_EXIT = 2;
	private static final int DIALOG_SHARE = 3;
	private static final int DIALOG_ABOUT = 4;
	
	public static final int ACTION_SAVE_AND_EXIT = 1;
	public static final int ACTION_SAVE_AND_RETURN = 2;
	public static final int ACTION_SAVE_AND_SHARE = 3;
	public static final int ACTION_SAVE_AND_ROTATE = 4;
	
	private static final String SETTINGS_STRORAGE = "settings";
		
	private PainterCanvas mCanvas;
	private SeekBar mBrushSize;
	private SeekBar mBrushBlurRadius;
	private Spinner mBrushBlurStyle;
	private LinearLayout mPresetsBar;
	private LinearLayout mPropertiesBar;
	private RelativeLayout mSettingsLayout;
	
	private PainterSettings mSettings;
	private boolean mIsNewFile = true;
	
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
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(Menu.NONE, 
				Painter.MENU_BRUSH,
				Menu.NONE,
				R.string.brush)
			.setIcon(android.R.drawable.ic_menu_edit);		
		
		menu.add(Menu.NONE, 
				Painter.MENU_SAVE,
				Menu.NONE,
				R.string.save)
			.setIcon(android.R.drawable.ic_menu_save);
		
		menu.add(Menu.NONE, 
				Painter.MENU_CLEAR,
				Menu.NONE,
				R.string.clear)
			.setIcon(android.R.drawable.ic_menu_delete);
		
		menu.add(Menu.NONE, 
				Painter.MENU_SHARE,
				Menu.NONE,
				R.string.share)
			.setIcon(android.R.drawable.ic_menu_share);
		
		menu.add(Menu.NONE, 
				Painter.MENU_ROTATE,
				Menu.NONE,
				R.string.rotate)
			.setIcon(android.R.drawable.ic_menu_rotate);
		
		menu.add(Menu.NONE, 
				Painter.MENU_ABOUT,
				Menu.NONE,
				R.string.about)
			.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
    }	
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case Painter.MENU_CLEAR:
    			if(this.mCanvas.isChanged()) {
    				this.showDialog(Painter.DIALOG_CLEAR);    
    			}
    			else {
    				this.clear();
    			}
    			break;
    		case Painter.MENU_BRUSH:   			
    			this.enterBrushSetup(); 
    		    break;    		
    		case Painter.MENU_SAVE: 
    			this.savePicture(Painter.ACTION_SAVE_AND_RETURN);
    			break;    		
    		case Painter.MENU_SHARE: 
    			this.share();
    			break;
    		case Painter.MENU_ROTATE: 
    			this.rotate();
    			break; 
    		case Painter.MENU_ABOUT: 
    			this.showDialog(Painter.DIALOG_ABOUT);
    			break;   
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
		}
		
		return null;
	}
	
	@Override
	protected void onStop() {
		this.mSettings.preset = this.mCanvas.getCurrentPreset();
		this.saveSettings();
		super.onStop();
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
		alert.setMessage(R.string.clear_bitmap_alert);
		alert.setCancelable(false);
		alert.setTitle(R.string.clear_app_prompt_title);
		
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
    
    private Dialog createDialogShare() {
    	AlertDialog.Builder alert =  new AlertDialog.Builder(this);
		alert.setMessage(R.string.share_prompt);
		alert.setCancelable(false);
		alert.setTitle(R.string.share_app_prompt_title);
		
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
					        	 Painter.this.mSettingsLayout.setVisibility(View.INVISIBLE);
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
		i.setType("image/png");
		i.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(i,
				getString(R.string.share_image)));
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
    	Blur blur;
    	
		switch((int) this.mBrushBlurStyle.getSelectedItemId()) {
			case 1: 
				blur = Blur.NORMAL;
				break;
			case 2: 				
				blur = Blur.SOLID;
				break;			
			case 3: 
				blur = Blur.OUTER;
				break;
			case 4: 				
				blur = Blur.INNER;
				break;
			default: 
				blur = Blur.NORMAL;				
				break;
		}
		
		this.mCanvas.setPresetBlur(blur, mBrushBlurRadius.getProgress());
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
    	
		String prefix = "picture_";
		String ext = ".png";
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
    	PainterSettings settings = new PainterSettings();
    	
    	try {
    		FileInputStream fis = this.openFileInput(
    				Painter.SETTINGS_STRORAGE
    		);
    		if(fis != null) {    			
    			try {   
    				ObjectInputStream oin = new ObjectInputStream(fis);
    				
    				try { 
    					settings = (PainterSettings) oin.readObject();
    				} catch(ClassNotFoundException e) {}
    				
    				if(settings != null) {    
						if(this.getRequestedOrientation() != settings.orientation) {
	    					this.setRequestedOrientation(settings.orientation);		    					
    					}		    				
    					if(settings.preset != null) {
    						this.mCanvas.setPreset(settings.preset);
    					}
    				}
    				else {
    					settings = new PainterSettings();
    					settings.preset = new BrushPreset(BrushPreset.PEN, Color.BLACK);
    					this.mCanvas.setPreset(settings.preset);    					
    				}
    			} catch(IOException e){}
    		}
    	} catch (FileNotFoundException e) {}
    	
    	this.mSettings = settings;
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
		try {
			FileOutputStream fos = openFileOutput(
					Painter.SETTINGS_STRORAGE, 
					Context.MODE_PRIVATE
			);			
			try {
				ObjectOutputStream oos = new ObjectOutputStream(fos);				
				oos.writeObject(this.mSettings);
				oos.close();
			} catch(IOException e) {}			
		} catch(FileNotFoundException e) {}			
    }
    
    
    private void clearSettings() {
    	this.mSettings.lastPicture = null;
    	this.deleteFile(Painter.SETTINGS_STRORAGE);		
    }
}