package com.artack.navigation;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.GnssListener;
import com.google.android.apps.location.gps.gnsslogger.R;
import com.google.android.apps.location.gps.gnsslogger.*;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RelativeNavigationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RelativeNavigationFragment extends Fragment {

    private final UIRelativeResultComponent mUiComponent = new UIRelativeResultComponent();
    private TextView mLogView;
    private ScrollView mScrollView;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    RealTimeRelativePositionCalculator realTimeRelativePositionCalculator;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RelativeNavigationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentRN.
     */
    // TODO: Rename and change types and number of parameters
    public static RelativeNavigationFragment newInstance(String param1, String param2) {
        RelativeNavigationFragment fragment = new RelativeNavigationFragment();
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

    public void postToast(String mes)
    {
        Toast.makeText(getContext(),realTimeRelativePositionCalculator.positionSolutionECEF[0] + " "+
                        realTimeRelativePositionCalculator.positionSolutionECEF[1] + " "+
                        realTimeRelativePositionCalculator.positionSolutionECEF[2] + " "
                ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_r_n, container, false /* attachToRoot */);
        mLogView = (TextView) newView.findViewById(R.id.log_view);
        mScrollView = (ScrollView) newView.findViewById(R.id.log_scroll);
        // Inflate the layout for this fragment
        realTimeRelativePositionCalculator = new RealTimeRelativePositionCalculator();
        if (realTimeRelativePositionCalculator != null) {
            realTimeRelativePositionCalculator.setUiResultComponent(mUiComponent);
        }
        return newView;
       // return inflater.inflate(R.layout.fragment_r_n, container, false);
    }
    /**
     * A facade for UI and Activity related operations that are required for {@link GnssListener}s.
     */
    public class UIRelativeResultComponent {

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
                            mLogView.append(builder);
                            SharedPreferences sharedPreferences = PreferenceManager.
                                    getDefaultSharedPreferences(getActivity());
                            Editable editable = mLogView.getEditableText();
                            int length = editable.length();
                            if (length > MAX_LENGTH) {
                                editable.delete(0, length - LOWER_THRESHOLD);
                            }
                            if (sharedPreferences.getBoolean(
                                    SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL, false /*default return value*/)){
                                mScrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mScrollView.fullScroll(View.FOCUS_DOWN);
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