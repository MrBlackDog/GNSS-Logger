package com.artack.navigation;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.GnssContainer;
import com.google.android.apps.location.gps.gnsslogger.GnssListener;
import com.google.android.apps.location.gps.gnsslogger.ResultFragment;
import com.google.location.lbs.gnss.gps.pseudorange.Ecef2LlaConverter;
import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;
import com.google.location.lbs.gnss.gps.pseudorange.Lla2EcefConverter;
import com.google.location.lbs.gnss.gps.pseudorange.PseudorangePositionVelocityFromRealTimeEvents;
import com.artack.navigation.RelativeNavigationFragment.UIRelativeResultComponent;
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

    private double[] mReferenceLocation = null;
    private double[] mReferenceLocationECEF = null;
    /**связаны с расчетом*/
    private double mArrivalTimeSinceGPSWeekNs = 0.0;
    private int mDayOfYear1To366 = 0;
    private int mGpsWeekNumber = 0;
    private long mArrivalTimeSinceGpsEpochNs = 0;
    private long mLargestTowNs = Long.MIN_VALUE;

    private HandlerThread mRelativePositionVelocityCalculationHandlerThread;
    private Handler mRelativePositionVelocityCalculationHandler;
    private PseudorangeRelativePositionVelocityFromRealTimeEvents mPseudorangeRelativePositionVelocityFromRealTimeEvents;

    GnssMeasurement currentmeasurementBase;
    GnssMeasurement currentmeasurementObject;
    GnssClock currentclockBase;
    GnssClock currentclockObject;
    double[] positionSolutionECEF;
    Matrix GradientMatrix;//Used Jama matrix class
    RealMatrix HMatrix;
    RealMatrix SolutionMatrix;

    public RealTimeRelativePositionCalculator() {
        mRelativePositionVelocityCalculationHandlerThread =
                new HandlerThread("Relative Position From Realtime Pseudoranges");
        mRelativePositionVelocityCalculationHandlerThread.start();
        mRelativePositionVelocityCalculationHandler =
                new Handler(mRelativePositionVelocityCalculationHandlerThread.getLooper());

        final Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mPseudorangeRelativePositionVelocityFromRealTimeEvents =
                                    new PseudorangeRelativePositionVelocityFromRealTimeEvents();
                        } catch (Exception e) {
                            Log.e(
                                    GnssContainer.TAG,
                                    " Exception in constructing PseudorangeRelativePositionFromRealTimeEvents : ",
                                    e);
                        }
                    }
                };

        mRelativePositionVelocityCalculationHandler.post(r);
    }
    /** Iterative WLS method for relative navigation solution*/
    public void WLSSolution()
    {

    }

    /** Sets a rough location of the receiver that can be used to request SUPL assistance data */
    public void setReferencePosition(double lat, double lng, double alt) {
        if (mReferenceLocation == null) {
            mReferenceLocation = new double[3];
            mReferenceLocationECEF = new double[3];
        }
        mReferenceLocation[0] = lat;
        mReferenceLocation[1] = lng;
        mReferenceLocation[2] = alt;
        Ecef2LlaConverter.GeodeticLlaValues geodeticLlaValues = new Ecef2LlaConverter.GeodeticLlaValues(lat,lng,alt);
        mReferenceLocationECEF = Lla2EcefConverter.convertFromLlaToEcefMeters(geodeticLlaValues);
        //тут не работает
        Log.e("RefLocation:",String.valueOf(mReferenceLocationECEF[0]) +" " +
                String.valueOf(mReferenceLocationECEF[1])+ " " +
                String.valueOf(mReferenceLocationECEF[2]));
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /**Отсюда можно получить первое решение, для получения хорошего начального приближения*/
    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)){
            if(mReferenceLocationECEF == null)
            setReferencePosition(location.getLatitude(),location.getLongitude(),location.getAltitude());
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    }

    /**тут надо замутить магию */
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        //возможно стоит все передавать в отедльный класс для отрешивания.

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

    public double[] getReferenceLocationECEF() {
        return mReferenceLocationECEF;
    }
    public void postToast(String mes)
    {
     /*   Toast.makeText(getContext(),realTimeRelativePositionCalculator.positionSolutionECEF[0] + " "+
                        realTimeRelativePositionCalculator.positionSolutionECEF[1] + " "+
                        realTimeRelativePositionCalculator.positionSolutionECEF[2] + " "
                ,Toast.LENGTH_SHORT).show();*/
    }

    /**UI для вывода результатов*/
    private UIRelativeResultComponent uiResultComponent;

    public synchronized UIRelativeResultComponent getUiResultComponent() {
        return uiResultComponent;
    }

    public synchronized void setUiResultComponent(UIRelativeResultComponent value) {
        uiResultComponent = value;
    }
}
