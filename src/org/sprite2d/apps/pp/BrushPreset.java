package org.sprite2d.apps.pp;

import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Color;

/**
 * Brush settings driver
 * 
 * @author Arthur Bikmullin (devolonter)
 * @version 1.15
 *
 */
public class BrushPreset {	
	public float size = 2;
	public int color = Color.BLACK;
	public Blur blurStyle = null;
	public int blurRadius = 0;
	public int type = BrushPreset.CUSTOM;
	
	public static final int PENCIL = 1;
	public static final int BRUSH = 2;
	public static final int MARKER = 3;
	public static final int PEN = 4;
	public static final int CUSTOM = 5;
	
	public static final int BLUR_NORMAL = 1;
	public static final int BLUR_SOLID = 2;
	public static final int BLUR_OUTER = 3;
	public static final int BLUR_INNER = 4;
	
	public BrushPreset() {}
	
	public BrushPreset(int type, int color){
		switch(type) {
			case BrushPreset.PENCIL:
                set(2, color, Blur.INNER, 10);
				break;
			case BrushPreset.BRUSH:
                set(15, color, Blur.NORMAL, 18);
				break;
			case BrushPreset.MARKER:
                set(20, color);
				break;
			case BrushPreset.PEN:
                set(2, color);
				break;
			case BrushPreset.CUSTOM:
                setColor(color);
				break;
		}

        setType(type);
	}
	
	public BrushPreset(float size){
        set(size);
	}
	
	public BrushPreset(float size, int color){
        set(size, color);
	}
	
	public BrushPreset(float size, Blur blurStyle, int blurRadius){
        set(size, blurStyle, blurRadius);
	}
	
	public BrushPreset(float size, int blurStyle, int blurRadius){
        set(size, blurStyle, blurRadius);
	}
	
	public BrushPreset(float size, int color, Blur blurStyle, int blurRadius){
        set(size, color, blurStyle, blurRadius);
	}
	
	public BrushPreset(float size, int color, int blurStyle, int blurRadius){
        set(size, color, blurStyle, blurRadius);
	}
	
	public void set(float size){
        setSize(size);
	}
	
	public void set(float size, int color){
        setSize(size);
        setColor(color);
	}
	
	public void set(float size, Blur blurStyle, int blurRadius){
        setSize(size);
        setBlur(blurStyle, blurRadius);
	}
	
	public void set(float size, int blurStyle, int blurRadius){
        setSize(size);
        setBlur(blurStyle, blurRadius);
	}
	
	public void set(float size, int color, Blur blurStyle, int blurRadius){
        setSize(size);
        setBlur(blurStyle, blurRadius);
        setColor(color);
	}
	
	public void set(float size, int color, int blurStyle, int blurRadius){
        setSize(size);
        setBlur(blurStyle, blurRadius);
        setColor(color);
	}
	
	public void setColor(int color) {
		this.color = color;	
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void setSize(float size) {
		if(this.size != size){
            setType(BrushPreset.CUSTOM);
		}
		this.size = (size > 0) ? size : 1;
	}
	
	public void setBlur(Blur blurStyle, int blurRadius) {
		if(this.blurStyle != blurStyle || this.blurRadius != blurRadius){
            setType(BrushPreset.CUSTOM);
		}
		this.blurStyle = blurStyle;
		this.blurRadius = blurRadius;
	}
	
	public void setBlur(int blurStyle, int blurRadius) {
		int style = (this.blurStyle != null) ? this.blurStyle.ordinal()+1 : 0;
		if(style != blurStyle || this.blurRadius != blurRadius){
            setType(BrushPreset.CUSTOM);
		}

		switch(blurStyle) {
			case BrushPreset.BLUR_NORMAL: 
				this.blurStyle = Blur.NORMAL;
				break;
			case BrushPreset.BLUR_SOLID: 				
				this.blurStyle = Blur.SOLID;
				break;			
			case BrushPreset.BLUR_OUTER: 
				this.blurStyle = Blur.OUTER;
				break;
			case BrushPreset.BLUR_INNER: 				
				this.blurStyle = Blur.INNER;
				break;
			default: 
				this.blurStyle = null;				
				break;
		}
		this.blurRadius = blurRadius;
	}
}