package dk.dtu.imm.sensible.movement.renderers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.SurfaceView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import dk.dtu.imm.sensible.map.LocationHelper;
import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.movement.RenderHelper;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.stats.SpeedStats;
import dk.dtu.imm.sensible.stats.StatsCalculator;
import dk.dtu.imm.sensible.stats.TimeAtLocation;
import dk.dtu.imm.sensible.utils.MathUtils;

public class AnimMovementRenderer extends CanvasRenderer {

	private static final int TRAIL_LENGTH = 20;

	private Paint paint;
	private BlurMaskFilter blurFilter;
	private RenderHelper renderHelper;
	private SimpleDateFormat dateFormat;
	private GregorianCalendar cal;
	private Point pt1 = new Point();
	private Point pt2 = new Point();
	private ComposePathEffect composePathEffect;
	private Path arrowShape;
	private int colStaticMov;

	private List<LocationResponseEntity> locations;
	private List<LocationResponseEntity> interpolated;

	public AnimMovementRenderer(SurfaceView surfaceView, MapView mapView) {
		super(surfaceView, mapView);
		paint = new Paint();
		paint.setAntiAlias(true);
		blurFilter = new BlurMaskFilter(10.0f, Blur.SOLID);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		renderHelper = new RenderHelper();
		cal = new GregorianCalendar();
		dateFormat = new SimpleDateFormat("HH:mm");
        arrowShape = new Path();
		arrowShape.moveTo(8, 0);
        arrowShape.lineTo(0, -6);
        arrowShape.lineTo(28, -6);
        arrowShape.lineTo(36, 0);
        arrowShape.lineTo(28, 6);
        arrowShape.lineTo(0, 6);
        CornerPathEffect cornerPathEffect = new CornerPathEffect(10);
        PathDashPathEffect dashPathEffect = new PathDashPathEffect(arrowShape, 36, 0,
        		PathDashPathEffect.Style.ROTATE);
        composePathEffect = new ComposePathEffect(dashPathEffect, cornerPathEffect);
        colStaticMov = Color.argb(96, 33, 244, 235);
	}

	@Override
	protected void doDraw(Canvas canvas, int frameIndex) {
		if(interpolated == null) {
			return; 
		}
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		drawStatic(canvas);
		Projection projection = getMapView().getProjection();
		paint.setStrokeWidth(25);
		projection.toPixels(interpolated.get(frameIndex).getGeoPoint(), pt1);
		paint.setColor(Color.RED);
		paint.setMaskFilter(blurFilter);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(pt1.x, pt1.y, 15, paint);
		for (int j = frameIndex; j > frameIndex - TRAIL_LENGTH && j > 0; j--) {
			projection.toPixels(interpolated.get(j).getGeoPoint(), pt1);
			projection.toPixels(interpolated.get(j-1).getGeoPoint(), pt2);
			int a = (int) MathUtils.interpolateXY(j, frameIndex - TRAIL_LENGTH, frameIndex, 16, 128);
			paint.setColor(Color.argb(a, 255, 0, 0));
			canvas.drawLine(pt1.x, pt1.y, pt2.x, pt2.y, paint);
		}
		cal.setTimeInMillis(interpolated.get(frameIndex).getTime() * 1000);
		renderHelper.renderText(canvas, dateFormat.format(cal.getTime()), 500, 85);
	}

	private void drawStatic(Canvas canvas) {
		Projection projection = getMapView().getProjection();
		paint.setMaskFilter(null);
		paint.setColor(colStaticMov);
		paint.setStrokeWidth(22);
		paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(composePathEffect);
		Path path = new Path();
		projection.toPixels(locations.get(0).getGeoPoint(), pt1);
		path.moveTo(pt1.x, pt1.y);
		for (int j = 1; j < locations.size(); j++) {
			projection.toPixels(locations.get(j).getGeoPoint(), pt2);
			path.lineTo(pt2.x, pt2.y);
		}
		canvas.drawPath(path, paint);
		paint.setPathEffect(null);
		paint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	public void initLocations(List<LocationResponseEntity> locations) {
		this.locations = locations;
		this.interpolated = LocationProcessor.interpolateLocs(locations);
	}

	@Override
	public int getFrameDuration() {
		return 50;
	}

	@Override
	public int getTotalFrames() {
		return interpolated == null ? 0 : interpolated.size() - 1;
	}

}
