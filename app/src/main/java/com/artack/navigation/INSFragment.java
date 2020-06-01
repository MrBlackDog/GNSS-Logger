package com.artack.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.apps.location.gps.gnsslogger.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link INSFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class INSFragment extends Fragment implements SensorEventListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static SensorManager sensorManager;
    public static Sensor sensor;
    public static boolean Register;
    private TextView textView;
    private final UiINSResults mUiComponent = new UiINSResults();
    private ScrollView insScroll;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public INSFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentINC.
     */
    // TODO: Rename and change types and number of parameters
    public static INSFragment newInstance(String param1, String param2) {
        INSFragment fragment = new INSFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //вот этот код надо закинуть в метод
        /*sensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
        }
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyro != null) {
            sensorManager.registerListener(this, gyro,
                    SensorManager.SENSOR_DELAY_GAME);
        }
        Sensor magn = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (gyro != null) {
            sensorManager.registerListener(this, magn,
                    SensorManager.SENSOR_DELAY_GAME);
        }
        Sensor Rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (gyro != null) {
            sensorManager.registerListener(this, Rotation,
                    SensorManager.SENSOR_DELAY_GAME);
        }
        *///конец кода.
    }
    public void RegisterListener (boolean register)  {
        sensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magn = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor Rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (register){

            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_GAME);
            }
            if (gyro != null) {
                sensorManager.registerListener(this, gyro,
                        SensorManager.SENSOR_DELAY_GAME);
            }

            if (gyro != null) {
                sensorManager.registerListener(this, magn,
                        SensorManager.SENSOR_DELAY_GAME);
            }
            if (gyro != null) {
                sensorManager.registerListener(this, Rotation,
                        SensorManager.SENSOR_DELAY_GAME);
            }
        }
        else {
            sensorManager.unregisterListener(this, accelerometer);
            sensorManager.unregisterListener(this,gyro);
            sensorManager.unregisterListener(this, Rotation);
            sensorManager.unregisterListener(this,magn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RegisterListener(Register);
        View newView = inflater.inflate(R.layout.fragment_i_n_c, container, false /* attachToRoot */);
        textView = (TextView) newView.findViewById(R.id.TextINS);
        insScroll = newView.findViewById(R.id.INScroll);
      return newView;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER: {
               // textView.append( "ACC " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                //        + " " + String.valueOf(event.values[2])+"\n");
                mUiComponent.logTextResults("ACC",SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                        + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_GYROSCOPE:{
              //  textView.append("GYR " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                       // + " " + String.valueOf(event.values[2])+"\n");
                mUiComponent.logTextResults("GYR",SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                         + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD:{
               // textView.append("MAG " + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
               //         + " " + String.valueOf(event.values[2])+"\n");
                mUiComponent.logTextResults("MAG",SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                                + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR:{
              //  textView.append("ROT" + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                    //    + " " + String.valueOf(event.values[2])  + " " + String.valueOf(event.values[3] )+"\n");
                mUiComponent.logTextResults("ROT",SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                            + " " + String.valueOf(event.values[2])  + " " + String.valueOf(event.values[3] )+"\n", Color.BLACK);
                break;
            }
            }
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public class UiINSResults {

        private static final int MAX_LENGTH = 12000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

        public synchronized void logTextResults(final String tag, final String text, int color) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(tag).append(" | ").append(text).append("\n");
            //MainActivity._ws.send("Location:" + tag + text );
            builder.setSpan(
                    new ForegroundColorSpan(color),
                    0 /* start */,
                    builder.length(),
                    SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            textView.append(builder);
                            SharedPreferences sharedPreferences = PreferenceManager.
                                    getDefaultSharedPreferences(getActivity());
                            Editable editable = textView.getEditableText();
                            int length = editable.length();
                            if (length > MAX_LENGTH) {
                                editable.delete(0, length - LOWER_THRESHOLD);
                            }
                            if (sharedPreferences.getBoolean(
                                    SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL, false /*default return value*/)){
                                insScroll.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        insScroll.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                            }
                        }
                    });
        }

        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }
}



