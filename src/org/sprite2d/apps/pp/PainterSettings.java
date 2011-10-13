package org.sprite2d.apps.pp;

import android.content.pm.ActivityInfo;

/**
 * Application settings class
 *
 * @author Arthur Bikmullin (devolonter)
 * @version 1.17
 *
 */

public class PainterSettings {
	public BrushPreset preset = null;
	public String lastPicture = null;
	public boolean forceOpenFile = false;
	public int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
}