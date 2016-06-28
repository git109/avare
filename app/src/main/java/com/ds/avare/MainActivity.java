/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ds.avare.fragment.AirportFragment;
import com.ds.avare.fragment.ChecklistFragment;
import com.ds.avare.fragment.LocationFragment;
import com.ds.avare.fragment.NearestFragment;
import com.ds.avare.fragment.PlanFragment;
import com.ds.avare.fragment.PlatesFragment;
import com.ds.avare.fragment.SatelliteFragment;
import com.ds.avare.fragment.SearchFragment;
import com.ds.avare.fragment.ThreeDFragment;
import com.ds.avare.fragment.TripFragment;
import com.ds.avare.fragment.WeatherFragment;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Boundaries;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zkhan
 */
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener {

    private static final String SELECTED_NAV_ITEM_IDX_KEY = "selectedNavItemId";

    private Preferences mPref;
    private NavigationView mNavigationView;
    private TabLayout mTabLayout;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;

    private Map<Integer, Integer> mTabIndexToNavItemIdMap = new HashMap<>();

    // Tab panels that can display at the bottom of the screen. These manifest as 
    // separate display panes with their own intent to handle the content. Each one
    // except tabMain is configurable on or off by the user. 
    public static final int NAV_ITEM_IDX_MAP = 0;
    public static final int NAV_ITEM_IDX_PLATES = 1;
    public static final int NAV_ITEM_IDX_AFD = 2;
    public static final int NAV_ITEM_IDX_FIND = 3;
    public static final int NAV_ITEM_IDX_PLAN = 4;
    public static final int NAV_ITEM_IDX_NEAR = 5;
    public static final int NAV_ITEM_IDX_THREE_D = 6;
    public static final int NAV_ITEM_IDX_CHECKLIST = 7;
    public static final int NAV_ITEM_IDX_WXB = 8;
    public static final int NAV_ITEM_IDX_TRIP = 9;
    public static final int NAV_ITEM_IDX_TOOLS = 10;
    public static final int NAV_ITEM_IDX_SPLIT = 11;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            /*
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) { }
    };

    @Override
    /**
     *
     */
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mPref = new Preferences(this);
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mService = null;

        setContentView(R.layout.main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        // set the back arrow in the toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        setupTabs(mTabLayout);
        mTabLayout.setOnTabSelectedListener(this);

        /*
         * Start service now, bind later. This will be no-op if service is already running
         */
        Intent intent = new Intent(this, StorageService.class);
        startService(intent);

        mTabLayout.setVisibility(mPref.getHideTabBar() ? View.GONE : View.VISIBLE);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        int selectedNavItemId = (savedInstanceState == null)
                ? NAV_ITEM_IDX_MAP
                : savedInstanceState.getInt(SELECTED_NAV_ITEM_IDX_KEY, 0);
        onNavigationItemSelected(mNavigationView.getMenu().getItem(selectedNavItemId));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.Add, R.string.Add);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        TextView navHeaderText = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.nav_header_text);
        navHeaderText.setText(mPref.getRegisteredEmail());
    }

    private void setupTabs(TabLayout tabLayout) {
        int tabIndex = 0;

        tabLayout.addTab(tabLayout.newTab().setText(R.string.Main), tabIndex, true);
        mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_MAP);

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_PLATES))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plates), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_PLATES);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_AFD))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.AFD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_AFD);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_FIND))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Find), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_FIND);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_PLAN))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plan), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_PLAN);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_NEAR))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Near), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_NEAR);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_THREE_D))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.ThreeD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_THREE_D);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_CHECKLIST))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.List), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_CHECKLIST);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_WXB))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.WXB), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_WXB);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_TRIP))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Trip), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_TRIP);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_TOOLS))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Tools), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_TOOLS);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        Fragment newFragment = null;
        String tag = null;
        int navItemIdx = -1;

        if (itemId == R.id.nav_map)  {
            newFragment = new LocationFragment();
            tag = LocationFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_MAP;
        } else if (itemId == R.id.nav_plate) {
            newFragment = new PlatesFragment();
            tag = PlatesFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_PLATES;
        } else if (itemId == R.id.nav_afd) {
            newFragment = new AirportFragment();
            tag = AirportFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_AFD;
        } else if (itemId == R.id.nav_find) {
            newFragment = new SearchFragment();
            tag = SearchFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_FIND;
        } else if (itemId == R.id.nav_plan) {
            newFragment = new PlanFragment();
            tag = PlanFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_PLAN;
        } else if (itemId == R.id.nav_near) {
            newFragment = new NearestFragment();
            tag = NearestFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_NEAR;
        } else if (itemId == R.id.nav_3d) {
            newFragment = new ThreeDFragment();
            tag = ThreeDFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_THREE_D;
        } else if (itemId == R.id.nav_list) {
            newFragment = new ChecklistFragment();
            tag = ChecklistFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_CHECKLIST;
        } else if (itemId == R.id.nav_wxb) {
            newFragment = new WeatherFragment();
            tag = WeatherFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_WXB;
        } else if (itemId == R.id.nav_trip) {
            newFragment = new TripFragment();
            tag = TripFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_TRIP;
        } else if (itemId == R.id.nav_tools) {
            newFragment = new SatelliteFragment();
            tag = SatelliteFragment.TAG;
            navItemIdx = NAV_ITEM_IDX_TOOLS;
        } else if (itemId == R.id.nav_preferences) {
            startPreferencesActivity();
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_downloads) {
            startChartsDownloadActivity();
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_ads) {
            startAdsActivity();
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_help) {
            startHelpActivity();
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_toggle_toolbar) {
            mToolbar.setVisibility((mToolbar.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_toggle_tablayout) {
            mTabLayout.setVisibility((mTabLayout.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (getSupportFragmentManager().getFragments() != null) {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                ft.hide(fragment);
            }
        }

        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (existingFragment == null) {
            ft.add(R.id.fragment_container, newFragment, tag);
        } else {
            ft.show(existingFragment);
        }

        ft.commit();

        if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);

        // set nav item as selected
        if (mNavigationView != null) mNavigationView.getMenu().getItem(navItemIdx).setChecked(true);

        // set tab item to selected
        if (mTabLayout != null) {
            for (Map.Entry<Integer, Integer> entries : mTabIndexToNavItemIdMap.entrySet()) {
                if (entries.getValue() == navItemIdx) {
                    mTabLayout.getTabAt(entries.getKey()).select();
                    break;
                }
            }
        }

        // redraw the toolbar menu
        invalidateOptionsMenu();

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        MenuItem chartItem = menu.findItem(R.id.action_chart);
        MenuItem layerItem = menu.findItem(R.id.action_layer);
        MenuItem tracksItem = menu.findItem(R.id.action_tracks);
        MenuItem simulationItem = menu.findItem(R.id.action_simulation);
        MenuItem flightPlanControlsItem = menu.findItem(R.id.action_flight_plan_controls);

        // these menu items should only be visible on specific tabs
        chartItem.setVisible(isMainNavItemSelected() || isThreeDNavItemSelected());
        layerItem.setVisible(isMainNavItemSelected());
        tracksItem.setVisible(isMainNavItemSelected());
        simulationItem.setVisible(isMainNavItemSelected());
        flightPlanControlsItem.setVisible(isMainNavItemSelected());

        AppCompatSpinner chartSpinner = (AppCompatSpinner) MenuItemCompat.getActionView(chartItem);
        AppCompatSpinner layerSpinner = (AppCompatSpinner) MenuItemCompat.getActionView(layerItem);

        ArrayAdapter<String> chartAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Boundaries.getChartTypes());
        chartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartSpinner.setAdapter(chartAdapter);

        if (isMainNavItemSelected()) {
            chartSpinner.setSelection(Integer.valueOf(mPref.getChartType()), false);
            chartSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (getLocationFragment() != null) {
                                mPref.setChartType(String.valueOf(position));
                                getLocationFragment().forceReloadLocationView();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) { }
                    }
            );
        } else if (isThreeDNavItemSelected()) {
            chartSpinner.setSelection(Integer.valueOf(mPref.getChartType3D()), false);
            chartSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (getThreeDFragment() != null) {
                                mPref.setChartType3D(String.valueOf(position));
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) { }
                    }
            );
        }

        List<String> layerItems = Arrays.asList("No Layer", "METAR", "NEXRAD");
        final ArrayAdapter<String> layerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, layerItems);
        layerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        layerSpinner.setAdapter(layerAdapter);
        layerSpinner.setSelection(layerItems.indexOf(mPref.getLayerType()), false);

        layerSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (getLocationFragment() != null) {
                            mPref.setLayerType(layerAdapter.getItem(position));
                            getLocationFragment().setLocationViewLayerType(mPref.getLayerType());
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                }
        );

        tracksItem.setChecked(mService != null && mService.getTracks());
        simulationItem.setChecked(mPref.isSimulationMode());
        flightPlanControlsItem.setChecked(mPref.getPlanControl());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preferences:
                startPreferencesActivity();
                break;
            case R.id.action_download:
                startChartsDownloadActivity();
                break;
            case R.id.action_ads:
                startAdsActivity();
                break;
            case R.id.action_help:
                startHelpActivity();
                break;
            case R.id.action_tracks:
                if (getLocationFragment() != null) {
                    item.setChecked(!item.isChecked());
                    getLocationFragment().setTracksMode(item.isChecked());
                    invalidateOptionsMenu();
                }
                break;
            case R.id.action_simulation:
                if (getLocationFragment() != null) {
                    item.setChecked(!item.isChecked());
                    getLocationFragment().setSimulationMode(item.isChecked());
                    invalidateOptionsMenu();
                }
                break;
            case R.id.action_flight_plan_controls:
                if (getLocationFragment() != null) {
                    item.setChecked(!item.isChecked());
                    mPref.setPlanControl(item.isChecked());
                    getLocationFragment().setPlanButtonVis();
                    invalidateOptionsMenu();
                }
                break;
            default:
                break;
        }

        return true;
    }

    private void startPreferencesActivity() {
        startActivity(new Intent(this, PrefActivity.class));
    }

    private void startChartsDownloadActivity() {
        Intent i = new Intent(this, ChartsDownloadActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    private void startAdsActivity() {
        startActivity(new Intent(this, MessageActivity.class));
    }

    private void startHelpActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("url", NetworkHelper.getHelpUrl(this));
        startActivity(intent);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int navItemIdx = mTabIndexToNavItemIdMap.get(tab.getPosition());
        onNavigationItemSelected(mNavigationView.getMenu().getItem(navItemIdx));
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { }


    @Override
    public void onBackPressed() {
        if (isMainNavItemSelected()) {
            if (getLocationFragment() != null) getLocationFragment().onBackPressed();
        } else {
            onNavigationItemSelected(mNavigationView.getMenu().getItem(NAV_ITEM_IDX_MAP));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(mConnection);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavigationView != null) {
            outState.putInt(SELECTED_NAV_ITEM_IDX_KEY, getSelectedNavItemIdx());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, StorageService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Helper.setOrientationAndOn(this);
    }

    @Override
    public void onDestroy() {
        /*
         * Start service now, bind later. This will be no-op if service is already running
         */
        if (!mPref.shouldLeaveRunning()) {
            if (isFinishing()) {
                /*
                 * Do not kill on orientation change
                 */
                Intent intent = new Intent(this, StorageService.class);
                stopService(intent);
            }
        }
        super.onDestroy();
    }

    /**
     * For switching tab from any tab activity
     */
    private void switchView(int navItemIdx) {
        onNavigationItemSelected(mNavigationView.getMenu().getItem(navItemIdx));

        /*
         * Hide soft keyboard that may be open
         */
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTabLayout.getApplicationWindowToken(), 0);
    }

    /**
     * Display the main/maps tab
     */
    public void showMapView() {
        switchView(NAV_ITEM_IDX_MAP);
    }

    /**
     * Display the Plan tab
     */
    public void showPlanView() {
        switchView(NAV_ITEM_IDX_PLAN);
    }

    /**
     * Show the Plates view
     */
    public void showPlatesView() {
        switchView(NAV_ITEM_IDX_PLATES);
    }

    /**
     * Show the AFD view
     */
    public void showAfdView() {
        switchView(NAV_ITEM_IDX_AFD);
    }

    private LocationFragment getLocationFragment() {
        if (isMainNavItemSelected()) {
            return (LocationFragment) getSupportFragmentManager().findFragmentByTag(LocationFragment.TAG);
        }
        return null;
    }

    private ThreeDFragment getThreeDFragment() {
        if (isThreeDNavItemSelected()) {
            return (ThreeDFragment) getSupportFragmentManager().findFragmentByTag(ThreeDFragment.TAG);
        }
        return null;
    }

    private boolean isMainNavItemSelected() {
        return mNavigationView.getMenu().getItem(NAV_ITEM_IDX_MAP).isChecked();
    }

    private boolean isThreeDNavItemSelected() {
        return mNavigationView.getMenu().getItem(NAV_ITEM_IDX_THREE_D).isChecked();
    }

    private int getSelectedNavItemIdx() {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            if (mNavigationView.getMenu().getItem(i).isChecked()) return i;
        }
        return 0;
    }

}