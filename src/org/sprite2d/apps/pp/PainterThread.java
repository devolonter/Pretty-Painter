package org.sprite2d.apps.pp;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Matrix;
import android.view.SurfaceHolder;

/**
 * Base draw logic 
 * 
 * @author Artut Bikmullin (devolonter)
 * @version 1.0 
 *
 */
public class PainterThread extends Thread {
	
	/**
	 * Freeze when freeze() called
	 */
	public static final int SLEEP = 0;
	
	/**
	 * Application ready when activate() called
	 */
	public static final int READY = 1;
	
	public static final int SETUP = 2;
	
	/**
	 * Holder
	 */
	private SurfaceHolder mHolder;
	
	/**
	 * Brush object instance of Paint Class
	 */
	private Paint mBrush;
	
	/**
	 * Brush size in pixels
	 */
	private float mBrushSize;	
	
	/**
	 * Last brush point detect for anti-alias
	 */
	private int mLastBrushPointX;
	
	/**
	 * Last brush point detect for anti-alias
	 */
	private int mLastBrushPointY;	
	
	/**
	 * Canvas clear color
	 */
	private int mCanvasBgColor;
	
	/**
	 * Canvas object for drawing bitmap
	 */
	private Canvas mCanvas;
	
	private Canvas mActiveCanvas;
	
	/**
	 * Bitmap for drawing
	 */
	private Bitmap mBitmap;
	
	private Bitmap mActiveBitmap;
	
	/**
	 * True if application is running
	 */
	private boolean mIsActive;
	
	private boolean mUndo;
	
	/**
	 * Status of the running application
	 */
	private int mStatus;
	
	/**
	 * 
	 * @param surfaceHolder
	 * @param context
	 * @param handler
	 */
	public PainterThread(SurfaceHolder surfaceHolder) {
		//base data
		this.mHolder = surfaceHolder;
	
		//defaults brush settings
		this.mBrushSize = 2;
		this.mBrush = new Paint();
		this.mBrush.setAntiAlias(true);
		this.mBrush.setColor(Color.rgb(0, 0, 0));
		this.mBrush.setStrokeWidth(this.mBrushSize);
		this.mBrush.setStrokeCap(Cap.ROUND);
		
		//default canvas settings
		this.mCanvasBgColor = Color.WHITE;		
		
		//set negative coordinates for reset last point
		this.mLastBrushPointX = -1;
		this.mLastBrushPointY = -1;		
	}	
	
	@Override
	public void run() {
		this.waitForBitmap();
		
        while (this.isRun()) {
        	Canvas c = null;
            try {
                c = this.mHolder.lockCanvas();
                synchronized (this.mHolder) {               	
                	switch(this.mStatus) {
                		case PainterThread.READY: {
                			c.drawBitmap(this.mBitmap, 0, 0, null);
                			if(!this.mUndo){
                				c.drawBitmap(this.mActiveBitmap, 0, 0, null);
                			}
                			break;
                		}
                		case PainterThread.SETUP: {
                			c.drawColor(this.mCanvasBgColor);
                			c.drawLine(
                    				50, 
                    				(this.mBitmap.getHeight()/100)*35, 
                    				this.mBitmap.getWidth() - 50, 
                    				(this.mBitmap.getHeight()/100)*35, 
                    				this.mBrush
                    		);
                			break;
                		}
                	}                   	
                }
            } finally {
                if (c != null) {
                    this.mHolder.unlockCanvasAndPost(c);
                }
                if(this.isFreeze()) {
                	try {
    					Thread.sleep(100);
    				} catch (InterruptedException e) {}
                }
            }
        }
    }	
	
	public void setPreset(BrushPreset preset) {
		this.mBrush.setColor(preset.color);
		this.mBrushSize = preset.size;
		this.mBrush.setStrokeWidth(preset.size);
		if(preset.blurStyle != null && preset.blurRadius > 0){
			this.mBrush.setMaskFilter(new BlurMaskFilter(preset.blurRadius, preset.blurStyle));
		}
		else {
			this.mBrush.setMaskFilter(null);
		}
	}	
	
	public void drawBegin() {
		this.mLastBrushPointX = -1;
		this.mLastBrushPointY = -1;
		PainterThread.this.completeDraw();		
	}
	
	public void completeDraw() {	
		synchronized (this.mHolder) { 
		    if(!this.mUndo) {
				this.mCanvas.drawBitmap(
						 this.mActiveBitmap, 0, 0, null);			
			}
			this.mActiveBitmap.eraseColor(Color.TRANSPARENT);
			this.redo();
		}
	}
	
	public void drawEnd() {
		this.mLastBrushPointX = -1;
		this.mLastBrushPointY = -1;	
	}
	
	public boolean draw(int x, int y) {	
		if(this.mLastBrushPointX > 0){			
			if(this.mLastBrushPointX - x == 0 && this.mLastBrushPointY - y == 0) {
				return false;
			}
			
			this.mActiveCanvas.drawLine(
					x, 
					y, 
					this.mLastBrushPointX, 
					this.mLastBrushPointY,
					this.mBrush
			);
		}
		else {
			this.mActiveCanvas.drawCircle(
					x, 
					y, 
					this.mBrushSize*.5f, 
					this.mBrush
			);
		}
		
		this.mLastBrushPointX = x;
		this.mLastBrushPointY = y;	
		return true;
	}
	
	public void setBitmap(Bitmap bitmap, boolean clear) {
		this.mBitmap = bitmap;
		if(clear){
			this.mBitmap.eraseColor(this.mCanvasBgColor);
		}	
		
		this.mCanvas = new Canvas(this.mBitmap);
	}
	
	public void setActiveBitmap(Bitmap bitmap, boolean clear) {
		this.mActiveBitmap = bitmap;
		if(clear){
			this.mActiveBitmap.eraseColor(Color.TRANSPARENT);
		}
		
		this.mActiveCanvas = new Canvas(this.mActiveBitmap);
	}
	
	public void restoreBitmap(Bitmap bitmap, Matrix matrix) {
		this.mCanvas.drawBitmap(bitmap, matrix, null);
	}
	
	public void clearBitmap() {
		this.mBitmap.eraseColor(this.mCanvasBgColor);
		this.mActiveBitmap.eraseColor(Color.TRANSPARENT);
	}
	
	public Bitmap getBitmap() {
		this.completeDraw();
		return this.mBitmap;
	}
	
	public void on() {
		this.mIsActive = true;
	}
	
	public void off() {
		this.mIsActive = false;
	}
	
	public void freeze() {
		this.mStatus = PainterThread.SLEEP;
	}
	
	public void activate() {
		this.mStatus = PainterThread.READY;
	}
	
	public void setup() {
		this.mStatus = PainterThread.SETUP;
	}
	
	public boolean isFreeze() {
		return (this.mStatus == PainterThread.SLEEP);
	}
	
	public boolean isSetup() {
		return (this.mStatus == PainterThread.SETUP);
	}
	
	public boolean isReady() {
		return (this.mStatus == PainterThread.READY);
	}
	
	public boolean isRun() {
		return this.mIsActive;
	}
	
	public void undo() {
		this.mUndo = true;
	}
	
	public void redo() {
		this.mUndo = false;
	}
	
	private void waitForBitmap() {
		while (this.mBitmap == null || this.mActiveBitmap == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
