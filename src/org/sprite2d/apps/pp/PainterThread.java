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
		mHolder = surfaceHolder;
	
		//defaults brush settings
		mBrushSize = 2;
		mBrush = new Paint();
		mBrush.setAntiAlias(true);
		mBrush.setColor(Color.rgb(0, 0, 0));
		mBrush.setStrokeWidth(mBrushSize);
		mBrush.setStrokeCap(Cap.ROUND);
		
		//default canvas settings
		mCanvasBgColor = Color.WHITE;		
		
		//set negative coordinates for reset last point
		mLastBrushPointX = -1;
		mLastBrushPointY = -1;		
	}	
	
	@Override
	public void run() {
		this.waitForBitmap();
		
        while (this.isRun()) {
        	Canvas c = null;
            try {
                c = mHolder.lockCanvas();
                synchronized (mHolder) {               	
                	switch(mStatus) {
                		case PainterThread.READY: {
                			c.drawBitmap(mBitmap, 0, 0, null);
                			if(!mUndo){
                				c.drawBitmap(mActiveBitmap, 0, 0, null);
                			}
                			break;
                		}
                		case PainterThread.SETUP: {
                			c.drawColor(mCanvasBgColor);
                			c.drawLine(
                    				50, 
                    				(mBitmap.getHeight()/100)*35, 
                    				mBitmap.getWidth() - 50, 
                    				(mBitmap.getHeight()/100)*35, mBrush);
                			break;
                		}
                	}                   	
                }
            } finally {
                if (c != null) {
                    mHolder.unlockCanvasAndPost(c);
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
		mBrush.setColor(preset.color);
		mBrushSize = preset.size;
		mBrush.setStrokeWidth(preset.size);
		if(preset.blurStyle != null && preset.blurRadius > 0){
			mBrush.setMaskFilter(new BlurMaskFilter(preset.blurRadius, preset.blurStyle));
		}
		else {
			mBrush.setMaskFilter(null);
		}
	}	
	
	public void drawBegin() {
		mLastBrushPointX = -1;
		mLastBrushPointY = -1;
		PainterThread.this.completeDraw();		
	}
	
	public void completeDraw() {	
		synchronized (mHolder) { 
		    if(!mUndo) {
				mCanvas.drawBitmap(mActiveBitmap, 0, 0, null);			
			}
			mActiveBitmap.eraseColor(Color.TRANSPARENT);
			this.redo();
		}
	}
	
	public void drawEnd() {
		mLastBrushPointX = -1;
		mLastBrushPointY = -1;	
	}
	
	public boolean draw(int x, int y) {	
		if(mLastBrushPointX > 0){			
			if(mLastBrushPointX - x == 0 && mLastBrushPointY - y == 0) {
				return false;
			}
			
			mActiveCanvas.drawLine(
					x, 
					y, 
					mLastBrushPointX, 
					mLastBrushPointY,
					mBrush
			);
		}
		else {
			mActiveCanvas.drawCircle(
					x, 
					y, 
					mBrushSize*.5f, 
					mBrush
			);
		}
		
		mLastBrushPointX = x;
		mLastBrushPointY = y;	
		return true;
	}
	
	public void setBitmap(Bitmap bitmap, boolean clear) {
		mBitmap = bitmap;
		if(clear){
			mBitmap.eraseColor(mCanvasBgColor);
		}	
		
		mCanvas = new Canvas(mBitmap);
	}
	
	public void setActiveBitmap(Bitmap bitmap, boolean clear) {
		mActiveBitmap = bitmap;
		if(clear){
			mActiveBitmap.eraseColor(Color.TRANSPARENT);
		}
		
		mActiveCanvas = new Canvas(mActiveBitmap);
	}
	
	public void restoreBitmap(Bitmap bitmap, Matrix matrix) {
		mCanvas.drawBitmap(bitmap, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
	}
	
	public void clearBitmap() {
		mBitmap.eraseColor(mCanvasBgColor);
		mActiveBitmap.eraseColor(Color.TRANSPARENT);
	}
	
	public Bitmap getBitmap() {
		this.completeDraw();
		return mBitmap;
	}
	
	public void on() {
		mIsActive = true;
	}
	
	public void off() {
		mIsActive = false;
	}
	
	public void freeze() {
		mStatus = PainterThread.SLEEP;
	}
	
	public void activate() {
		mStatus = PainterThread.READY;
	}
	
	public void setup() {
		mStatus = PainterThread.SETUP;
	}
	
	public boolean isFreeze() {
		return (mStatus == PainterThread.SLEEP);
	}
	
	public boolean isSetup() {
		return (mStatus == PainterThread.SETUP);
	}
	
	public boolean isReady() {
		return (mStatus == PainterThread.READY);
	}
	
	public boolean isRun() {
		return mIsActive;
	}
	
	public void undo() {
		mUndo = true;
	}
	
	public void redo() {
		mUndo = false;
	}
	
	public int getBackgroundColor() {
		return mCanvasBgColor;
	}
	
	private void waitForBitmap() {
		while (mBitmap == null || mActiveBitmap == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
