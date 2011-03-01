package org.sprite2d.apps.pp;

import java.io.Serializable;

import android.content.pm.ActivityInfo;

/**
 * Application settings class
 * 
 * @author Artut Bikmullin (devolonter)
 * @version 1.0 
 *
 */
class PainterSettings implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public BrushPreset preset = null;
	public String lastPicture = null;
	public int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
}
