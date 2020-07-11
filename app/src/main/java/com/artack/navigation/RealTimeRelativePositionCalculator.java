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
import com.google.android.apps.location.gps.gnsslogger.MainActivity;
import com.google.android.apps.location.gps.gnsslogger.ResultFragment;
import com.google.android.apps.location.gps.gnsslogger.WebSocketGolos;
import com.google.location.lbs.gnss.gps.pseudorange.Ecef2LlaConverter;
import com.google.location.lbs.gnss.gps.pseudorange.GpsMathOperations;
import com.google.location.lbs.gnss.gps.pseudorange.GpsMeasurement;
import com.google.location.lbs.gnss.gps.pseudorange.GpsNavigationMessageStore;
import com.google.location.lbs.gnss.gps.pseudorange.GpsTime;
import com.google.location.lbs.gnss.gps.pseudorange.Lla2EcefConverter;
import com.google.location.lbs.gnss.gps.pseudorange.PseudorangePositionVelocityFromRealTimeEvents;
import com.artack.navigation.RelativeNavigationFragment.UIRelativeResultComponent;
import Jama.Matrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class RealTimeRelativePositionCalculator implements GnssListener {

     private double[] mReferenceLocation = null;
    private double[] mReferenceLocationECEF = null;

    private HandlerThread mRelativePositionVelocityCalculationHandlerThread;
    private Handler mRelativePositionVelocityCalculationHandler;
    // класс для расчета дальностей и прочего
    private PseudorangeRelativePositionVelocityFromRealTimeEvents mPseudorangeRelativePositionVelocityFromRealTimeEvents;
    public SatteliteMeasurement[] targetMeasurement;
    double[] positionSolutionECEF;
    private String Xd;
    private String state = "Base";

    public String getState() {
        return state;
    }

    public RealTimeRelativePositionCalculator() {
        WebSocketGolos.getMeasurementsCallBack = this::setTargetMeasurement;
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

   /* /** Sets a rough location of the receiver that can be used to request SUPL assistance data
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
    }*/

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /**Отсюда можно получить первое решение, для получения хорошего начального приближения*/
    @Override
    public void onLocationChanged(final Location location) {
        if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            final Runnable r =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mPseudorangeRelativePositionVelocityFromRealTimeEvents == null) {
                                return;
                            }
                            try {
                                mPseudorangeRelativePositionVelocityFromRealTimeEvents.setReferencePosition(
                                        (int) (location.getLatitude() * 1E7),
                                        (int) (location.getLongitude() * 1E7),
                                        (int) (location.getAltitude() * 1E7));
                            } catch (Exception e) {
                                Log.e(GnssContainer.TAG, " Exception setting reference location : ", e);
                            }
                        }
                    };

            mRelativePositionVelocityCalculationHandler.post(r);
        }
     /*   if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            if (mReferenceLocationECEF == null)
                setReferencePosition(
                        (int) (location.getLatitude() * 1E7),
                        (int) (location.getLongitude() * 1E7),
                        (int) (location.getAltitude() * 1E7));
        }*/
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    }

    /**тут надо замутить магию */
    @Override
    public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
        //возможно стоит все передавать в отедльный класс для отрешивания.
        final Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        if (mPseudorangeRelativePositionVelocityFromRealTimeEvents == null) {
                            return;
                        }
                        try {
                            if(state.equals("Target"))
                            {
                                for (GnssMeasurement measurement : event.getMeasurements())
                                {
                                    final String format = "   %-4s = %s\n";
                                    final StringBuilder builder = new StringBuilder();
                                    if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS ) {
                                        builder.append("GnssMeasurement \n");

                                        DecimalFormat numberFormat = new DecimalFormat("#0.000");
                                        DecimalFormat numberFormat1 = new DecimalFormat("#0.000E00");

                                        builder.append(String.format(format, "Svid", measurement.getSvid()));
                                        builder.append(String.format(format, "ConstellationType", measurement.getConstellationType()));
                                        builder.append(String.format(format, "TimeOffsetNanos", measurement.getTimeOffsetNanos()));

                                        builder.append(String.format(format, "State", measurement.getState()));

                                        builder.append(
                                                String.format(format, "ReceivedSvTimeNanos", measurement.getReceivedSvTimeNanos()));
                                        builder.append(
                                                String.format(
                                                        format,
                                                        "ReceivedSvTimeUncertaintyNanos",
                                                        measurement.getReceivedSvTimeUncertaintyNanos()));

                                        builder.append(String.format(format, "Cn0DbHz", numberFormat.format(measurement.getCn0DbHz())));

                                        builder.append(
                                                String.format(
                                                        format,
                                                        "PseudorangeRateMetersPerSecond",
                                                        numberFormat.format(measurement.getPseudorangeRateMetersPerSecond())));
                                        builder.append(
                                                String.format(
                                                        format,
                                                        "PseudorangeRateUncertaintyMetersPerSeconds",
                                                        numberFormat.format(measurement.getPseudorangeRateUncertaintyMetersPerSecond())));

                                        if (measurement.getAccumulatedDeltaRangeState() != 0) {
                                            builder.append(
                                                    String.format(
                                                            format, "AccumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState()));

                                            builder.append(
                                                    String.format(
                                                            format,
                                                            "AccumulatedDeltaRangeMeters",
                                                            numberFormat.format(measurement.getAccumulatedDeltaRangeMeters())));
                                            builder.append(
                                                    String.format(
                                                            format,
                                                            "AccumulatedDeltaRangeUncertaintyMeters",
                                                            numberFormat1.format(measurement.getAccumulatedDeltaRangeUncertaintyMeters())));
                                        }

                                        if (measurement.hasCarrierFrequencyHz()) {
                                            builder.append(
                                                    String.format(format, "CarrierFrequencyHz", measurement.getCarrierFrequencyHz()));
                                        }

                                        if (measurement.hasCarrierCycles()) {
                                            builder.append(String.format(format, "CarrierCycles", measurement.getCarrierCycles()));
                                        }

                                        if (measurement.hasCarrierPhase()) {
                                            builder.append(String.format(format, "CarrierPhase", measurement.getCarrierPhase()));
                                        }

                                        if (measurement.hasCarrierPhaseUncertainty()) {
                                            builder.append(
                                                    String.format(
                                                            format, "CarrierPhaseUncertainty", measurement.getCarrierPhaseUncertainty()));
                                        }

                                        builder.append(
                                                String.format(format, "MultipathIndicator", measurement.getMultipathIndicator()));

                                        if (measurement.hasSnrInDb()) {
                                            builder.append(String.format(format, "SnrInDb", measurement.getSnrInDb()));
                                        }

                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            if (measurement.hasAutomaticGainControlLevelDb()) {
                                                builder.append(
                                                        String.format(format, "AgcDb", measurement.getAutomaticGainControlLevelDb()));
                                            }
                                            if (measurement.hasCarrierFrequencyHz()) {
                                                builder.append(String.format(format, "CarrierFreqHz", measurement.getCarrierFrequencyHz()));
                                            }
                                        }
                                    }
                                    MainActivity._ws.send("Diff:"+ builder.toString());
                                }

                            }

                            /** пока не знаю, стоит ли оставлять это условие и что с ним делать */
                           /* if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
                                    && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                // The position at last epoch is used for the residual analysis.
                                // This is happening by updating the ground truth for pseudorange before using the
                                // new arriving pseudoranges to compute a new position.
                                mPseudorangeRelativePositionVelocityFromRealTimeEvents
                                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
                            }*/
                           /** сюда ещё надо прикурить измерения с целевого приемника,так как отрешиваемся на базе*/
                            mPseudorangeRelativePositionVelocityFromRealTimeEvents
                                    .computePositionVelocitySolutionsFromRawMeas(event);
                            /**вообще хз че это, лень думать */
                            // Running on main thread instead of in parallel will improve the thread safety
                         /*   if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED) {
                                mMainActivity.runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                mPlotFragment.updatePseudorangeResidualTab(
                                                        mPseudorangePositionVelocityFromRealTimeEvents
                                                                .getPseudorangeResidualsMeters(),
                                                        TimeUnit.NANOSECONDS.toSeconds(
                                                                event.getClock().getTimeNanos()));
                                            }
                                        }
                                );
                            }*/
                            /**вообще хз че это, лень думать */
                            /*else {
                                mMainActivity.runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                // Here we create gaps when the residual plot is disabled
                                                mPlotFragment.updatePseudorangeResidualTab(
                                                        GpsMathOperations.createAndFillArray(
                                                                GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES, Double.NaN),
                                                        TimeUnit.NANOSECONDS.toSeconds(
                                                                event.getClock().getTimeNanos()));
                                            }
                                        }
                                );
                            }*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
        mRelativePositionVelocityCalculationHandler.post(r);

    }

    public void setTargetMeasurement(SatteliteMeasurement[] targetMeasurement) {
        this.targetMeasurement = targetMeasurement;
        Log.d("CallBack","XD");
        //this.targetMeasurement = targetMeasurement;
    }

    public SatteliteMeasurement[] getTargetMeasurement() {
        return targetMeasurement;
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
    }

    /**получает навигационное сообщение, чтобы достать эфимериды */
    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) {
            mPseudorangeRelativePositionVelocityFromRealTimeEvents.parseHwNavigationMessageUpdates(event);
        }
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
