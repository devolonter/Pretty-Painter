/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sprite2d.apps.pp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;


public class ColorPickerDialog extends Dialog {

	public interface OnColorChangedListener {
		void colorChanged(int color);
	}

	private OnColorChangedListener mListener;
	private int mInitialColor;

	private static class ColorPickerView extends View {
		private Paint mPaint;
		private Paint mCenterPaint;
		private Paint mRadialPaint;
		private final int[] mRadialColors;
		private OnColorChangedListener mListener;

		private Paint mGradientPaint;
		private int[] mLinearColors;

		ColorPickerView(Context c, OnColorChangedListener l, int color) {
			super(c);
			mListener = l;
			mRadialColors = new int[] { 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
					0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
			Shader s = new SweepGradient(0, 0, mRadialColors, null);

			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setShader(s);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(32);

			mLinearColors = getColors(color);
			Shader shader = new LinearGradient(0, 0, Center_X * 2, 0,
					mLinearColors, null, Shader.TileMode.CLAMP);

			mGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mGradientPaint.setStyle(Paint.Style.STROKE);
			mGradientPaint.setShader(shader);
			mGradientPaint.setStrokeWidth(32);

			mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mCenterPaint.setColor(color);
			mCenterPaint.setStrokeWidth(6);

			mRadialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mRadialPaint.setColor(color);
			mRadialPaint.setStrokeWidth(6);
		}

		private int[] getColors(int color) {
			if (color == Color.BLACK || color == Color.WHITE) {
				return new int[] { Color.BLACK, Color.WHITE };
			}
			return new int[] { Color.BLACK, color, Color.WHITE };
		}

		private boolean mHighlightCenter;
		private boolean mTrackingCenter;
		private boolean mTrackingLinGradient;

		@Override
		protected void onDraw(Canvas canvas) {
			float r = COLOR_CIRCLE - mPaint.getStrokeWidth() * 0.5f;

			canvas.translate(Center_X, COLOR_CIRCLE);

			canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

			if (mTrackingCenter) {
				int color = mCenterPaint.getColor();
				mCenterPaint.setStyle(Paint.Style.STROKE);

				if (mHighlightCenter) {
					mCenterPaint.setAlpha(0xFF);
				} else {
					mCenterPaint.setAlpha(0x80);
				}
				canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(), mCenterPaint);

				mCenterPaint.setStyle(Paint.Style.FILL);
				mCenterPaint.setColor(color);
			}

			int color = mRadialPaint.getColor();
			mLinearColors = getColors(color);
			Shader shader = new LinearGradient(0, 0, Center_X * 2, 0,
					mLinearColors, null, Shader.TileMode.CLAMP);
			mGradientPaint.setShader(shader);

			canvas.translate(-Center_X, 0);
			canvas.drawLine(0, r + 50, Center_X * 2, r + 50, mGradientPaint);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
			/*super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		    int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		    setMeasuredDimension(parentWidth, parentHeight);
			Center_X = (int) Math.ceil(parentWidth*.5);
			Center_Y = (int) Math.ceil(parentHeight*.5);*/

			setMeasuredDimension(Center_X * 2, Center_Y * 2 + 70);
		}

		private static int Center_X = 110;
		private static int Center_Y = 100;
		private static final int CENTER_RADIUS = 32;
		private static final int COLOR_CIRCLE = 100;

		private int ave(int s, int d, float p) {
			return s + Math.round(p * (d - s));
		}

		private int interpColor(int colors[], float unit) {
			if (unit <= 0) {
				return colors[0];
			}
			if (unit >= 1) {
				return colors[colors.length - 1];
			}

			float p = unit * (colors.length - 1);
			int i = (int) p;
			p -= i;

			// now p is just the fractional part [0...1) and i is the index
			int c0 = colors[i];
			int c1 = colors[i + 1];
			int a = ave(Color.alpha(c0), Color.alpha(c1), p);
			int r = ave(Color.red(c0), Color.red(c1), p);
			int g = ave(Color.green(c0), Color.green(c1), p);
			int b = ave(Color.blue(c0), Color.blue(c1), p);

			return Color.argb(a, r, g, b);
		}

		private static final float PI = 3.1415926f;

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX() - Center_X;
			float y = event.getY() - COLOR_CIRCLE;
			boolean inCenter = Math.sqrt(x * x + y * y) <= CENTER_RADIUS;
			boolean outOfRadialGradient = y > COLOR_CIRCLE;

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mTrackingCenter = inCenter;
				mTrackingLinGradient = outOfRadialGradient;
				if (inCenter) {
					mHighlightCenter = true;
					invalidate();
					break;
				}
			case MotionEvent.ACTION_MOVE:
				if (mTrackingCenter) {
					if (mHighlightCenter != inCenter) {
						mHighlightCenter = inCenter;
						invalidate();
					}
				} else if (mTrackingLinGradient) {
					float unit = Math.max(0, Math.min(Center_X * 2, x
							+ Center_X))
							/ (Center_X * 2);
					mCenterPaint.setColor(interpColor(mLinearColors, unit));
					invalidate();
				} else {
					float angle = (float) Math.atan2(y, x);
					// need to turn angle [-PI ... PI] into unit [0....1]
					float unit = angle / (2 * PI);
					if (unit < 0) {
						unit += 1;
					}
					int color = interpColor(mRadialColors, unit);
					mCenterPaint.setColor(color);
					mRadialPaint.setColor(color);
					invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTrackingCenter) {
					if (inCenter) {
						mListener.colorChanged(mCenterPaint.getColor());
					}
					mTrackingCenter = false; // so we draw w/o halo
					invalidate();
				}
				break;
			}
			return true;
		}
	}

	public ColorPickerDialog(Context context, OnColorChangedListener listener,
			int initialColor) {
		super(context);

		mListener = listener;
		mInitialColor = initialColor;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OnColorChangedListener l = new OnColorChangedListener() {
			public void colorChanged(int color) {
				mListener.colorChanged(color);
				dismiss();
			}
		};


        setContentView(new ColorPickerView(getContext(), l, mInitialColor));
        setTitle(R.string.color_pick);

		/*Display display = this.getWindow().getWindowManager().getDefaultDisplay();
		if(display.getWidth() < display.getHeight()) {
			this.getWindow().setLayout(
					LayoutParams.FILL_PARENT,
					(int) Math.ceil(display.getHeight()*.7f)
			);
		}
		else {
			this.getWindow().setLayout(
					(int) Math.ceil(display.getWidth()*.7f),
					LayoutParams.FILL_PARENT
			);
		}*/
	}
}