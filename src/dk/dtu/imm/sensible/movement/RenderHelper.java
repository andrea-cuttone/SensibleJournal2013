package dk.dtu.imm.sensible.movement;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class RenderHelper {

	final int colorTextBg = Color.argb(96, 0, 0, 0);
	private Paint paint;
	private RectF rect;
	
	public RenderHelper() {
		paint = new Paint();
		paint.setTextSize(65.0f);
		rect = new RectF();
	}
	
	public void renderText(Canvas canvas, String text, int x, int y) {
		paint.setColor(colorTextBg);
		float w = paint.measureText(text);
		rect.set(x - 5, y - paint.getTextSize() + 10, x + w + 5, y + 10);
		canvas.drawRoundRect(rect, 15, 15, paint);
		paint.setColor(Color.WHITE);
		canvas.drawText(text, x, y, paint);
	}
}
