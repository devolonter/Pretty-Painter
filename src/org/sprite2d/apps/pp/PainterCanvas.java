package org.sprite2d.apps.pp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Color;
import android.graphics.Matrix;

import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

/**
 * Draw surface class
 * 
 * @author Artut Bikmullin (devolonter)
 * @version 1.0 
 *
 */
public class PainterCanvas extends SurfaceView implements Callback {	
		
	private PainterThread mThread;
	private Bitmap mBitmap;
	private Bitmap mActiveBitmap;
	private BrushPreset mPreset;
	
	private boolean mIsSetup;
	private int countChanges;
	private boolean mUndo;

	public static final int BLUR_TYPE_NONE = 0;
	public static final int BLUR_TYPE_NORMAL = 1;
	public static final int BLUR_TYPE_INNER = 2;
	public static final int BLUR_TYPE_OUTER = 3;
	public static final int BLUR_TYPE_SOLID = 4;	

	public PainterCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        SurfaceHolder holder = this.getHolder();
        holder.addCallback(this);
        
        this.mPreset = new BrushPreset(BrushPreset.PEN, Color.BLACK);

        this.setFocusable(true);
	}
	
	public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus){
        	this.getThread().freeze();
        }
        else {
        	if(!this.isSetup()){
        		this.getThread().activate();
        	}
        	else {
        		this.getThread().setup();
        	}
        }
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(this.mActiveBitmap == null) {
			this.mActiveBitmap = Bitmap.createBitmap(
					width, 
					height, 
					Bitmap.Config.ARGB_8888
			);			
			this.getThread().setActiveBitmap(this.mActiveBitmap, true);
		}
		else {
			this.getThread().setActiveBitmap(this.mActiveBitmap, false);
		}
		
		if(this.mUndo) {
			this.getThread().undo();
		}
		
		if(this.mBitmap == null) {
			this.mBitmap = Bitmap.createBitmap(
					width, 
					height, 
					Bitmap.Config.ARGB_8888
			);	
			
			this.getThread().setBitmap(this.mBitmap, true);			
			Painter painter = (Painter) this.getContext();			
			Bitmap bitmap = painter.getLastPicture();			
			
			if(bitmap != null) {
				float bitmapWidth = bitmap.getWidth();
				float bitmapHeight = bitmap.getHeight();
				float scale = 1.0f;
				
				Matrix matrix = new Matrix();
				if(width != bitmapWidth || height != bitmapHeight) {					
					if(width == bitmapHeight || height == bitmapWidth){
						if(width > height) {						
							matrix.postRotate(-90, width/2, height/2);						
						}
						else {
							matrix.postRotate(90, width/2, height/2);		
						}
					}
					else {
						if(painter.getRequestedOrientation() == 
							ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
							if(bitmapWidth > bitmapHeight && bitmapWidth > width) {
								scale = (float) width / bitmapWidth ;								
							}
							else if(bitmapHeight > bitmapWidth && bitmapHeight > height) {
								scale = (float) height / bitmapHeight;
							}
						}
						else {							
							if(bitmapHeight > bitmapWidth && bitmapHeight > height) {
								scale = (float) height / bitmapHeight;
							}
							else if(bitmapWidth > bitmapHeight && bitmapWidth > width) {
								scale = (float) width / bitmapWidth ;
							}
						}
					}
					
					if(scale == 1.0f) {
						matrix.preTranslate(
								width/2 - bitmapWidth/2, 
								height/2 - bitmapHeight/2
						);	
					}
					else {
						matrix.postScale(scale, scale, bitmapWidth/2, bitmapHeight/2);
						matrix.postTranslate(
								width/2 - bitmapWidth/2, 
								height/2 - bitmapHeight/2
						);	
					}
				}
				this.getThread().restoreBitmap(bitmap, matrix);
			}			
		}
		else {
			this.getThread().setBitmap(this.mBitmap, false);
		}		
		
		this.getThread().setPreset(this.mPreset);
		if(!this.isSetup()){
			this.getThread().activate();
		}
		else {
			this.getThread().setup();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {		
		this.getThread().on();		
		this.getThread().start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {		
		boolean retry = true;
        this.getThread().off();
        while (retry) {
            try {
            	this.getThread().join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
        
        this.mThread = null;
	}
	
	public boolean onTouchEvent(MotionEvent event) {  
		if(!this.getThread().isReady()) return false;
		
		switch(event.getAction()) 
		{ 			
			case MotionEvent.ACTION_DOWN: 
				this.countChanges+= 1;
				this.getThread().drawBegin();
				this.mUndo = false;
				break;
			case MotionEvent.ACTION_MOVE: 			
				this.getThread().draw((int)event.getX(),(int)event.getY());		
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				this.getThread().drawEnd();
				break;			
		} 
		return true; 
	}
	
	public PainterThread getThread() {
		if (this.mThread == null) {
			this.mThread = new PainterThread(this.getHolder());
		}
		return this.mThread;
	}
	
	public void saveBitmap(String pictureName) throws FileNotFoundException {
		synchronized (this.getHolder()){
			FileOutputStream fos = new FileOutputStream(pictureName);
			this.getThread().getBitmap().compress(CompressFormat.PNG, 100, fos);
		}
	}
	
	public BrushPreset getCurrentPreset() {
		return this.mPreset;
	}
	
	public void setPresetColor(int color) {
		this.mPreset.setColor(color);
		this.getThread().setPreset(this.mPreset);
	}
	
	public void setPresetSize(float size) {
		this.mPreset.setSize(size);
		this.getThread().setPreset(this.mPreset);
	}
	
	public void setPresetBlur(Blur blurStyle, int blurRadius) {
		this.mPreset.setBlur(blurStyle, blurRadius);
		this.getThread().setPreset(this.mPreset);
	}
	
	public void setPresetBlur(int blurStyle, int blurRadius) {
		this.mPreset.setBlur(blurStyle, blurRadius);
		this.getThread().setPreset(this.mPreset);
	}
	
	public void setPreset(BrushPreset preset) {
		this.mPreset = preset;
		this.getThread().setPreset(this.mPreset);
	}
	
	public boolean isSetup() {
		return this.mIsSetup;
	}
	
	public void setup(boolean setup) {
		this.mIsSetup = setup;
	}
	
	public boolean isChanged() {
		return (this.countChanges > 0);
	}
	
	public void changed(boolean changed) {
		if(!changed) {
			this.countChanges = 0;
		}
		else {
			this.countChanges+= 1;
		}		
	}
	
	public void undo() {
		if(!this.mUndo) {
			this.mUndo = true;
			this.getThread().undo();
			this.countChanges-= 1;
		}
		else {
			this.mUndo = false;
			this.getThread().redo();
			this.countChanges+= 1;
		}
	}
	
	public boolean canUndo() {
		if(this.isChanged() && !this.mUndo) {
			return true;
		}
		return false;
	}
	
	public boolean canRedo() {
		if(this.mUndo) {
			return true;
		}
		return false;
	}
	
	public void setCanvasAsWallpaper() {
		// TODO Auto-generated method stub
		synchronized (this.getHolder()){
			  WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
	          try {
				wallpaperManager.setBitmap(this.getThread().getBitmap());
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
}
