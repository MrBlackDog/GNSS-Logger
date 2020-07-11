package com.artack.navigation;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.cts.nano.Ephemeris;
import android.location.cts.suplClient.SuplRrlpController;
import android.util.Log;

import com.google.android.apps.location.gps.gnsslogger.MainActivity;
import com.google.android.apps.location.gps.gnsslogger.WebSocketGolos;
import com.google.location.lbs.gnss.gps.pseudorange.GpsMeasurement;
import com.google.location.lbs.gnss.gps.pseudorange.GpsNavigationMessageStore;
import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;
import com.google.location.lbs.gnss.gps.pseudorange.SatellitePositionCalculator;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PseudorangeRelativePositionVelocityFromRealTimeEvents {

    private static final String TAG = "PseudorangeRelativePositionVelocityFromRealTimeEvents";
    //Supl Server and port
    private static final String SUPL_SERVER_NAME = "supl.google.com";
    private static final int SUPL_SERVER_PORT = 7276;

    private Ephemeris.GpsNavMessageProto mHardwareGpsNavMessageProto = null;
    // navigation message parser
    private GpsNavigationMessageStore mGpsNavigationMessageStore = new GpsNavigationMessageStore();

    /** Constants*/
    private static final int VALID_ACCUMULATED_DELTA_RANGE_STATE = 1;
    private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
    private static final int SECONDS_IN_WEEK = 604800;
    private static final double NANO_SECONDS_IN_WEEK = SECONDS_IN_WEEK*1e9;
    private static final double LEAST_SQUARE_TOLERANCE_METERS = 4.0e-8;
    private static final int C_TO_N0_THRESHOLD_DB_HZ = 18;
    private static final int TOW_DECODED_MEASUREMENT_STATE_BIT = 3;

    private int [] mReferenceLocationLLA = null;
    private double[] mReferenceLocationECEF = null;

    /**связаны с расчетом*/
    private boolean mFirstUsefulMeasurementSet = true;
    private int[] mReferenceLocation = null;
    private long mLastReceivedSuplMessageTimeMillis = 0;
    private long mDeltaTimeMillisToMakeSuplRequest = TimeUnit.MINUTES.toMillis(30);
    private boolean mFirstSuplRequestNeeded = true;
    private Ephemeris.GpsNavMessageProto mGpsNavMessageProtoUsed = null;

    private GpsMeasurement[] mUsefulSatellitesToReceiverMeasurements =
            new GpsMeasurement[GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES];
    private Long[] mUsefulSatellitesToTowNs =
            new Long[GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES];
    private long mLargestTowNs = Long.MIN_VALUE;
    private double mArrivalTimeSinceGPSWeekNs = 0.0;
    private int mDayOfYear1To366 = 0;
    private int mGpsWeekNumber = 0;
    private long mArrivalTimeSinceGpsEpochNs = 0;

    private String messageToSend;
    private String state = "Base";
    private boolean BaseReady;
    private boolean TargetReady;
    private boolean Completed;
    //public GpsMeasurement targetMeasurement;
    public SatteliteMeasurement[] targetMeasurement = new SatteliteMeasurement[GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES];
    public SatteliteMeasurement[] baseMeasurement = new SatteliteMeasurement[GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES];
    public PseudorangeRelativePositionVelocityFromRealTimeEvents() {
        WebSocketGolos.getMeasurementsCallBack = this::setTargetMeasurement;
    }

    public void computePositionVelocitySolutionsFromRawMeas(GnssMeasurementsEvent event) throws Exception {
        Completed = false;
        messageToSend = "";
        GnssClock gnssClock = event.getClock();
        /** НУЖНО*/
        //расчитать время приема T_Rx_GNSS
        double ReceivedTimeGNSS = gnssClock.getTimeNanos()   -
                (gnssClock.getFullBiasNanos() + gnssClock.getBiasNanos());

        //расчитать время приема Trx
        double WeekNumberNanos = Math.floor(-1* gnssClock.getFullBiasNanos()/(NANO_SECONDS_IN_WEEK))*NANO_SECONDS_IN_WEEK;
        double WeekNumber = WeekNumberNanos/NANO_SECONDS_IN_WEEK;
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
            if (measurement.getCn0DbHz() >= C_TO_N0_THRESHOLD_DB_HZ
                && (measurement.getState() & (1L << TOW_DECODED_MEASUREMENT_STATE_BIT)) != 0)
            {
                measurement.getTimeOffsetNanos();
                double Pseudorange = (ReceivedTimeGPS - measurement.getReceivedSvTimeNanos())*1e-9 *SPEED_OF_LIGHT_MPS;
                //Log.d("ArTack","PD " + String.valueOf(Pseudorange));
                SatteliteMeasurement satmeas = new SatteliteMeasurement(measurement.getSvid(),Pseudorange);
                baseMeasurement[measurement.getSvid()-1] = satmeas;
                messageToSend = String.valueOf(WeekNumber) + " " + String.valueOf(ReceivedTimeGPS);
                messageToSend = messageToSend.concat(measurement.getSvid() + " ");
                messageToSend = messageToSend.concat(Pseudorange + " ");
            }
        }
        if(state.equals("Target"))
        {
            MainActivity._ws.send("Diff:" + messageToSend);

        }
        Log.d("ArTack","Message " + String.valueOf(messageToSend));
        /*mArrivalTimeSinceGpsEpochNs = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos();
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
                mUsefulSatellitesToTowNs[measurement.getSvid() - 1] = receivedGPSTowNs;
                GpsMeasurement gpsReceiverMeasurement =
                        new GpsMeasurement(
                                (long) mArrivalTimeSinceGPSWeekNs,
                                measurement.getAccumulatedDeltaRangeMeters(),
                                measurement.getAccumulatedDeltaRangeState() == VALID_ACCUMULATED_DELTA_RANGE_STATE,
                                measurement.getPseudorangeRateMetersPerSecond(),
                                measurement.getCn0DbHz(),
                                measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                                measurement.getPseudorangeRateUncertaintyMetersPerSecond());
                mUsefulSatellitesToReceiverMeasurements[measurement.getSvid() - 1] = gpsReceiverMeasurement;
             //   double Pseudorange = (ReceivedTimeGPS - measurement.getReceivedSvTimeNanos())*1e-9 *SPEED_OF_LIGHT_MPS;
             //   Log.d("ArTack",String.valueOf(Pseudorange));
                //надо добавить отправку gpsReceiverMeasurement на базу

            }
        }*/
        // check if we should continue using the navigation message from the SUPL server, or use the
        // navigation message from the device if we fully received it
        boolean useNavMessageFromSupl =
                continueUsingNavMessageFromSupl(
                        mUsefulSatellitesToReceiverMeasurements, mHardwareGpsNavMessageProto);
        if (useNavMessageFromSupl) {
            Log.d(TAG, "Using navigation message from SUPL server");

            if (mFirstSuplRequestNeeded
                    || (System.currentTimeMillis() - mLastReceivedSuplMessageTimeMillis)
                    > mDeltaTimeMillisToMakeSuplRequest) {
                // The following line is blocking call for SUPL connection and back. But it is fast enough
                mGpsNavMessageProtoUsed = getSuplNavMessage(mReferenceLocation[0], mReferenceLocation[1]);
                if (!isEmptyNavMessage(mGpsNavMessageProtoUsed)) {
                    mFirstSuplRequestNeeded = false;
                    mLastReceivedSuplMessageTimeMillis = System.currentTimeMillis();
                } else {
                    return;
                }
            }

        } else {
            Log.d(TAG, "Using navigation message from the GPS receiver");
            mGpsNavMessageProtoUsed = mHardwareGpsNavMessageProto;
        }
        BaseReady = true;
        if(TargetReady && !Completed)
        {
            Completed = true;
            performRelativePositionVelocityComputationEcef(baseMeasurement,targetMeasurement,mGpsNavMessageProtoUsed,ReceivedTimeGPS,(int)WeekNumber);

        }
        // some times the SUPL server returns less satellites than the visible ones, so remove those
        // visible satellites that are not returned by SUPL
        /*for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
            if (mUsefulSatellitesToReceiverMeasurements[i] != null
                    && !navMessageProtoContainsSvid(mGpsNavMessageProtoUsed, i + 1)) {
                mUsefulSatellitesToReceiverMeasurements[i] = null;
                mUsefulSatellitesToTowNs[i] = null;
            }
        }

        // calculate the number of useful satellites
        int numberOfUsefulSatellites = 0;
        for (GpsMeasurement element : mUsefulSatellitesToReceiverMeasurements) {
            if (element != null) {
                numberOfUsefulSatellites++;
            }
        }*/

    }

    private void performRelativePositionVelocityComputationEcef(SatteliteMeasurement[] base,
                                                                SatteliteMeasurement[] target,
                                                                Ephemeris.GpsNavMessageProto navMessageProto,double ToW, int WeekNumber) throws Exception {
    SatellitePositionCalculator.PositionAndVelocity satPosAndVel =
            new SatellitePositionCalculator.PositionAndVelocity(0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0);
        SatellitePositionCalculator.RangeAndRangeRate rangeAndRangeRate = new SatellitePositionCalculator.RangeAndRangeRate(0,0);
        for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++)
        {
            Ephemeris.GpsEphemerisProto ephemeridesProto = getEphemerisForSatellite(navMessageProto, i + 1);
            SatellitePositionCalculator.calculateSatellitePositionAndVelocity(ephemeridesProto,ToW,WeekNumber,rangeAndRangeRate,satPosAndVel);//UserSatRangeAndRate eq 0 for no Sangak correction
        }

    }

    public void setTargetMeasurement(String ToW,String WeekNumber,SatteliteMeasurement[] targetMeasurement) {
        this.targetMeasurement = targetMeasurement;
        Log.d("ArTack","CallBack");
        TargetReady = true;
        if(BaseReady && !Completed)
        {
            Completed = true;
          //  performRelativePositionVelocityComputationEcef(baseMeasurement,targetMeasurement,mGpsNavMessageProtoUsed);

        }
        //this.targetMeasurement = targetMeasurement;
    }

    private boolean isEmptyNavMessage(Ephemeris.GpsNavMessageProto navMessageProto) {
        if(navMessageProto.iono == null)return true;
        if(navMessageProto.ephemerids.length ==0)return true;
        return  false;
    }

    private boolean navMessageProtoContainsSvid(Ephemeris.GpsNavMessageProto navMessageProto, int svid) {
        List<Ephemeris.GpsEphemerisProto> ephemeridesList =
                new ArrayList<Ephemeris.GpsEphemerisProto>(Arrays.asList(navMessageProto.ephemerids));
        for (Ephemeris.GpsEphemerisProto ephProtoFromList : ephemeridesList) {
            if (ephProtoFromList.prn == svid) {
                return true;
            }
        }
        return false;
    }
    /** Searches ephemerides list for the ephemeris associated with current satellite in process */
    private Ephemeris.GpsEphemerisProto getEphemerisForSatellite(Ephemeris.GpsNavMessageProto navMeassageProto,
                                                                 int satPrn) {
        List<Ephemeris.GpsEphemerisProto> ephemeridesList
                = new ArrayList<Ephemeris.GpsEphemerisProto>(Arrays.asList(navMeassageProto.ephemerids));
        Ephemeris.GpsEphemerisProto ephemeridesProto = null;
        int ephemerisPrn = 0;
        for (Ephemeris.GpsEphemerisProto ephProtoFromList : ephemeridesList) {
            ephemerisPrn = ephProtoFromList.prn;
            if (ephemerisPrn == satPrn) {
                ephemeridesProto = ephProtoFromList;
                break;
            }
        }
        return ephemeridesProto;
    }
    /**
     * Reads the navigation message from the SUPL server by creating a Stubby client to Stubby server
     * that wraps the SUPL server. The input is the time in nanoseconds since the GPS epoch at which
     * the navigation message is required and the output is a {@link Ephemeris.GpsNavMessageProto}
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    private Ephemeris.GpsNavMessageProto getSuplNavMessage(long latE7, long lngE7)
            throws UnknownHostException, IOException {
        SuplRrlpController suplRrlpController =
                new SuplRrlpController(SUPL_SERVER_NAME, SUPL_SERVER_PORT);
        Ephemeris.GpsNavMessageProto navMessageProto = suplRrlpController.generateNavMessage(latE7, lngE7);

        return navMessageProto;
    }

    /**
     * Checks if we should continue using the navigation message from the SUPL server, or use the
     * navigation message from the device if we fully received it. If the navigation message read from
     * the receiver has all the visible satellite ephemerides, return false, otherwise, return true.
     */
    private static boolean continueUsingNavMessageFromSupl(
            GpsMeasurement[] usefulSatellitesToReceiverMeasurements,
            Ephemeris.GpsNavMessageProto hardwareGpsNavMessageProto) {
        boolean useNavMessageFromSupl = true;
        if (hardwareGpsNavMessageProto != null) {
            ArrayList<Ephemeris.GpsEphemerisProto> hardwareEphemeridesList=
                    new ArrayList<Ephemeris.GpsEphemerisProto>(Arrays.asList(hardwareGpsNavMessageProto.ephemerids));
            if (hardwareGpsNavMessageProto.iono != null) {
                for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
                    if (usefulSatellitesToReceiverMeasurements[i] != null) {
                        int prn = i + 1;
                        for (Ephemeris.GpsEphemerisProto hardwareEphProtoFromList : hardwareEphemeridesList) {
                            if (hardwareEphProtoFromList.prn == prn) {
                                useNavMessageFromSupl = false;
                                break;
                            }
                            useNavMessageFromSupl = true;
                        }
                        if (useNavMessageFromSupl == true) {
                            break;
                        }
                    }
                }
            }
        }
        return useNavMessageFromSupl;
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
