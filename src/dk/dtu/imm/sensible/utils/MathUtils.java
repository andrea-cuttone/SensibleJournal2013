package dk.dtu.imm.sensible.utils;

import android.graphics.Color;

public class MathUtils {
	
	public static double interpolateXY(double x, double x1, double x2, double y1, double y2) {
		return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
	}
	
	public static float interpolate(float a, float b, float proportion) {
		return (a + ((b - a) * proportion));
	}
	
	public static int interpolateColors(int [] cols, float proportion) {
		int index_a = (int) Math.floor(proportion * cols.length);
		int index_b = (int) Math.ceil(proportion * cols.length);
		float p = 1f;
		return interpolateColor(index_a, index_b, p);
	}

	public static int interpolateColor(int a, int b, float proportion) {
		float[] hsva = new float[3];
		float[] hsvb = new float[3];
		Color.colorToHSV(a, hsva);
		Color.colorToHSV(b, hsvb);
		for (int i = 0; i < 3; i++) {
			hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
		}
		return Color.HSVToColor(hsvb);
	}
	
	public static float roundToMultiple(double base, double multiple) {
		return (float) (Math.round(base / multiple) * multiple);
	}

	public static float random(float min, float max) {
		return (float) (min + (max - min) * Math.random());
	}

	public static int randomValueInSet(int... x) {
		return x[(int) random(0, x.length - 1)];
	}
	
}
