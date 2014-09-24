package com.ryansteckler.nlpunbounce;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ryansteckler.nlpunbounce.models.UnbounceStatsCollection;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class MaterialSettingsActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        WakelocksFragment.OnFragmentInteractionListener,
        WakelockDetailFragment.FragmentInteractionListener,
        HomeFragment.OnFragmentInteractionListener,
        AlarmsFragment.OnFragmentInteractionListener,
        AlarmDetailFragment.FragmentInteractionListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private boolean mIsPremium = true;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private int mCurrentSection = 1;
    int mLastActionbarColor = 0;

    private static final String TAG = "NlpUnbounceSettings: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_settings);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                drawerLayout);

        mLastActionbarColor = getResources().getColor(R.color.background_primary);

        if (isPremium()) {
              updateDonationUi();
        }
    }

    private void updateDonationUi() {
        if (isPremium()) {
            TextView textview = (TextView) findViewById(R.id.textviewKarma);
            if (textview != null)
                textview.setVisibility(View.VISIBLE);
            View donateView = (View) findViewById(R.id.layoutDonate);
            if (donateView != null)
                donateView.setVisibility(View.GONE);
        }
    }

    public boolean isPremium() {
        return mIsPremium;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        if (position == 0) {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
                    .replace(R.id.container, HomeFragment.newInstance(position + 1))
                    .commit();
        } else if (position == 1) {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
                    .replace(R.id.container, WakelocksFragment.newInstance(position + 1))
                    .addToBackStack("wakelocks")
                    .commit();
        } else if (position == 2) {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
                    .replace(R.id.container, AlarmsFragment.newInstance(position + 1))
                    .addToBackStack("alarms")
                    .commit();
        } else if (position == 3) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            if (mCurrentSection == 1) {
                getMenuInflater().inflate(R.menu.home, menu);
                restoreActionBar();
                return true;
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWakelocksSetTitle(String title) {
        mTitle = title;
        restoreActionBar();
        animateActionbarBackground(getResources().getColor(R.color.background_secondary), 400);
    }

    @Override
    public void onHomeSetTitle(String title) {
        mTitle = title;
        restoreActionBar();
        animateActionbarBackground(getResources().getColor(R.color.background_primary), 400);
    }

    @Override
    public void onWakelockDetailSetTitle(String title) {
        mTitle = title;
        restoreActionBar();
    }

    private void animateActionbarBackground(final int colorTo, final int durationInMs) {
        //Not great form, but the animation to show the details view takes 400ms.  We'll set our background
        //color back to normal once the animation finishes.  I wish there was a more elegant way to avoid
        //a timer.
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mLastActionbarColor, colorTo);
        final ColorDrawable colorDrawable = new ColorDrawable(mLastActionbarColor);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animator) {
                colorDrawable.setColor((Integer) animator.getAnimatedValue());
                getActionBar().setBackgroundDrawable(colorDrawable);
            }
        });
        if (durationInMs >= 0)
            colorAnimation.setDuration(durationInMs);
        colorAnimation.start();
        mLastActionbarColor = colorTo;

    }


    @Override
    public void onAlarmDetailSetTitle(String title) {
        mTitle = title;
        restoreActionBar();
    }

    @Override
    public void onAlarmsSetTitle(String title) {
        mTitle = title;
        restoreActionBar();
        animateActionbarBackground(getResources().getColor(R.color.background_four), 400);
    }

}
