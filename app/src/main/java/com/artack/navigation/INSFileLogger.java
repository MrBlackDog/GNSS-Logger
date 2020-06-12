package com.artack.navigation;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.BuildConfig;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.FileLogger;
import com.google.android.apps.location.gps.gnsslogger.GnssContainer;
import com.google.android.apps.location.gps.gnsslogger.LoggerFragment;
import com.google.android.apps.location.gps.gnsslogger.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.artack.navigation.INSFragment.sensorManager;

public class INSFileLogger implements SensorEventListener {

    private static final String TAG = "INSFileLogger";
    private static final String FILE_PREFIX = "ins_log";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();
    private BufferedWriter mFileWriter;
    private File mFile;

   // private LoggerFragment.UIFragmentComponent mUiComponent;

    private INSFragment.UiINSResults mUiComponent;

    public synchronized INSFragment.UiINSResults getUiComponent() {
        return mUiComponent;
    }

    public synchronized void setUiComponent(INSFragment.UiINSResults value) {
        mUiComponent = value;
    }

    public INSFileLogger(Context mContext) {
        this.mContext = mContext;
    }

    public void startNewLog() {
        synchronized (mFileLock) {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
            Date now = new Date();
            String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(now));
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }

            // initialize the contents of the file
            try {
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("Header Description:");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write(VERSION_TAG);
                String manufacturer = Build.MANUFACTURER;
                String model = Build.MODEL;
                String fileVersion =
                        mContext.getString(R.string.app_version)
                                + " Platform: "
                                + Build.VERSION.RELEASE
                                + " "
                                + "Manufacturer: "
                                + manufacturer
                                + " "
                                + "Model: "
                                + model;
                currentFileWriter.write(fileVersion);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("ACC,ElapsedRealtimeNanos,xAcceleration,yAcceleration,zAcceleration");
                currentFileWriter.newLine();
            } catch (IOException e) {
                logException("Count not initialize file: " + currentFilePath, e);
                return;
            }
            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }

            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:
            // - Remove all empty files
            FileFilter filter = new INSFileLogger.INSFileToDeleteFilter(mFile);
            for (File existingFile : baseDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }
        }
    }

    /**
     * Send the current log via email or other options selected from a pop menu shown to the user. A
     * new log is started when calling this function.
     */
    public void send() {
        if (mFile == null) {
            return;
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SensorLog");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        // attach the file
        Uri fileURI =
                FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", mFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
        getUiComponent().startActivity(Intent.createChooser(emailIntent, "Send log.."));
        if (mFileWriter != null) {
            try {
                mFileWriter.flush();
                mFileWriter.close();
                mFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER: {
                String ACCStream =
                        String.format("%s%s%s%s", "ACC ", System.nanoTime() + " ", String.valueOf(event.values[0]) + ' ', String.valueOf(event.values[1])
                                + ' ' + String.valueOf(event.values[2]));
                synchronized (mFileLock) {
                    if (mFileWriter == null) {
                        return;
                    }
                    try {
                        mFileWriter.write(ACCStream);
                        mFileWriter.newLine();
                    } catch (IOException e) {
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
                break;
            }
            case Sensor.TYPE_GYROSCOPE: {
                synchronized (mFileLock) {
                    if (mFileWriter == null) {
                        return;
                    }
                    String GYRStream =
                            String.format("%s%s%s%s", "GYR ", System.nanoTime() + " ", String.valueOf(event.values[0]) + ' ', String.valueOf(event.values[1])
                                    + ' ' + String.valueOf(event.values[2]));
                    /*try {
                        mFileWriter.write(GYRStream);
                        mFileWriter.newLine();
                    } catch (IOException e) {
                        logException(ERROR_WRITING_FILE, e);
                    }*/
                    break;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link INSFileLogger.INSFileToDeleteFilter#mRetainedFiles}.
     */
    private static class INSFileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public INSFileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }
        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link INSFileLogger.INSFileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }
}
