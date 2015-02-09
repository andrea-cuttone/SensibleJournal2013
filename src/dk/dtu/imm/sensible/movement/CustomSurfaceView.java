package dk.dtu.imm.sensible.movement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class CustomSurfaceView extends SurfaceView {

	public CustomSurfaceView(Context context, AttributeSet attributeSet) {
	    super(context, attributeSet);
	    setZOrderOnTop(true);    
	    getHolder().setFormat(PixelFormat.TRANSPARENT);
	}
	
	public void clearCanvas() {
		Canvas canvas = getHolder().lockCanvas();
		if(canvas != null) {
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			getHolder().unlockCanvasAndPost(canvas);
		}
	}
	
}
