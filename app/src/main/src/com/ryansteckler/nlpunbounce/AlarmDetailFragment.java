package com.ryansteckler.nlpunbounce;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.ryansteckler.nlpunbounce.models.AlarmStats;
import com.ryansteckler.nlpunbounce.models.UnbounceStatsCollection;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.ryansteckler.nlpunbounce.AlarmDetailFragment.FragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class AlarmDetailFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_START_TOP = "startTop";
    private static final String ARG_FINAL_TOP = "finalTop";
    private static final String ARG_START_BOTTOM = "startBottom";
    private static final String ARG_FINAL_BOTTOM = "finalBottom";
    private static final String ARG_CUR_STAT = "curStat";

    private int mStartTop;
    private int mFinalTop;
    private int mStartBottom;
    private int mFinalBottom;
    private AlarmStats mStat;
    private FragmentClearListener mClearListener = null;

    private boolean mKnownSafeAlarm = false;
    private boolean mFreeAlarm = false;

    private FragmentInteractionListener mListener;

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        ExpandingLayout anim = (ExpandingLayout) getActivity().findViewById(R.id.layoutDetails);
        anim.setAnimationBounds(mStartTop, mFinalTop, mStartBottom, mFinalBottom);
        super.onViewCreated(view, savedInstanceState);
        if (mListener != null)
            mListener.onAlarmDetailSetTitle(mStat.getName());

        loadStatsFromSource(view);

        final EditText edit = (EditText) view.findViewById(R.id.editAlarmSeconds);

        SharedPreferences prefs = getActivity().getSharedPreferences(AlarmDetailFragment.class.getPackage().getName() + "_preferences", Context.MODE_WORLD_READABLE);
        String blockSeconds = "alarm_" + mStat.getName() + "_seconds";
        edit.setText(String.valueOf(prefs.getLong(blockSeconds, 240)));
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    try {
                        long seconds = Long.parseLong(textView.getText().toString());
                        SharedPreferences prefs = getActivity().getSharedPreferences(AlarmDetailFragment.class.getPackage().getName() + "_preferences", Context.MODE_WORLD_READABLE);
                        String blockName = "alarm_" + mStat.getName() + "_seconds";
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(blockName, seconds);
                        editor.commit();
                        textView.clearFocus();
                        // hide virtual keyboard
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                        return true;

                    } catch (NumberFormatException nfe) {
                        //Not a number.  Android let us down.
                    }
                }
                return false;
            }
        });

        final Switch onOff = (Switch) view.findViewById(R.id.switchAlarm);
        String blockName = "alarm_" + mStat.getName() + "_enabled";
        boolean enabled = prefs.getBoolean(blockName, false);
        onOff.setChecked(enabled);
        getView().findViewById(R.id.editAlarmSeconds).setEnabled(onOff.isChecked());

        onOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean b = onOff.isChecked();  //! because it's the user *just* clicked it and it hasn't changed yet.
                //Check license
                if (((MaterialSettingsActivity) getActivity()).isPremium() || mFreeAlarm) {
                    if (b && !mKnownSafeAlarm) {
                        warnUnknownAlarm(onOff);
                    } else {
                        onOff.setChecked(b);
                        updateEnabledAlarm(b);
                    }
                } else {
                    //Deny based on licensing.
                    warnLicensing(onOff);
                }
            }
        });

        View panel = (View)getView().findViewById(R.id.settingsPanel);
        panel.setBackgroundColor(enabled ?
                getResources().getColor(R.color.background_panel_enabled) :
                getResources().getColor(R.color.background_panel_disabled));
        panel.setAlpha(enabled ? 1 : (float) .4);

        TextView resetButton = (TextView)view.findViewById(R.id.buttonResetStats);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View resetView) {
                //Reset stats
                UnbounceStatsCollection stats = UnbounceStatsCollection.getInstance();
                stats.resetStats(getActivity(), mStat.getName());
                loadStatsFromSource(view);
                if (mClearListener != null)
                {
                    mClearListener.onAlarmCleared();
                }
            }
        });

        TextView description = (TextView)view.findViewById(R.id.textViewAlarmDescription);
        description.setText("We don't have any information about this alarm, yet.  It may not be safe to unbounce this alarm.  Only " +
                "do so if you know what you're doing, or you know how to disable Xposed at boot.  We're working hard to collect information about every" +
                " major wakelock and alarm.  Please be patient while we collect this information in the next few weeks.");

        if (mStat.getName().toLowerCase().equals("com.google.android.gms.nlp.alarm_wakeup_locator")) {
            description.setText("This alarm is safe to unbounce.  It's used by Google Play Services to determine your rough location using a " +
                    "combination of cell towers and WiFi.  Once it has your location, it stores it locally so other apps, like Google Now, can access your " +
                    "location without using GPS or getting a new fix.  Recommended settings are between 180 and 600 seconds.");
            mKnownSafeAlarm = true;
            mFreeAlarm = true;
        } else if (mStat.getName().toLowerCase().equals("com.google.android.gms.nlp.alarm_wakeup_activity_detection")) {
            description.setText("This alarm is safe to unbounce.  It's used by Google Play Services to determine your rough location using a " +
                    "combination of cell towers and wifi.  Once it has your location, it sends it back to Google so they can expand their database " +
                    "of WiFi locations.  Recommended settings are between 180 and 600 seconds.");
            mKnownSafeAlarm = true;
            mFreeAlarm = true;
        }
    }

    private void warnLicensing(final Switch onOff) {
        new AlertDialog.Builder(getActivity())
                .setTitle("This is a Pro feature.")
                .setMessage("To Unbounce non-standard wakelocks and alarms, you need to purchase a donation package.")
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onOff.setChecked(false);
                        updateEnabledAlarm(false);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void warnUnknownAlarm(final Switch onOff) {
        new AlertDialog.Builder(getActivity())
            .setTitle("Unbounce unknown alarm?")
            .setMessage("This alarm hasn't been proven safe to Unbounce.  Would you like to Unbounce it anyway?")
            .setPositiveButton("UNBOUNCE", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    onOff.setChecked(true);
                    updateEnabledAlarm(true);
                }
            })
            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //don't set the switch
                    onOff.setChecked(false);
                    updateEnabledAlarm(false);
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }


    private void updateEnabledAlarm(boolean b) {
        String blockName = "alarm_" + mStat.getName() + "_enabled";
        SharedPreferences prefs = getActivity().getSharedPreferences("com.ryansteckler.nlpunbounce" + "_preferences", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(blockName, b);
        editor.commit();

        //Enable or disable the seconds setting.
        getView().findViewById(R.id.editAlarmSeconds).setEnabled(b);
        View panel = (View)getView().findViewById(R.id.settingsPanel);
        panel.setBackgroundColor(b ?
                getResources().getColor(R.color.background_panel_enabled) :
                getResources().getColor(R.color.background_panel_disabled));
        panel.setAlpha(b ? 1 : (float) .4);

        if (mClearListener != null)
        {
            mClearListener.onAlarmCleared();
        }
    }

    private void loadStatsFromSource(View view) {
        UnbounceStatsCollection coll = UnbounceStatsCollection.getInstance();
        mStat = coll.getAlarmStats(getActivity(), mStat.getName());

        TextView textView = (TextView)view.findViewById(R.id.textLocalAlarmBlocked);
        textView.setText(String.valueOf(mStat.getBlockCount()));
        textView = (TextView)view.findViewById(R.id.textLocalAlarmAcquired);
        textView.setText(String.valueOf(mStat.getAllowedCount()));
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlarmlockDetailFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlarmDetailFragment newInstance(int startTop, int finalTop, int startBottom, int finalBottom, AlarmStats stat) {
        AlarmDetailFragment fragment = new AlarmDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_START_TOP, startTop);
        args.putInt(ARG_FINAL_TOP, finalTop);
        args.putInt(ARG_START_BOTTOM, startBottom);
        args.putInt(ARG_FINAL_BOTTOM, finalBottom);
        args.putSerializable(ARG_CUR_STAT, stat);
        fragment.setArguments(args);
        return fragment;
    }
    public AlarmDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mStartTop = getArguments().getInt(ARG_START_TOP);
            mFinalTop = getArguments().getInt(ARG_FINAL_TOP);
            mStartBottom = getArguments().getInt(ARG_START_BOTTOM);
            mFinalBottom = getArguments().getInt(ARG_FINAL_BOTTOM);
            mStat = (AlarmStats)getArguments().getSerializable(ARG_CUR_STAT);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_alarm_detail, container, false);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.alarm_detail, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface FragmentInteractionListener {
        // TODO: Update argument type and name
        public void onAlarmDetailSetTitle(String title);
    }

    public interface FragmentClearListener {
        public void onAlarmCleared();
    }

    public void attachClearListener(FragmentClearListener fragment)
    {
        mClearListener = fragment;
    }



}
