package com.artack.navigation;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Bundle;

import com.google.android.apps.location.gps.gnsslogger.GnssListener;
import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;

import Jama.Matrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Calendar;

public class RealTimeRelativePositionCalculator implements GnssListener {


    /** Constants*/
    private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
    private static final int SECONDS_IN_WEEK = 604800;
    private static final double LEAST_SQUARE_TOLERANCE_METERS = 4.0e-8;
    private static final int C_TO_N0_THRESHOLD_DB_HZ = 18;
    private static final int TOW_DECODED_MEASUREMENT_STATE_BIT = 3;

    private int[] mReferenceLocation = null;
    /**связаны с расчетом*/
    private double mArrivalTimeSinceGPSWeekNs = 0.0;
    private int mDayOfYear1To366 = 0;
    private int mGpsWeekNumber = 0;
    private long mArrivalTimeSinceGpsEpochNs = 0;
    private long mLargestTowNs = Long.MIN_VALUE;

    GnssMeasurement currentmeasurementBase;
    GnssMeasurement currentmeasurementObject;
    GnssClock currentclockBase;
    GnssClock currentclockObject;
    double[] positionSolutionECEF;
    Matrix GradientMatrix;//Used Jama matrix class
    RealMatrix HMatrix;

    public RealTimeRelativePositionCalculator(GnssMeasurement measurement) {
    }

    public void WLSSolution()
    {

    }

    /** Sets a rough location of the receiver that can be used to request SUPL assistance data */
    public void setReferencePosition(int latE7, int lngE7, int altE7) {
        if (mReferenceLocation == null) {
            mReferenceLocation = new int[3];
        }
        mReferenceLocation[0] = latE7;
        mReferenceLocation[1] = lngE7;
        mReferenceLocation[2] = altE7;
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    }

    /**тут надо замутить магию */
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
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

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
    }

    @Override
    public void onNmeaReceived(long l, String s) {
    }

    @Override
    public void onTTFFReceived(long l) {
    }

}
