package org.sprite2d.apps.pp;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Extended spinner for onclick reaction process
 * 
 * @author Artut Bikmullin (devolonter)
 * @version 1.0 
 *
 */
public class BlurStyleSpinner extends Spinner {

	public BlurStyleSpinner(Context context) {
		super(context);
	}

	public BlurStyleSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BlurStyleSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Painter painter = (Painter) this.getContext();
		painter.resetPresets();
		super.onClick(dialog, which);		
	}
}
