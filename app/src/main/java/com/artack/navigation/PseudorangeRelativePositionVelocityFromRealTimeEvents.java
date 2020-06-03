package com.artack.navigation;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;

import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;

import java.util.Calendar;

public class PseudorangeRelativePositionVelocityFromRealTimeEvents {

    /** Constants*/
    private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
    private static final int SECONDS_IN_WEEK = 604800;
    private static final double LEAST_SQUARE_TOLERANCE_METERS = 4.0e-8;
    private static final int C_TO_N0_THRESHOLD_DB_HZ = 18;
    private static final int TOW_DECODED_MEASUREMENT_STATE_BIT = 3;

    private double[] mReferenceLocation = null;
    private double[] mReferenceLocationECEF = null;
    /**связаны с расчетом*/
    private double mArrivalTimeSinceGPSWeekNs = 0.0;
    private int mDayOfYear1To366 = 0;
    private int mGpsWeekNumber = 0;
    private long mArrivalTimeSinceGpsEpochNs = 0;
    private long mLargestTowNs = Long.MIN_VALUE;


    public void computePositionVelocitySolutionsFromRawMeas(GnssMeasurementsEvent event)
    {
        GnssClock gnssClock = event.getClock();
        mArrivalTimeSinceGpsEpochNs = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos();

        for (GnssMeasurement measurement : event.getMeasurements()) {
            // ignore any measurement if it is not from GPS constellation
            if (measurement.getConstellationType() != GnssStatus.CONSTELLATION_GPS) {
                continue;
            }
            // ignore raw data if time is zero, if signal to noise ratio is below threshold or if
            // TOW is not yet decoded
            if (measurement.getCn0DbHz() >= C_TO_N0_THRESHOLD_DB_HZ
                    && (measurement.getState() & (1L << TOW_DECODED_MEASUREMENT_STATE_BIT)) != 0) {

                // calculate day of year and Gps week number needed for the least square
                GpsTime gpsTime = new GpsTime(mArrivalTimeSinceGpsEpochNs);
                // Gps weekly epoch in Nanoseconds: defined as of every Sunday night at 00:00:000
                long gpsWeekEpochNs = GpsTime.getGpsWeekEpochNano(gpsTime);
                mArrivalTimeSinceGPSWeekNs = mArrivalTimeSinceGpsEpochNs - gpsWeekEpochNs;
                mGpsWeekNumber = gpsTime.getGpsWeekSecond().first;
                // calculate day of the year between 1 and 366
                Calendar cal = gpsTime.getTimeInCalendar();
                mDayOfYear1To366 = cal.get(Calendar.DAY_OF_YEAR);

                long receivedGPSTowNs = measurement.getReceivedSvTimeNanos();
                if (receivedGPSTowNs > mLargestTowNs) {
                    mLargestTowNs = receivedGPSTowNs;
                }
            }
        }
    }

}
