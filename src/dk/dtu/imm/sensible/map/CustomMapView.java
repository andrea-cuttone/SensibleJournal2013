package dk.dtu.imm.sensible.map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class CustomMapView extends MapView {

    private int oldZoomLevel = -1;
    private GeoPoint oldCenterGeoPoint;
    private MapEventsListener listener;

    public CustomMapView(Context context, String apiKey) {
        super(context, apiKey);
    }

    public CustomMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMapEventsListener(MapEventsListener listener) {
        this.listener = listener;
    }

    @Override
	public boolean onTouchEvent(MotionEvent ev) {
		GeoPoint centerGeoPoint = this.getMapCenter();
		if (oldCenterGeoPoint == null 
				|| (oldCenterGeoPoint.getLatitudeE6() != centerGeoPoint.getLatitudeE6())
				|| (oldCenterGeoPoint.getLongitudeE6() != centerGeoPoint.getLongitudeE6())) {
			if (listener != null) {
				listener.onPan();
			}
		}
		oldCenterGeoPoint = this.getMapCenter();
		return super.onTouchEvent(ev);
	}

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (getZoomLevel() != oldZoomLevel) {
        	if(listener != null) {
        		listener.onZoom();
        	}
            oldZoomLevel = getZoomLevel();
        }
    }

}