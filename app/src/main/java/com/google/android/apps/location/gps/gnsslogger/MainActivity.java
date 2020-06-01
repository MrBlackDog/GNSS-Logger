/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.location.gps.gnsslogger;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.TabLayoutOnPageChangeListener;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.artack.navigation.INSFragment;
import com.artack.navigation.RealTimeRelativePositionCalculator;
import com.artack.navigation.RelativeNavigationFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

import okhttp3.WebSocket;

/** The activity for the application. */
public class MainActivity extends AppCompatActivity
    implements OnConnectionFailedListener, ConnectionCallbacks, GroundTruthModeSwitcher, SensorEventListener {
  private static final int LOCATION_REQUEST_ID = 1;
  private static final String[] REQUIRED_PERMISSIONS = {
    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
  };
  private static final int NUMBER_OF_FRAGMENTS = 8;
  private static final int FRAGMENT_INDEX_SETTING = 0;
  private static final int FRAGMENT_INDEX_LOGGER = 1;
  private static final int FRAGMENT_INDEX_RESULT = 2;
  private static final int FRAGMENT_INDEX_MAP = 3;
  private static final int FRAGMENT_INDEX_AGNSS = 4;
  private static final int FRAGMENT_INDEX_PLOT = 5;
  private static final int FRAGMENT_INDEX_INC = 6;
  private static final int FRAGMENT_INDEX_RN = 7;
  private static final String TAG = "MainActivity";

  public static WebSocket _ws;
  public static SocketManager sm;

  public final static String model = "test2";
  private final static String FILE_NAME = "INS" + model +".txt";
  private File myFile = new File("/sdcard/INS" + model + ".txt");

  public final Object locker = new Object();
  public boolean Connected;

  private GnssContainer mGnssContainer;
  private UiLogger mUiLogger;
  private RealTimePositionVelocityCalculator mRealTimePositionVelocityCalculator;
  //мой класс для расчета относительной навигации
  private RealTimeRelativePositionCalculator mRealTimeRelativePositionCalculator;

  private FileLogger mFileLogger;
  private AgnssUiLogger mAgnssUiLogger;
  private Fragment[] mFragments;
  private GoogleApiClient mGoogleApiClient;
  private boolean mAutoSwitchGroundTruthMode;
  private final ActivityDetectionBroadcastReceiver mBroadcastReceiver =
      new ActivityDetectionBroadcastReceiver();

  public static SensorManager sensorManager;
  public static Sensor sensor;
  private final float[] accelerometerReading = new float[3];
  private final float[] magnetometerReading = new float[3];

  private final float[] rotationMatrix = new float[9];
  private final float[] orientationAngles = new float[3];

  private static final float NS2S = 1.0f / 1000000000.0f;
  private final float[] deltaRotationVector = new float[4];
  private float timestamp;
  private final float EPSILON = 1e-6f;
  TextView TextINC;
  Sensor sensorAccel;
  Sensor sensorGravity;

  private ServiceConnection mConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
          // Empty
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
          // Empty
        }
      };




  @Override
  protected void onStart() {
    super.onStart();
    // Bind to the timer service to ensure it is available when app is running
    bindService(new Intent(this, TimerService.class), mConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                    mBroadcastReceiver, new IntentFilter(
                            DetectedActivitiesIntentReceiver.AR_RESULT_BROADCAST_ACTION));
   /* sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer != null) {
      sensorManager.registerListener(, accelerometer,
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
    }*/
  }

  @Override
  protected void onPause() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    unbindService(mConnection);
  }

  @Override
  protected void onDestroy(){
    mGnssContainer.unregisterAll();
    super.onDestroy();
  }

  public void Connect(){
    sm = new SocketManager();
    _ws = sm.Connect();
    //SendGUIDandLobbyInfo(LobbyName,Pass,Status);

    /*try {
      for (int i =0; i< 50; i++) {
        if (!Connected) {
          Thread.sleep(100);
        }
        else
          break;
      }
    } catch (InterruptedException e){}

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        while (true) {
          synchronized (locker) {
            Connected = false;
          }
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
          }

          synchronized (locker) {
            if (!Connected) {
              break;
            }
          }
        }
      }
    });
    thread.start();*/
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SharedPreferences sharedPreferences = PreferenceManager.
        getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL, false);
    editor.commit();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    buildGoogleApiClient();
    Connect();
    requestPermissionAndSetupFragments(this);

  }

  protected PendingIntent createActivityDetectionPendingIntent() {
    Intent intent = new Intent(this, DetectedActivitiesIntentReceiver.class);
    return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private synchronized void buildGoogleApiClient() {
    mGoogleApiClient =
        new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(ActivityRecognition.API)
            .build();
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult result) {
    if (Log.isLoggable(TAG, Log.INFO)){
      Log.i(TAG,  "Connection failed: ErrorCode = " + result.getErrorCode());
    }
  }

  @Override
  public void onConnected(Bundle connectionHint) {
    if (Log.isLoggable(TAG, Log.INFO)) {
      Log.i(TAG, "Connected to GoogleApiClient");
    }
    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
        mGoogleApiClient, 0, createActivityDetectionPendingIntent());
  }

  @Override
  public void onConnectionSuspended(int cause) {
    if (Log.isLoggable(TAG, Log.INFO)) {
      Log.i(TAG, "Connection suspended");
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType())
    {
      case  Sensor.TYPE_ACCELEROMETER:
      {
    //    float[] gravity = new float[3];
    //    float[] linear_acceleration = new float[3];
    //    final float alpha = 0.8f;
        // Isolate the force of gravity with the low-pass filter.
     //   gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
      //  gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
      //  gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        // Remove the gravity contribution with the high-pass filter.
      //  linear_acceleration[0] = event.values[0] - gravity[0];
       // linear_acceleration[1] = event.values[1] - gravity[1];
      //  linear_acceleration[2] = event.values[2] - gravity[2];
       // _ws.send(String.valueOf(linear_acceleration[0]) + ' ' + String.valueOf(linear_acceleration[1])
         //       + ' ' + String.valueOf(linear_acceleration[2]) );
     final String str = "INS:" + "ACC " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + ' ' + String.valueOf(event.values[1])
                + ' ' + String.valueOf(event.values[2]);
        Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            MainActivity._ws.send(str);
          }
        });
        thread.start();
        try {
          FileOutputStream fOut = new FileOutputStream(myFile,true);
          OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
          myOutWriter.append(model +" "+ str).append("\n");
          myOutWriter.close();
          fOut.close();
        } catch (Exception e) {
          Log.e("ERRR", "Could not create file",e);
        } ;
        break;
      }
      case Sensor.TYPE_GYROSCOPE:
      {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
    /*    if (timestamp != 0) {
          final float dT = (event.timestamp - timestamp) * NS2S;
          // Axis of the rotation sample, not normalized yet.
          float axisX = event.values[0];
          float axisY = event.values[1];
          float axisZ = event.values[2];

          // Calculate the angular speed of the sample
          float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

          // Normalize the rotation vector if it's big enough to get the axis
          // (that is, EPSILON should represent your maximum allowable margin of error)
          if (omegaMagnitude > EPSILON) {
            axisX /= omegaMagnitude;
            axisY /= omegaMagnitude;
            axisZ /= omegaMagnitude;
          }

          // Integrate around this axis with the angular speed by the timestep
          // in order to get a delta rotation from this sample over the timestep
          // We will convert this axis-angle representation of the delta rotation
          // into a quaternion before turning it into the rotation matrix.
          float thetaOverTwo = omegaMagnitude * dT / 2.0f;
          float sinThetaOverTwo =(float) Math.sin(thetaOverTwo);
          float cosThetaOverTwo =(float) Math.cos(thetaOverTwo);
          deltaRotationVector[0] = sinThetaOverTwo * axisX;
          deltaRotationVector[1] = sinThetaOverTwo * axisY;
          deltaRotationVector[2] = sinThetaOverTwo * axisZ;
          deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        _ws.send(String.valueOf(deltaRotationVector[0]) + ' ' + String.valueOf(deltaRotationVector[1])
             + ' ' + String.valueOf(deltaRotationVector[2]) + ' ' + String.valueOf(deltaRotationVector[3]));*/
        final String str ="INS:" + "GYR " + SystemClock.elapsedRealtimeNanos() +" " + String.valueOf(event.values[0]) + ' ' + String.valueOf(event.values[1])
                + ' ' + String.valueOf(event.values[2]);
      /*  FileOutputStream fos = null;
        try {
          fos = openFileOutput(FILE_NAME, MODE_APPEND);
          fos.write(str.getBytes());
        }
        catch(IOException ex) {
         }
        finally{
          try{
            if(fos!=null) {
              //Toast.makeText(this, Context.getFileStreamPath( FILE_NAME), Toast.LENGTH_SHORT).show();

              fos.close();
            }
          }
          catch(IOException ex){

          }
        }*/
        try {
          FileOutputStream fOut = new FileOutputStream(myFile,true);
          OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
          myOutWriter.append(model +" " + str).append("\n");
          myOutWriter.close();
          fOut.close();
        } catch (Exception e) {
          Log.e("ERRR", "Could not create file",e);
        } ;

        Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            MainActivity._ws.send(str);
          }
        });
        thread.start();

        //   textView10.setText(deltaRotationMatrix[0] + "\n" + deltaRotationMatrix[1] + "\n" + deltaRotationMatrix[2] + "\n" + deltaRotationMatrix[0] + "\n" +
       //         deltaRotationMatrix[3] + "\n" + deltaRotationMatrix[4] + "\n" +deltaRotationMatrix[5] + "\n" +
         //       deltaRotationMatrix[6] + "\n" +deltaRotationMatrix[7] + "\n" +deltaRotationMatrix[8]);
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;

        break;
      }
      case Sensor.TYPE_MAGNETIC_FIELD: {
        final String str = "INS:" + "MAG " + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + ' ' + String.valueOf(event.values[1])
                + ' ' + String.valueOf(event.values[2]);
        Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            MainActivity._ws.send(str);
          }
        });
        thread.start();
        try {
          FileOutputStream fOut = new FileOutputStream(myFile,true);
          OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
          myOutWriter.append(model +" "+ str).append("\n");
          myOutWriter.close();
          fOut.close();
        } catch (Exception e) {
          Log.e("ERRR", "Could not create file",e);
        } ;
        break;
      }
      case Sensor.TYPE_ROTATION_VECTOR:
        final String str = "INS:" + "ROT " + SystemClock.elapsedRealtimeNanos() + " " + String.valueOf(event.values[0]) + ' ' + String.valueOf(event.values[1])
                + ' ' + String.valueOf(event.values[2])  + ' ' + String.valueOf(event.values[3]  + ' ' + String.valueOf(event.values[4]));
        Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            MainActivity._ws.send(str);
          }
        });
        thread.start();
        try {
          FileOutputStream fOut = new FileOutputStream(myFile,true);
          OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
          myOutWriter.append(model +" "+ str).append("\n");
          myOutWriter.close();
          fOut.close();
        } catch (Exception e) {
          Log.e("ERRR", "Could not create file",e);
        } ;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).equals(state)) {
      return true;
    }
    return false;
  }
  public File getDir(String albumName)
  {
    File file = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS),albumName );
    //"INS Logs"
    if (!file.mkdirs()) {

    }
    return file;
  }
  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
   * sections/tabs/pages.
   */
  public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      switch (position) {
        case FRAGMENT_INDEX_SETTING:
          return mFragments[FRAGMENT_INDEX_SETTING];
        case FRAGMENT_INDEX_LOGGER:
          return mFragments[FRAGMENT_INDEX_LOGGER];
        case FRAGMENT_INDEX_RESULT:
          return mFragments[FRAGMENT_INDEX_RESULT];
        case FRAGMENT_INDEX_MAP:
          return mFragments[FRAGMENT_INDEX_MAP];
        case FRAGMENT_INDEX_AGNSS:
          return mFragments[FRAGMENT_INDEX_AGNSS];
        case FRAGMENT_INDEX_PLOT:
          return mFragments[FRAGMENT_INDEX_PLOT];
        case FRAGMENT_INDEX_INC:
          return mFragments[FRAGMENT_INDEX_INC];
        case FRAGMENT_INDEX_RN:
          return mFragments[FRAGMENT_INDEX_RN];
        default:
          throw new IllegalArgumentException("Invalid section: " + position);
      }
    }

    @Override
    public int getCount() {
      // Show total pages.
      return NUMBER_OF_FRAGMENTS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale locale = Locale.getDefault();
      switch (position) {
        case FRAGMENT_INDEX_SETTING:
          return getString(R.string.title_settings).toUpperCase(locale);
        case FRAGMENT_INDEX_LOGGER:
          return getString(R.string.title_log).toUpperCase(locale);
        case FRAGMENT_INDEX_RESULT:
          return getString(R.string.title_offset).toUpperCase(locale);
        case FRAGMENT_INDEX_MAP:
          return getString(R.string.title_map).toUpperCase(locale);
        case FRAGMENT_INDEX_AGNSS:
          return getString(R.string.title_agnss).toUpperCase(locale);
        case FRAGMENT_INDEX_PLOT:
          return getString(R.string.title_plot).toLowerCase(locale);
        case FRAGMENT_INDEX_INC:
          return getString(R.string.title_INC).toLowerCase(locale);
        case FRAGMENT_INDEX_RN:
          return getString(R.string.title_RN).toLowerCase(locale);
        default:
          return super.getPageTitle(position);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == LOCATION_REQUEST_ID) {
      // If request is cancelled, the result arrays are empty.
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        setupFragments();
      }
    }
  }

  private void setupFragments() {
    mUiLogger = new UiLogger();
    mRealTimePositionVelocityCalculator = new RealTimePositionVelocityCalculator();
    mRealTimeRelativePositionCalculator = new RealTimeRelativePositionCalculator();
    mRealTimePositionVelocityCalculator.setMainActivity(this);
    mRealTimePositionVelocityCalculator.setResidualPlotMode(
        RealTimePositionVelocityCalculator.RESIDUAL_MODE_DISABLED, null /* fixedGroundTruth */);

    mFileLogger = new FileLogger(getApplicationContext());
    mAgnssUiLogger = new AgnssUiLogger();
    mGnssContainer =
        new GnssContainer(
            getApplicationContext(),
            mUiLogger,
            mFileLogger,
            mRealTimePositionVelocityCalculator,
            mAgnssUiLogger,
            mRealTimeRelativePositionCalculator);
    mFragments = new Fragment[NUMBER_OF_FRAGMENTS];
    SettingsFragment settingsFragment = new SettingsFragment();
    settingsFragment.setGpsContainer(mGnssContainer);
    settingsFragment.setRealTimePositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    settingsFragment.setAutoModeSwitcher(this);
    mFragments[FRAGMENT_INDEX_SETTING] = settingsFragment;

    LoggerFragment loggerFragment = new LoggerFragment();
    loggerFragment.setUILogger(mUiLogger);
    loggerFragment.setFileLogger(mFileLogger);
    mFragments[FRAGMENT_INDEX_LOGGER] = loggerFragment;

    ResultFragment resultFragment = new ResultFragment();
    resultFragment.setPositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    mFragments[FRAGMENT_INDEX_RESULT] = resultFragment;

    MapFragment mapFragment = new MapFragment();
    mapFragment.setPositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    mFragments[FRAGMENT_INDEX_MAP] = mapFragment;

    AgnssFragment agnssFragment = new AgnssFragment();
    agnssFragment.setGpsContainer(mGnssContainer);
    agnssFragment.setUILogger(mAgnssUiLogger);
    mFragments[FRAGMENT_INDEX_AGNSS] = agnssFragment;

    PlotFragment plotFragment = new PlotFragment();
    mFragments[FRAGMENT_INDEX_PLOT] = plotFragment;
    mRealTimePositionVelocityCalculator.setPlotFragment(plotFragment);

    INSFragment incFragment = new INSFragment();
    //FragmentINC.setGpsContainer(mGnssContainer);
    //settingsFragment.setRealTimePositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    //settingsFragment.setAutoModeSwitcher(this);
    mFragments[FRAGMENT_INDEX_INC] = incFragment;
    RelativeNavigationFragment rnFragment = new RelativeNavigationFragment();
    //FragmentINC.setGpsContainer(mGnssContainer);
    //settingsFragment.setRealTimePositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    //settingsFragment.setAutoModeSwitcher(this);
    mFragments[FRAGMENT_INDEX_RN] = rnFragment;



    // The viewpager that will host the section contents.
    ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
    viewPager.setOffscreenPageLimit(5);
    ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
    viewPager.setAdapter(adapter);

    TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
    tabLayout.setTabsFromPagerAdapter(adapter);

    // Set a listener via setOnTabSelectedListener(OnTabSelectedListener) to be notified when any
    // tab's selection state has been changed.
    tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

    // Use a TabLayout.TabLayoutOnPageChangeListener to forward the scroll and selection changes to
    // this layout
    viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(tabLayout));
  }

  private boolean hasPermissions(Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
      // Permissions granted at install time.
      return true;
    }
    for (String p : REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private void requestPermissionAndSetupFragments(final Activity activity) {
    if (hasPermissions(activity)) {
      setupFragments();
    } else {
      ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
    }
  }

  /**
   * Toggles the flag to allow Activity Recognition updates to change ground truth mode
   */
  public void setAutoSwitchGroundTruthModeEnabled(boolean enabled) {
    mAutoSwitchGroundTruthMode = enabled;
  }

  /**
   * A receiver for result of
   * {@link ActivityRecognition#ActivityRecognitionApi#requestActivityUpdates()} broadcast by {@link
   * DetectedActivitiesIntentReceiver}
   */
  public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

      // Modify the status of mRealTimePositionVelocityCalculator only if the status is set to auto
      // (indicated by mAutoSwitchGroundTruthMode).
      if (mAutoSwitchGroundTruthMode) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        setGroundTruthModeOnResult(result);
      }
    }
  }

  /**
   * Sets up the ground truth mode of {@link RealTimePositionVelocityCalculator} given an result
   * from Activity Recognition update. For activities other than {@link DetectedActivity#STILL}
   * and {@link DetectedActivity#TILTING}, we conservatively assume the user is moving and use the
   * last WLS position solution as ground truth for corrected residual computation.
   */
  private void setGroundTruthModeOnResult(ActivityRecognitionResult result){
    if (result != null){
      int detectedActivityType = result.getMostProbableActivity().getType();
      if (detectedActivityType == DetectedActivity.STILL
          || detectedActivityType == DetectedActivity.TILTING){
        mRealTimePositionVelocityCalculator.setResidualPlotMode(
            RealTimePositionVelocityCalculator.RESIDUAL_MODE_STILL, null);
      } else {
        mRealTimePositionVelocityCalculator.setResidualPlotMode(
            RealTimePositionVelocityCalculator.RESIDUAL_MODE_MOVING, null);
      }
    }
  }
}
