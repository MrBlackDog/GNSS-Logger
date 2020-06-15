package com.artack.navigation;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.cts.nano.Ephemeris;
import android.util.Log;

import com.google.location.lbs.gnss.gps.pseudorange.GpsNavigationMessageStore;
import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;

import java.util.Calendar;

public class PseudorangeRelativePositionVelocityFromRealTimeEvents {

    private Ephemeris.GpsNavMessageProto mHardwareGpsNavMessageProto = null;

    // navigation message parser
    private GpsNavigationMessageStore mGpsNavigationMessageStore = new GpsNavigationMessageStore();

    /** Constants*/
    private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
    private static final int SECONDS_IN_WEEK = 604800;
    private static final double NANO_SECONDS_IN_WEEK = SECONDS_IN_WEEK*1e9;
    private static final double LEAST_SQUARE_TOLERANCE_METERS = 4.0e-8;
    private static final int C_TO_N0_THRESHOLD_DB_HZ = 18;
    private static final int TOW_DECODED_MEASUREMENT_STATE_BIT = 3;

    private int [] mReferenceLocationLLA = null;
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
        /** НУЖНО*/
        //расчитать время приема T_Rx_GNSS
        double ReceivedTimeGNSS = gnssClock.getTimeNanos() + gnssClock.getTimeUncertaintyNanos() -
                (gnssClock.getFullBiasNanos() + gnssClock.getBiasNanos());
        mArrivalTimeSinceGpsEpochNs = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos();

        //расчитать время приема Trx
        double WeekNumberNanos = Math.floor(-1* gnssClock.getFullBiasNanos()/(NANO_SECONDS_IN_WEEK))*NANO_SECONDS_IN_WEEK;

        //T_Rx
        double ReceivedTimeGPS = ReceivedTimeGNSS - WeekNumberNanos;
        //Найти ПД для каждого спутника, который подходит по условиям.
        for (GnssMeasurement measurement : event.getMeasurements())
        {
            // ignore any measurement if it is not from GPS constellation
            if (measurement.getConstellationType() != GnssStatus.CONSTELLATION_GPS)
            {
                continue;
            }
            // ignore raw data if time is zero, if signal to noise ratio is below threshold or if
            // TOW is not yet decoded
            if (measurement.getCn0DbHz() >= C_TO_N0_THRESHOLD_DB_HZ
                    && (measurement.getState() & (1L << TOW_DECODED_MEASUREMENT_STATE_BIT)) != 0)
            {
                double Pseudorange = (ReceivedTimeGPS - measurement.getReceivedSvTimeNanos())*1e-9 *SPEED_OF_LIGHT_MPS;
                Log.d("ArTack",String.valueOf(Pseudorange));
            }
        }
            //double WeekNumberNanos = Math.floor(-1* gnssClock.getFullBiasNanos()/(SECONDS_IN_WEEK*1e9))*
        //GpsTime
       /*
// calculate day of year and Gps week number needed for the least square
        GpsTime gpsTime = new GpsTime(mArrivalTimeSinceGpsEpochNs);
        // Gps weekly epoch in Nanoseconds: defined as of every Sunday night at 00:00:000
        long gpsWeekEpochNs = GpsTime.getGpsWeekEpochNano(gpsTime);
        mArrivalTimeSinceGPSWeekNs = mArrivalTimeSinceGpsEpochNs - gpsWeekEpochNs;
        //
        mGpsWeekNumber = gpsTime.getGpsWeekSecond().first;

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
                //
                mGpsWeekNumber = gpsTime.getGpsWeekSecond().first;
                // calculate day of the year between 1 and 366
                Calendar cal = gpsTime.getTimeInCalendar();
                mDayOfYear1To366 = cal.get(Calendar.DAY_OF_YEAR);
                //Время передачи tTx
                long receivedGPSTowNs = measurement.getReceivedSvTimeNanos();
                //посчитать псевдодальности
                if (receivedGPSTowNs > mLargestTowNs) {
                    mLargestTowNs = receivedGPSTowNs;
                }
            }
        }*/
    }

    /** Sets a rough location of the receiver that can be used to request SUPL assistance data */
    public void setReferencePosition(int latE7, int lngE7, int altE7) {
        if (mReferenceLocationLLA == null) {
            mReferenceLocationLLA = new int[3];
        }
        mReferenceLocationLLA[0] = latE7;
        mReferenceLocationLLA[1] = lngE7;
        mReferenceLocationLLA[2] = altE7;
    }

    /**
     * Parses a string array containing an updates to the navigation message and return the most
     * recent {@link Ephemeris.GpsNavMessageProto}.
     */
    public void parseHwNavigationMessageUpdates(GnssNavigationMessage navigationMessage) {
        byte messagePrn = (byte) navigationMessage.getSvid();
        byte messageType = (byte) (navigationMessage.getType() >> 8);
        int subMessageId = navigationMessage.getSubmessageId();

        byte[] messageRawData = navigationMessage.getData();
        // parse only GPS navigation messages for now
        if (messageType == 1) {
            mGpsNavigationMessageStore.onNavMessageReported(
                    messagePrn, messageType, (short) subMessageId, messageRawData);
            mHardwareGpsNavMessageProto = mGpsNavigationMessageStore.createDecodedNavMessage();
        }

    }
}
