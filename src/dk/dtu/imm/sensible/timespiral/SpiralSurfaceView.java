package dk.dtu.imm.sensible.timespiral;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.SurfaceView;
import dk.dtu.imm.sensible.stats.TimeAtLocation;
import dk.dtu.imm.sensible.utils.MathUtils;
import static dk.dtu.imm.sensible.Constants.PI;

public class SpiralSurfaceView extends SurfaceView {

	private static final int PERIOD_24H = 0;
	private static final int PERIOD_7DAYS = 1;
	private static final String [] DAY_NAMES = { "Fri", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu" };
	
	private static final int[] START_ANGLE = { 70, 20 };
	private static final float END_ANGLE = 0;
	private static final float[] A = { 25f, 33f };
	private static final float[] B = { 0.01f, 0.03f };
	private static final float[] THETA_INCREMENT = { PI/4, PI/16 };
	private static final float[] TEXT_SIZE_K = { 1.9f, 0.8f};
	private static final double[] TEXT_MIN_ANGLE = { PI/16, PI/8 };

	private Paint paint;
	private DashPathEffect dashPathEffect;
	private RectF rect = new RectF();
	private ColorGenerator colorGenerator;
	private ScaleGestureDetector scaleGestureDetector;
	private int selected;
	private float zoomFactor;
	private boolean isZooming;
	private List<TimeAtLocation> allStaticLocs;
	private int offsetX;
	private int offsetY;
	private GestureDetector gestureDetector;
	private int periodMode;
	private PorterDuffXfermode xfermode;
	private Path spiralPath = new Path();
	private Path textPath = new Path();
	
	public SpiralSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		periodMode = PERIOD_24H;
		zoomFactor = 1f;
		allStaticLocs = new ArrayList<TimeAtLocation>();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);
		paint.setStrokeCap(Cap.BUTT);
		paint.setStrokeJoin(Join.BEVEL);
		dashPathEffect = new DashPathEffect(new float[] {10,20}, 0);
		xfermode = new PorterDuffXfermode(Mode.SRC);
		scaleGestureDetector = new ScaleGestureDetector(context, new OnScaleGestureListener() {

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
				isZooming = false;
			}
			
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				isZooming = true;
				return true;
			}
			
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				zoomFactor *= detector.getScaleFactor();
				zoomFactor = Math.min(10.0f, Math.max(1f, zoomFactor));
				doDraw();
				return true;
			}
		});
		gestureDetector = new GestureDetector(new SimpleOnGestureListener() {
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if(isZooming == false) {
					if(FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY) < 300) {
						offsetX -= distanceX;
						offsetY -= distanceY;
						doDraw();
					}
				}
				return true;
			}
			
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				periodMode = (periodMode + 1) % 2;
				offsetX = offsetY = 0;
				zoomFactor = 1;
				doDraw();
				return true;
			}
		});

	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleGestureDetector.onTouchEvent(event);
		gestureDetector.onTouchEvent(event);
		return true;
	}
	
	public void setColorGenerator(ColorGenerator colorGenerator) {
		this.colorGenerator = colorGenerator;
	}
	
	public void render(List<TimeAtLocation> allStaticLocs, int selected) {
		this.allStaticLocs = allStaticLocs;
		this.selected = selected;
		doDraw();
	}

	private void doDraw() {
		Canvas canvas = null;
		try {
			canvas = getHolder().lockCanvas(null);
			if(canvas != null) {
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				int centerX = canvas.getWidth() / 2 + offsetX;
				int centerY = canvas.getHeight() / 2 + offsetY;
				paint.setColor(Color.WHITE);
				paint.setPathEffect(null);
				paint.setXfermode(xfermode);
				paint.setColor(Color.argb(64, 255, 255, 255));
				if(allStaticLocs.size() > 0)
					drawSpiralArc(canvas, centerX, centerY, timeToAngle(allStaticLocs.get(allStaticLocs.size() - 1).start),
							timeToAngle(allStaticLocs.get(0).start), 0.5f, "");
				for (int i = 0; i < allStaticLocs.size(); i++) {
					TimeAtLocation t = allStaticLocs.get(i);
						float startTheta = timeToAngle(t.start);
						float endTheta = timeToAngle(t.start + t.duration);
						if(endTheta < END_ANGLE * PI) {
							break;
						}
						paint.setColor(colorGenerator.get(t.clusterId));
						drawSpiralArc(canvas, centerX, centerY, startTheta, endTheta, 1f, t.getDateTimeString());
						if(selected == i) {
							paint.setColor(Color.BLACK);
							drawSpiralArc(canvas, centerX, centerY, startTheta, endTheta, 0.3f, "");
						}
				}
				paint.setXfermode(null);
				paint.setTextSize(32.0f);
				if(periodMode == PERIOD_24H) {
					drawText(canvas, "06:00", canvas.getWidth() - 50, centerY + 30);
					drawText(canvas, "18:00", 40, centerY + 30);
					drawText(canvas, "00:00", centerX - 55, 28);
					drawText(canvas, "12:00", centerX + 40, canvas.getHeight() - 8);
				} else if(periodMode == PERIOD_7DAYS) {
					for (int i = 0; i < 7; i++) {
						float angle = i * 2 * PI / 7;
						final int D = 280;
						int xx = (int) (centerX - 15 + D * FloatMath.cos(angle));
						int yy = (int) (centerY + 20 + D * FloatMath.sin(angle));
						drawText(canvas, DAY_NAMES[i], xx, yy);
					}
				}
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(1.0f);
				paint.setPathEffect(dashPathEffect);
				if(periodMode == PERIOD_24H) {
					canvas.drawLine(0, centerY, canvas.getWidth(), centerY, paint);
					canvas.drawLine(centerX, 0, centerX, canvas.getHeight(), paint);
				} else {
					for (int i = 0; i < 7; i++) {
						float angle = i * 2 * PI / 7 + PI / 7;
						final int D = 400;
						int xx = (int) (centerX - 35 + D * FloatMath.cos(angle));
						int yy = (int) (centerY + 18 + D * FloatMath.sin(angle));
						canvas.drawLine(centerX, centerY, xx, yy, paint);
					}
				}
				paint.setPathEffect(null);
			}
		} finally {
			if (canvas != null) {
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
	}
	
	private void drawText(Canvas canvas, String text, int x, int y) {
			float w = paint.measureText(text);
			rect.set(x - w / 2 - 2, y - paint.getTextSize() - 2, x + w/2 + 2, y + 10);
			paint.setColor(Color.argb(196, 0, 0, 0));
			canvas.drawRect(rect, paint);
			paint.setColor(Color.WHITE);
			canvas.drawText(text, x, y, paint);
	}
	
	private void drawSpiralArc(Canvas canvas, int centerX, int centerY, double startTheta, double endTheta, float widthMultiplier, String text) {
		float theta = (float) startTheta;
		paint.setStyle(Paint.Style.STROKE);
		while (theta < endTheta) {
			float outerRadius = getRadius(theta);
			rect.set(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
			float w = (getRadius(theta + 2 * PI) - outerRadius) * 0.8f * widthMultiplier;
			paint.setStrokeWidth(w);
			float a0 = theta * 180 / PI;
			float a1 = (float) (Math.min(THETA_INCREMENT[periodMode], endTheta - theta) * 180 / PI);
			spiralPath.reset();
			spiralPath.addArc(rect, a0, a1);
			canvas.drawPath(spiralPath, paint);
			theta = (float) Math.min(theta + THETA_INCREMENT[periodMode], endTheta);
		}
		double centerTheta = (startTheta + endTheta) / 2;
		float w = getRadius(centerTheta + 2 * PI) - getRadius(centerTheta);
		float a0 = (float) (startTheta * 180 / PI);
		float a1 = (float) ((endTheta - startTheta) * 180 / PI);
		paint.setStyle(Paint.Style.FILL);
		textPath.reset();
		textPath.addArc(rect, a0, a1);
		float textSize = Math.min(35, w * TEXT_SIZE_K[periodMode]);
		if (textSize > 15 && endTheta - startTheta > TEXT_MIN_ANGLE[periodMode]) {
			paint.setTextSize(textSize);
			paint.setColor(Color.WHITE);
			canvas.drawTextOnPath(text, textPath, 0, w / 2, paint);
		}
	}

	private float timeToAngle(long t) {
		long startt = (long) MathUtils.roundToMultiple(allStaticLocs.get(0).start, 60 * 60 * 24 * 7);
		// convert to GMT + 1
		t += 60 * 60;
		// hack to adjust timestamp before daylight saving termination 
		if(t < 1351375200) {
			t += 60 * 60;
		}
		float a = 0;
		if(periodMode == PERIOD_7DAYS) {
			//t = (long) MathUtils.roundToMultiple(t, 60 * 60 * 1);
			a = (MathUtils.roundToMultiple(START_ANGLE[periodMode] * zoomFactor, 2) - 0.5f) * PI 
					- 2 * PI * (startt - t) / (60 * 60 * 24 * 7f);
		} else if(periodMode == PERIOD_24H) {
			a = (MathUtils.roundToMultiple(START_ANGLE[periodMode] * zoomFactor, 2) - 0.5f) * PI 
					- 2 * PI * (startt - t) / (60 * 60 * 24);
		}
		return a;
	}
	
	private float getRadius(double theta) {
		return (float) (A[periodMode] * Math.exp(B[periodMode] * theta));
	}
	
}
