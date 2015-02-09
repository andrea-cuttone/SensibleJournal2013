package dk.dtu.imm.sensible.movement.renderers;

import java.util.List;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.maps.MapView;

import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public abstract class CanvasRenderer {
	private SurfaceView surfaceView;
	private MapView mapView;

	public CanvasRenderer(SurfaceView surfaceView, MapView mapView) {
		this.surfaceView = surfaceView;
		this.mapView = mapView;
	}
	
	public void render(int frameIndex) {
		if (frameIndex < 0 || frameIndex > getTotalFrames()) {
			return;
		}
		Canvas c = null;
		try {
			c = surfaceView.getHolder().lockCanvas(null);
			if(c != null) {
				doDraw(c, frameIndex);
			}
		} finally {
			if (c != null) {
				surfaceView.getHolder().unlockCanvasAndPost(c);
			}
		}
	}
	
	public MapView getMapView() {
		return mapView;
	}
	
	public abstract void initLocations(List<LocationResponseEntity> locations);
	protected abstract void doDraw(Canvas c, int frameIndex);
	public abstract int getFrameDuration();
	public abstract int getTotalFrames();

}