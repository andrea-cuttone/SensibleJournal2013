package dk.dtu.imm.sensible;

import android.graphics.Color;
import hirondelle.date4j.DateTime;

public class Constants {

	public final static String APP_NAME = "SensibleJournal";

	public static final double DTU_LAT = 55.785696;
	public static final double DTU_LON = 12.521654;
	
	public static final String ANDREA_UID_OLD = "351565052633002";
	public static final String ANDREA_UID = "351565052631394";
	
	public static final int VEHICLE_SPEED = 3;
	public static final double WALKING_SPEED = 0.5;
	
	public static final DateTime START_DATE = DateTime.forDateOnly(2012, 11, 21);
	public static final DateTime EARLIEST_DATE = DateTime.forDateOnly(2012, 10, 1);
	
	public static final long SECS_ONE_HOUR = 60 * 60;
	public static final long SECS_ONE_DAY = SECS_ONE_HOUR * 24;
	public static final long SECS_ONE_WEEK = SECS_ONE_DAY * 7;
	
	public static final float PI = (float) Math.PI;
	
	public static final int [] BASE_COLORS = { 
		Color.rgb(141, 211, 199),
		Color.rgb( 255, 255, 179),
		Color.rgb( 190, 186, 218),
		Color.rgb( 251, 128, 114),
		Color.rgb( 128, 177, 211),
		Color.rgb( 253, 180, 98),
		Color.rgb( 179, 222, 105),
		Color.rgb( 252, 205, 229),
		Color.rgb( 217, 217, 217),
		Color.rgb( 188, 128, 189),
		Color.rgb( 204, 235, 197),
		Color.rgb( 255, 237, 111)
	};
}
