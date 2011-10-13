package org.sprite2d.apps.pp;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.view.SurfaceHolder;

/**
 * Base draw logic 
 * 
 * @author Arthur Bikmullin (devolonter)
 * @version 1.17
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

	/**
	 * Bitmap for drawing
	 */
	private Bitmap mBitmap;	
	
	/**
	 * True if application is running
	 */
	private boolean mIsActive;	
	
	/**
	 * Status of the running application
	 */
	private int mStatus;
	
	private State mState;
	
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
        waitForBitmap();
		
        while (isRun()) {
        	Canvas canvas = null;
            try {
                canvas = mHolder.lockCanvas();
                synchronized (mHolder) {               	
                	switch(mStatus) {
                		case PainterThread.READY: {
                			canvas.drawBitmap(mBitmap, 0, 0, null);
							break;
                		}
                		case SETUP: {
                			canvas.drawColor(mCanvasBgColor);
                			canvas.drawLine(
                    				50, 
                    				(mBitmap.getHeight()/100)*35, 
                    				mBitmap.getWidth() - 50, 
                    				(mBitmap.getHeight()/100)*35, mBrush);
                			break;
                		}
                	}                   	
                }
            } finally {
                if (canvas != null) {
                    mHolder.unlockCanvasAndPost(canvas);
                }
                if(isFreeze()) {
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
		
		if (mState.redoBuffer != null && !mState.isUndo) {
			mState.undoBuffer = mState.redoBuffer;
		}
		
		mState.isUndo = false;
	}
	
	public void drawEnd() {
		mState.redoBuffer = saveBuffer();
		
		mLastBrushPointX = -1;
		mLastBrushPointY = -1;
	}
	
	public void draw(int x, int y) {
		if(mLastBrushPointX > 0){
			if(mLastBrushPointX - x == 0 && mLastBrushPointY - y == 0) {
                return;
			}

			mCanvas.drawLine(
					x, 
					y, 
					mLastBrushPointX, 
					mLastBrushPointY,
					mBrush
			);
		}
		else {
			mCanvas.drawCircle(
					x, 
					y, 
					mBrushSize*.5f, 
					mBrush
			);
		}
		
		mLastBrushPointX = x;
		mLastBrushPointY = y;
    }

	public void setBitmap(Bitmap bitmap, boolean clear) {
		mBitmap = bitmap;
		if(clear){
			mBitmap.eraseColor(mCanvasBgColor);
		}
	
		mCanvas = new Canvas(mBitmap);
	}
	
	public void restoreBitmap(Bitmap bitmap, Matrix matrix) {
		mCanvas.drawBitmap(bitmap, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
		mState.undoBuffer = saveBuffer();
	}
	
	public void clearBitmap() {
		mBitmap.eraseColor(mCanvasBgColor);
		mState.undoBuffer = null;
		mState.redoBuffer = null;
	}
	
	public Bitmap getBitmap() {
		return mBitmap;
	}

	public void on() {
		mIsActive = true;
	}
	
	public void off() {
		mIsActive = false;
	}
	
	public void freeze() {
		mStatus = SLEEP;
	}
	
	public void activate() {
		mStatus = READY;
	}
	
	public void setup() {
		mStatus = SETUP;
	}
	
	public boolean isFreeze() {
		return (mStatus == SLEEP);
	}
	
	public boolean isSetup() {
		return (mStatus == SETUP);
	}
	
	public boolean isReady() {
		return (mStatus == READY);
	}
	
	public boolean isRun() {
		return mIsActive;
	}
	
	public void undo() {
		if (mState.undoBuffer == null) {
			mBitmap.eraseColor(mCanvasBgColor);
		} else {
			restoreBuffer(mState.undoBuffer);	
		}
		
		mState.isUndo = true;
	}
	
	public void redo() {
		if (mState.redoBuffer != null) {
			restoreBuffer(mState.redoBuffer);
		}
		
		mState.isUndo = false;
	}
	
	public int getBackgroundColor() {
		return mCanvasBgColor;
	}
	
	public void setState(State state) {
		this.mState = state;
	}
	
	private void waitForBitmap() {
		while (mBitmap == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private byte[] saveBuffer() {
		byte[] buffer = new byte[mBitmap.getRowBytes() * mBitmap.getHeight()];		
		Buffer byteBuffer = ByteBuffer.wrap(buffer);
		mBitmap.copyPixelsToBuffer(byteBuffer);	
		return buffer;
	}

	private void restoreBuffer(byte[] buffer) {
		Buffer byteBuffer = ByteBuffer.wrap(buffer);
		mBitmap.copyPixelsFromBuffer(byteBuffer);
	}
	
	public static class State {
		public byte[] undoBuffer = null;
		public byte[] redoBuffer = null;
		public boolean isUndo = false;
	}
}
