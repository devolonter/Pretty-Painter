package org.sprite2d.apps.pp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

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

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		mPreset = new BrushPreset(BrushPreset.PEN, Color.BLACK);

        setFocusable(true);
	}

	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) {
            getThread().freeze();
		} else {
			if (!isSetup()) {
                getThread().activate();
			} else {
                getThread().setup();
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mActiveBitmap == null) {
			mActiveBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
            getThread().setActiveBitmap(mActiveBitmap, true);
		} else {
            getThread().setActiveBitmap(mActiveBitmap, false);
		}

		if (mUndo) {
            getThread().undo();
		}

		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            getThread().setBitmap(mBitmap, true);
			Painter painter = (Painter) getContext();
			Bitmap bitmap = painter.getLastPicture();

			if (bitmap != null) {
				float bitmapWidth = bitmap.getWidth();
				float bitmapHeight = bitmap.getHeight();
				float scale = 1.0f;

				Matrix matrix = new Matrix();
				if (width != bitmapWidth || height != bitmapHeight) {
					if (width == bitmapHeight || height == bitmapWidth) {
						if (width > height) {
							matrix.postRotate(-90, width / 2, height / 2);
						} else if (bitmapWidth != bitmapHeight) {
							matrix.postRotate(90, width / 2, height / 2);
						} else {
							if (painter.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
								matrix.postRotate(-90, width / 2, height / 2);
							}
						}
					} else {
						if (painter.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
							if (bitmapWidth > bitmapHeight
									&& bitmapWidth > width) {
								scale = (float) width / bitmapWidth;
							} else if (bitmapHeight > bitmapWidth
									&& bitmapHeight > height) {
								scale = (float) height / bitmapHeight;
							}
						} else {
							if (bitmapHeight > bitmapWidth
									&& bitmapHeight > height) {
								scale = (float) height / bitmapHeight;
							} else if (bitmapWidth > bitmapHeight
									&& bitmapWidth > width) {
								scale = (float) width / bitmapWidth;
							}
						}
					}

					if (scale == 1.0f) {
						matrix.preTranslate((width - bitmapWidth) / 2,
								(height - bitmapHeight) / 2);
					} else {
						matrix.postScale(scale, scale, bitmapWidth / 2,
								bitmapHeight / 2);
						matrix.postTranslate((width - bitmapWidth) / 2,
								(height - bitmapHeight) / 2);
					}
				}
                getThread().restoreBitmap(bitmap, matrix);
			}
		} else {
            getThread().setBitmap(mBitmap, false);
		}

        getThread().setPreset(mPreset);
		if (!isSetup()) {
            getThread().activate();
		} else {
            getThread().setup();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
        getThread().on();
        getThread().start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
        getThread().off();
		while (retry) {
			try {
                getThread().join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}

		mThread = null;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (!getThread().isReady()) {
			return false;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			countChanges += 1;
            getThread().drawBegin();
			mUndo = false;
			break;
		case MotionEvent.ACTION_MOVE:
            getThread().draw((int) event.getX(), (int) event.getY());
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
            getThread().drawEnd();
			break;
		}
		return true;
	}

	public PainterThread getThread() {
		if (mThread == null) {
			mThread = new PainterThread(getHolder());
		}
		return mThread;
	}

	public void saveBitmap(String pictureName) throws FileNotFoundException {
		synchronized (getHolder()) {
			FileOutputStream fos = new FileOutputStream(pictureName);
            getThread().getBitmap().compress(CompressFormat.PNG, 100, fos);
		}
	}

	public BrushPreset getCurrentPreset() {
		return mPreset;
	}

	public void setPresetColor(int color) {
		mPreset.setColor(color);
        getThread().setPreset(mPreset);
	}

	public void setPresetSize(float size) {
		mPreset.setSize(size);
        getThread().setPreset(mPreset);
	}

	public void setPresetBlur(Blur blurStyle, int blurRadius) {
		mPreset.setBlur(blurStyle, blurRadius);
        getThread().setPreset(mPreset);
	}

	public void setPresetBlur(int blurStyle, int blurRadius) {
		mPreset.setBlur(blurStyle, blurRadius);
        getThread().setPreset(mPreset);
	}

	public void setPreset(BrushPreset preset) {
		mPreset = preset;
        getThread().setPreset(mPreset);
	}

	public boolean isSetup() {
		return mIsSetup;
	}

	public void setup(boolean setup) {
		mIsSetup = setup;
	}

	public boolean isChanged() {
		return (countChanges > 0);
	}

	public void changed(boolean changed) {
		if (!changed) {
			countChanges = 0;
		} else {
			countChanges += 1;
		}
	}

	public void undo() {
		if (!mUndo) {
			mUndo = true;
            getThread().undo();
			countChanges -= 1;
		} else {
			mUndo = false;
            getThread().redo();
			countChanges += 1;
		}
	}

	public boolean canUndo() {
        return isChanged() && !mUndo;
    }

	public boolean canRedo() {
        return mUndo;
    }
}
