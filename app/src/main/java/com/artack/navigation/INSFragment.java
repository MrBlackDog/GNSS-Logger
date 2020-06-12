package com.artack.navigation;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.R;
import com.google.android.apps.location.gps.gnsslogger.SettingsFragment;
import com.google.android.apps.location.gps.gnsslogger.TimerFragment;
import com.google.android.apps.location.gps.gnsslogger.TimerService;
import com.google.android.apps.location.gps.gnsslogger.TimerValues;
import com.google.android.apps.location.gps.gnsslogger.UiLogger;

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
    private static final String TIMER_FRAGMENT_TAG ="timer";
    public static SensorManager sensorManager;
    public static Sensor sensor;
    public static boolean Register;
    private TextView textView;
    //private final UiINSResults mUiComponent = new UiINSResults();
    private ScrollView insScroll;
    private Button mStartLog;
    private Button mTimer;
    private Button mSendFile;
    private TextView mTimerDisplay;
    private TextView mLogView;
    private ScrollView mScrollView;
    private UiLogger mUiLogger;
    private INSFileLogger mINSFileLogger;

    private final UiINSResults mUiComponents = new UiINSResults();

    private void launchTimerDialog() {
        TimerFragment timer = new TimerFragment();
        timer.setTargetFragment(this, 0);
        timer.setArguments(mTimerValues.toBundle());
        timer.show(getFragmentManager(), TIMER_FRAGMENT_TAG);
    }
    private TimerValues mTimerValues =
            new TimerValues(0 /* hours */, 0 /* minutes */, 0 /* seconds */);
    void stopAndSend() {
        if (mTimer != null) {
            mTimerService.stopTimer();
        }
        enableOptions(true /* start */);
        Toast.makeText(getContext(), R.string.stop_message, Toast.LENGTH_LONG).show();
        displayTimer(mTimerValues, false /* countdownStyle */);
        mINSFileLogger.send();
    }

    private void enableOptions(boolean start) {
        mTimer.setEnabled(start);
        mStartLog.setEnabled(start);
        mSendFile.setEnabled(!start);
    }
    void displayTimer(TimerValues values, boolean countdownStyle) {
        String content;

        if (countdownStyle) {
            content = values.toCountdownString();
        } else {
            content = values.toString();
        }

        mTimerDisplay.setText(
                String.format("%s: %s", getResources().getString(R.string.timer_display), content));
    }
    private ServiceConnection mConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
                    mTimerService = ((TimerService.TimerBinder) serviceBinder).getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mTimerService = null;
                }
            };
    private TimerService mTimerService;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public INSFragment() {
        // Required empty public constructor
    }
    public void setFileLogger(INSFileLogger value) {
        mINSFileLogger = value;
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


    public void RegisterListeners (boolean register)  {
        sensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //Sensor magn = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //Sensor Rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (register){

            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_GAME);
            }
            /*if (gyro != null) {
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
            }*/
        }
        else {
            sensorManager.unregisterListener(this, accelerometer);
            //sensorManager.unregisterListener(this,gyro);
            //sensorManager.unregisterListener(this, Rotation);
            //sensorManager.unregisterListener(this,magn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RegisterListeners(Register);
        View newView = inflater.inflate(R.layout.fragment_i_n_c, container, false /* attachToRoot */);

            textView = (TextView) newView.findViewById(R.id.TextINS);
            insScroll = (ScrollView) newView.findViewById(R.id.INScroll);

            getActivity()
                    .bindService(
                            new Intent(getActivity(), TimerService.class), mConnection, Context.BIND_AUTO_CREATE);

            UiLogger currentUiLogger = mUiLogger;
            if (currentUiLogger != null) {
                currentUiLogger.setUiINSResults(mUiComponents);
            }
            INSFileLogger currentFileLogger = mINSFileLogger;
            if (currentFileLogger != null) {
                currentFileLogger.setUiComponent(mUiComponents);
            }

            Button start = (Button) newView.findViewById(R.id.start_logs);
            Button end = (Button) newView.findViewById(R.id.end_log);
            Button clear = (Button) newView.findViewById(R.id.clear_log);
            mTimerDisplay = (TextView) newView.findViewById(R.id.ins_timer);
            mTimer = (Button) newView.findViewById(R.id.start_timer);
            mStartLog = (Button) newView.findViewById(R.id.start_log_ins);
            mSendFile = (Button) newView.findViewById(R.id.send_log);
            start.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mScrollView.fullScroll(View.FOCUS_UP);
                        }
                    });

            end.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });

            clear.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mLogView.setText("");
                        }
                    });



            displayTimer(mTimerValues, false /* countdownStyle */);
            enableOptions(true /* start */);

            mStartLog.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            enableOptions(false /* start */);
                            Toast.makeText(getContext(), R.string.start_message, Toast.LENGTH_LONG).show();
                            mINSFileLogger.startNewLog();

                            if (!mTimerValues.isZero() && (mTimerService != null)) {
                                mTimerService.startTimer();
                            }
                        }
                    });

            mSendFile.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            stopAndSend();
                        }
                    });

            mTimer.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            launchTimerDialog();
                        }
                    });

            return newView;
        }



    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER: {
               // textView.append( "ACC " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                //        + " " + String.valueOf(event.values[2])+"\n");
                mUiComponents.logTextResults("ACC",SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                        + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_GYROSCOPE:{
              //  textView.append("GYR " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                       // + " " + String.valueOf(event.values[2])+"\n");
                mUiComponents.logTextResults("GYR",SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                         + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD:{
               // textView.append("MAG " + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
               //         + " " + String.valueOf(event.values[2])+"\n");
                mUiComponents.logTextResults("MAG",SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                                + " " + String.valueOf(event.values[2])+"\n", Color.BLACK);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR:{
              //  textView.append("ROT" + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
                    //    + " " + String.valueOf(event.values[2])  + " " + String.valueOf(event.values[3] )+"\n");
                mUiComponents.logTextResults("ROT",SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1])
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



