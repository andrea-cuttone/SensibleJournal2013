package dk.dtu.imm.sensible.btnetwork;

import static dk.dtu.imm.sensible.Constants.PI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.SurfaceView;
import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensible.utils.MathUtils;

public class BtNetworkView extends SurfaceView {

	private static final int LAYOUT_COMMUNITIES_CLUSTERS = 0;
	private static final int LAYOUT_SORT_BY_SIZE = 1;

	private static final int CANVAS_W = 720;
	private static final int CANVAS_H = 950;
	private static final int STARTING_UPDATE_COUNTER = 300;

	private int layoutType = LAYOUT_COMMUNITIES_CLUSTERS;
	private Paint paint;
	private Map<String, Bubble> bubbles;
	private String dateStr = "";
	private ScaleGestureDetector scaleGestureDetector;
	private GestureDetector gestureDetector;
	private boolean isZooming;
	private float zoomFactor;
	private float offsetX;
	private float offsetY;
	private int updateCounter;
	private Path textPath;
	private RectF rectF;
	private HashMap<Integer, Integer> comm2index;
	private Comparator<Bubble> bubblePackingComparator;
	private Comparator<Bubble> sizeComparator;

	public BtNetworkView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sizeComparator = new Comparator<Bubble>() {

			@Override
			public int compare(Bubble lhs, Bubble rhs) {
				return (int) (rhs.r - lhs.r);
			}
		};
		bubblePackingComparator = new Comparator<Bubble>() {

			@Override
			public int compare(Bubble lhs, Bubble rhs) {
					double d1 = Math.hypot(lhs.x - lhs.centerX, lhs.y - lhs.centerY);
					double d2 = Math.hypot(rhs.x - rhs.centerX, rhs.y - rhs.centerY);
					return d1 > d2 ? +1 : -1;
				} 
		};
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);
		textPath = new Path();
		rectF = new RectF();
		bubbles = new HashMap<String, Bubble>();
		zoomFactor = 1f;
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
				zoomFactor = Math.min(30, Math.max(0.5f, zoomFactor));
				update();
				return true;
			}
		});
		gestureDetector = new GestureDetector(new SimpleOnGestureListener() {
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if (isZooming == false) {
					if (FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY) < 300) {
						offsetX -= 3 * distanceX / zoomFactor;
						offsetY -= 3 * distanceY / zoomFactor;
						update();
					}
				}
				return true;
			}
			
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				layoutType = (layoutType + 1) % 2;
				setCenters();
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
	
	public void update() {
		Canvas canvas = null;
		try {
			canvas = getHolder().lockCanvas(null);
			if(canvas != null) {
				updateLayout();
				doDraw(canvas);
			}
		} finally {
			if (canvas != null) {
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
	}

	private void updateLayout() {
		List<Bubble> list = new ArrayList(this.bubbles.values());
		int size = list.size();
		if (layoutType == LAYOUT_COMMUNITIES_CLUSTERS) {
			//http://wiki.mcneel.com/developer/sdksamples/2dcirclepacking
			Collections.sort(list, bubblePackingComparator);
			for (int i = 0; i < size - 1; i++) {
				Bubble iBubble = list.get(i);
				for (int j = i + 1; j < size; j++) {
					Bubble jBubble = list.get(j);
					if (jBubble.centerY == iBubble.centerY) {
						double dx = jBubble.x - iBubble.x;
						double dy = jBubble.y - iBubble.y;
						double centersDistance = Math.hypot(dx, dy);
						float totalR = jBubble.r + iBubble.r + 5;
						if (centersDistance < totalR) {
							jBubble.x += (dx / centersDistance) * (totalR - centersDistance) * 0.5;
							jBubble.y += (dy / centersDistance) * (totalR - centersDistance) * 0.5;
							iBubble.x -= (dx / centersDistance) * (totalR - centersDistance) * 0.5;
							iBubble.y -= (dy / centersDistance) * (totalR - centersDistance) * 0.5;
						}
					}
				}
			}
		}
		double speed = layoutType == LAYOUT_COMMUNITIES_CLUSTERS ? 0.02 : 0.05;
		for (int i = 0; i < size; i++) {
			Bubble bubble = list.get(i);
			bubble.x -= (bubble.x - bubble.centerX) * speed;
			bubble.y -= (bubble.y - bubble.centerY) * speed;
		}
	}

	private void doDraw(Canvas canvas) {
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		for(Bubble b : bubbles.values()) {
			paint.setColor(b.communityId < Constants.BASE_COLORS.length ? Constants.BASE_COLORS[b.communityId] : Color.LTGRAY);
			float x = (b.x + offsetX) * zoomFactor;
			float y = (b.y + offsetY) * zoomFactor;
			float r = b.r * zoomFactor;
			canvas.drawCircle(x, y, r, paint);
			paint.setColor(Color.DKGRAY);
			float textsize = r * 0.3f;
			if(textsize > 16) {
				paint.setTextSize(textsize);
				textPath.reset();
				float innerR = r * 0.75f; 
				rectF.set(x - innerR, y - innerR, x + innerR, y + innerR);
				textPath.addArc(rectF, 135, 270);
				canvas.drawTextOnPath(RestClientV2.instance(getContext()).getPublicNickname(b.uid), textPath, 0, 0, paint);
				//canvas.drawPath(textPath, paint);
			}
		}
		paint.setColor(Color.WHITE);
		paint.setTextSize(46);
		canvas.drawText(dateStr, 150, 60, paint);
		//canvas.drawText(String.format("%.0f %.0f",  offsetX, offsetY), 200, 260, paint);
	}
	
	private static class Bubble {
		public float x, y;
		public float r;
		public int centerX;
		public int centerY;
		public Integer communityId;
		public String uid;
	}

	public void setBtBeans(long timestamp, List<BtNetworkBean> beans) {
		this.dateStr = DateTimeUtils.timestampToDateTime(timestamp).format("DD MMM YYYY", Locale.US);
		comm2index = new HashMap<Integer, Integer>();
		for (BtNetworkBean b : beans) {
			comm2index.put(b.community, comm2index.containsKey(b.community) ? comm2index.get(b.community) + 1 : 1);
		}
		ArrayList<Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>(comm2index.entrySet());
		Collections.sort(list, new Comparator<Entry<Integer, Integer>>() {

			@Override
			public int compare(Entry<Integer, Integer> lhs, Entry<Integer, Integer> rhs) {
				return rhs.getValue().compareTo(lhs.getValue());
			}
		});
		comm2index.clear();
		for (int i = 0; i < list.size(); i++) {
			comm2index.put(list.get(i).getKey(), i);
		}
		for (BtNetworkBean bean : beans) {
			if (bean.freq > 0) {
				float r = (float) (Math.sqrt(bean.freq) * 0.1f);
				if (bubbles.containsKey(bean.uid) == false) {
					float angle = MathUtils.random(0, 2 * PI);
					float x = (float) (CANVAS_W * 0.5 + CANVAS_W * 0.5 * Math.cos(angle));
					float y = (float) (CANVAS_H * 0.5 + CANVAS_H * 0.5 * Math.sin(angle));
					Bubble bubble = new Bubble();
					bubble.x = x;
					bubble.y = y;
					bubble.uid = bean.uid;
					bubbles.put(bean.uid, bubble);
				}
				bubbles.get(bean.uid).communityId = comm2index.get(bean.community);
				bubbles.get(bean.uid).r = r;
			}
		}
		setCenters();
	}

	private void setCenters() {
		if(layoutType == LAYOUT_SORT_BY_SIZE) {
			List<Bubble> l = new ArrayList<Bubble>(bubbles.values());
			Collections.sort(l, sizeComparator);
			float maxD = 2 * l.get(0).r;
			float marginLeft = 50;
			int bubblesPerRow = (int) ((CANVAS_W - 2 * marginLeft) / maxD);
			for (int i = 0; i < l.size(); i++) {
				l.get(i).centerX = (int) (marginLeft + maxD * (i % bubblesPerRow));
				l.get(i).centerY = (int) (150 + maxD * (i / bubblesPerRow));
			}
		} else {
			for(Bubble b : bubbles.values()) {
				b.centerX = (int) (CANVAS_W * 0.5 + 0.25 * CANVAS_W * Math.cos(b.communityId * 2 * PI / comm2index.size()));
				b.centerY = (int) (CANVAS_H * 0.5 + 0.25 * CANVAS_H * Math.sin(b.communityId * 2 * PI / comm2index.size()));
			}
		}
		updateCounter = STARTING_UPDATE_COUNTER;
		post(new Runnable() {

			@Override
			public void run() {
				if(updateCounter > 0) {
					update();
					post(this);
				}
				updateCounter--;
			}
		});
	}

	public void clearBubbles() {
		bubbles.clear();
	}

}
