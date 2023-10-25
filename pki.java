/*
 * SettingsSiteLocalisationFragment.java
 * Created by username on  30/01/19 11:13
 *
 * module: tydomApp
 * Project: D2282_Android_AppTydom
 *
 * Copyright (c) 2019 DeltaDore. All rights reserved.
 * Last modified 30/01/19 10:53
 */

package com.deltadore.tydom.app.account.flow.gateway;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.deltadore.tydom.apiservice.sitemgt.models.InvitationRoles;
import com.deltadore.tydom.app.AppUtils;
import com.deltadore.tydom.app.DisplayDialogFragment;
import com.deltadore.tydom.app.Localisation;
import com.deltadore.tydom.app.LocalisationUtils;
import com.deltadore.tydom.app.R;
import com.deltadore.tydom.app.baseui.TydomAlertDialog;
import com.deltadore.tydom.app.connexion.ConnectionExecutive;
import com.deltadore.tydom.app.home.HomeActivity;
import com.deltadore.tydom.app.home.IHomeLayout;
import com.deltadore.tydom.app.site.GatewayConnectionFailureFragment;
import com.deltadore.tydom.app.viewmodel.GeolocService;
import com.deltadore.tydom.contract.TydomContract;
import com.deltadore.tydom.contract.managers.impl.GatewayManager;
import com.deltadore.tydom.contract.managers.impl.json.JsonConstants;
import com.deltadore.tydom.contract.model.Connection;
import com.deltadore.tydom.contract.model.Gateway;
import com.deltadore.tydom.contract.model.GatewayExtensions;
import com.deltadore.tydom.data.db.AppExecutors;
import com.deltadore.tydom.data.db.Status;
import com.deltadore.tydom.data.db.entity.CameraEntity;
import com.deltadore.tydom.data.repositories.Repository;
import com.deltadore.tydom.dddesignsystem.button.DDButton;
import com.deltadore.tydom.dddesignsystem.sliderCase.SliderCaseCustomView;
import com.deltadore.tydom.dddesignsystem.sliderCase.SliderIndicatorModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class GatewayLocalisationFragment extends DisplayDialogFragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Localisation.IAccessLocationGranted {

    private View _backButton = null;
    private ProgressBar pbLoading = null;
    private AppCompatImageView btnInfo = null;
    private SliderCaseCustomView cvSlider = null;
    private DDButton btnSkip = null;
    private DDButton btnValid = null;
    private TextView toolbarTitle = null;
    private DDButton locateMe = null;

    private Gateway _gateway;
    private GatewayLocalisationViewModel _gatewayLocalisationViewModel;
    private GatewayCreationViewModel gatewayCreationViewModel;

    // Logs
    private final Logger log = LoggerFactory.getLogger(GatewayLocalisationFragment.class);
    private GoogleApiClient mGoogleApiClient;
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker marker;
    private MarkerOptions markerOption;
    public boolean setAutomaticPosition = true; //Come to My site from ConnectActivity
    private View noInternetContainerView;
    private View mapContainerView;
    private TydomAlertDialog infoDialog = null;

    private final static String FROM_INSTALLER = "FROM_INSTALLER";
    private final static String FROM_PROGRAM_ACTIVITY = "FROM_PROGRAM_ACTIVITY";
    private final static String RETRIEVE_CURRENT_GATEWAY = "RETRIEVE_CURRENT_GATEWAY";
    private final static String IS_NEW_GATEWAY = "IS_NEW_GATEWAY";
    private final static String MAC_ADDRESS_NUMBER_NEW_GATEWAY_KEY = "MAC_ADDRESS_NUMBER_NEW_GATEWAY";
    private final static String TYDOM_NAME_NEW_GATEWAY_KEY = "TYDOM_NAME_NEW_GATEWAY";

    private final static String SCHEME_PACKAGE_NAME = "package";


    private boolean fromInstaller = false;
    private boolean fromProgramActivity = false;
    private boolean retrieveCurrentGateway = false;
    private boolean isNewGatewayInstallation = false;

    private String macAddressNumber = null;
    private String tydomName = null;

    private double initialLatitude, initialLongitude;
    private boolean startsGatewayConnectionFailure = false;

    private IHomeLayout _homeLayout;

    private ArrayList<String> rolesList = new ArrayList<>();

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    Repository mRepository;

    @Inject
    AppExecutors mExecutor;

    @Inject
    ConnectionExecutive connectionExecutive;
    boolean finePermissionAllowed = false;

    public static GatewayLocalisationFragment getInstance(boolean fromInstaller, boolean fromProgram, boolean retrieveCurrentGateway) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FROM_INSTALLER, fromInstaller);
        bundle.putBoolean(FROM_PROGRAM_ACTIVITY, fromProgram);
        bundle.putBoolean(RETRIEVE_CURRENT_GATEWAY, retrieveCurrentGateway);
        GatewayLocalisationFragment fragment = new GatewayLocalisationFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static GatewayLocalisationFragment getInstance(
            boolean fromInstaller,
            boolean isNewGateway,
            String macAddressNumber,
            String tydomName
    ) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FROM_INSTALLER, fromInstaller);
        bundle.putBoolean(IS_NEW_GATEWAY, isNewGateway);
        bundle.putString(MAC_ADDRESS_NUMBER_NEW_GATEWAY_KEY, macAddressNumber);
        bundle.putString(TYDOM_NAME_NEW_GATEWAY_KEY, tydomName);
        GatewayLocalisationFragment fragment = new GatewayLocalisationFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            this.fromInstaller = getArguments().getBoolean(FROM_INSTALLER, false);
            this.fromProgramActivity = getArguments().getBoolean(FROM_PROGRAM_ACTIVITY, false);
            this.retrieveCurrentGateway = getArguments().getBoolean(RETRIEVE_CURRENT_GATEWAY, false);
            isNewGatewayInstallation = getArguments().getBoolean(IS_NEW_GATEWAY, false);
            if (isNewGatewayInstallation) {
                macAddressNumber = getArguments().getString(MAC_ADDRESS_NUMBER_NEW_GATEWAY_KEY);
                tydomName = getArguments().getString(TYDOM_NAME_NEW_GATEWAY_KEY);
            }
        }

        _homeLayout = getActivity() instanceof IHomeLayout ? (IHomeLayout)getActivity() : null;

        View view = inflater.inflate(R.layout.settings_site_localisation_layout, null);
        _backButton = view.findViewById(R.id.settings_site_localisation_back_button);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasLocationPermission()) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)){
                multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }else {
                multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        }

    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    private boolean hasLocationPermission(){
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private final ActivityResultLauncher<String[]> multiplePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {

                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null){
                    finePermissionAllowed = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    if (!finePermissionAllowed){
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                            updateMarker();
                            if(marker != null)
                                marker.setVisible(false);
                            btnValid.setEnabled(false);
                            animateCamera();
                        }
                    }
                }
            });


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //disable connection snackBar during this fragment
        if(requireActivity() instanceof HomeActivity) {
            ((HomeActivity)requireActivity()).pauseConnectionObserver();
        }

        LocalBroadcastManager
                .getInstance(requireActivity())
                .registerReceiver(
                        mGeoLocationReceiver,
                        new IntentFilter(JsonConstants.TAG_GEOLOCATION_FILTER)
                );

        TextView localisationExplain = view.findViewById(R.id.settings_site_locate_explain);

        pbLoading = view.findViewById(R.id.pb_loading);
        cvSlider = view.findViewById(R.id.cv_slider);

        btnValid = view.findViewById(R.id.btn_valid);
        if (isNewGatewayInstallation) {
            btnValid.setEnabled(false);
        }

        btnValid.setOnClickListener(v -> onValidAction());
        if (fromInstaller) {
            View navBar = view.findViewById(R.id.settings_site_localisation_back_layout);
            navBar.setVisibility(View.GONE);

            localisationExplain.setText(R.string.USERACCOUNT_ADD_NEW_HOME_MAP_REASON_INSTALLER);
        }

        if(fromProgramActivity){
            View navBar = view.findViewById(R.id.settings_site_localisation_back_layout);
            navBar.setVisibility(View.GONE);
        }

        gatewayCreationViewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(GatewayCreationViewModel.class);
        _gatewayLocalisationViewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(GatewayLocalisationViewModel.class);
        if (retrieveCurrentGateway)
            _gatewayLocalisationViewModel.retrieveCurrentGateway();
        _gateway = _gatewayLocalisationViewModel.get();

        if (_gateway == null) {
            _gateway = GatewayExtensions.createGateway("");
        } else {
            _gatewayLocalisationViewModel.setAutomaticallyRegisterLocalisation();
        }

        if (_gateway.getLatitude() != null && !GatewayManager.PARIS_LATITUDE.equals(_gateway.getLatitude())) {
            setAutomaticPosition = false;
        }

        double latitude = GatewayManager.PARIS_LATITUDE;
        double longitude = GatewayManager.PARIS_LONGITUDE;
        if(_gateway.getLatitude() != null) latitude = _gateway.getLatitude();
        if(_gateway.getLongitude() != null) longitude = _gateway.getLongitude();
        initialLatitude = AppUtils.manageCoordinatesFromDB(latitude);
        initialLongitude = AppUtils.manageCoordinatesFromDB(longitude);

        LocalisationUtils.saveSiteLocalisationViewed(getContext(), GatewayExtensions.getSingleAddress(_gateway));

        noInternetContainerView = view.findViewById(R.id.noInternetContainer);
        mapContainerView = view.findViewById(R.id.mapContainer);

        _backButton.setOnClickListener(v -> {
            getActivity().onBackPressed();
        });

        mGoogleApiClient = new GoogleApiClient.Builder(requireActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


        // Init MapView
        mapView = view.findViewById(R.id.site_map);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);


        mGoogleApiClient = new GoogleApiClient.Builder(requireActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        locateMe = view.findViewById(R.id.locate_me);
        locateMe .setOnClickListener(v -> {
            setAutomaticPosition = true;
            Localisation.getInstance().getLocalisation(getActivity(), GatewayLocalisationFragment.this, GatewayLocalisationFragment.this);
        });

        btnInfo = view.findViewById(R.id.btn_info);
        btnInfo.setOnClickListener(v -> showInformationDialog());

        btnSkip = view.findViewById(R.id.btn_skip);

        _backButton = view.findViewById(R.id.settings_site_localisation_back_button);
        localisationExplain.setPaintFlags(localisationExplain.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        localisationExplain.setOnClickListener(v -> showLocalisationExplaindialog());

        // For Automatic Tests purposes
        String screenPrefix = getString(R.string.TA_SITE_LOCALIZATION_PREFIX);
        _backButton.setContentDescription(screenPrefix + getString(R.string.TA_BACK_BUTTON));
        toolbarTitle = view.findViewById(R.id.settings_site_localisation_title_text_item);
        toolbarTitle.setContentDescription(screenPrefix + getString(R.string.TA_TITLE));
        mapView.setContentDescription(screenPrefix + getString(R.string.TA_MAP_VIEW));
        btnValid.setContentDescription(screenPrefix + getString(R.string.TA_BUTTON_PREFIX) + getString(R.string.TA_SAVE_LOCATION));
        locateMe.setContentDescription(screenPrefix + getString(R.string.TA_BUTTON_PREFIX) + getString(R.string.TA_LOCATE_ME));
        localisationExplain.setContentDescription(screenPrefix + getString(R.string.TA_TEXT_PREFIX) + getString(R.string.TA_EXPLAIN));

        setupView();
        initSliderView();
        initNewGatewayInstallationListener();
    }

    private void setupView() {
        if (isNewGatewayInstallation) {
            btnInfo.setVisibility(View.INVISIBLE);
            btnSkip.setVisibility(View.VISIBLE);

            if (fromInstaller) {
                toolbarTitle.setText(getString(R.string.USERACCOUNT_ADD_NEW_HOME_TITLE_INSTALLER));
            } else {
                toolbarTitle.setText(getString(R.string.USERACCOUNT_ADD_NEW_HOME_TITLE_USER));
            }

        } else {
            btnInfo.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.GONE);
        }
    }

    private void initSliderView() {
        SliderIndicatorModel item = new SliderIndicatorModel(getString(R.string.SETTINGS_SITE_LOCATION), getString(R.string.HOME_LOCATION_SCREEN_DESCRIPTION));
        List<SliderIndicatorModel> data = new ArrayList<>();
        data.add(item);
        data.add(item);

        cvSlider.setData(data);
        cvSlider.setHasDescription(true);
        cvSlider.setConfig();
        cvSlider.setCurrentIndex(1);
        if (!isNewGatewayInstallation) cvSlider.getBinding().sliderIndicator.setVisibility(View.GONE);

    }

    private void onValidAction() {
        if (isNewGatewayInstallation) {
            onAddNewGateway(initialLongitude, initialLatitude);
        } else {
            onSaveNewPosition();
        }
    }

    private void onSaveNewPosition() {
        _gateway = GatewayExtensions.copyLatitudeLongitude(
                _gateway,
                AppUtils.manageCoordinates(initialLatitude),
                AppUtils.manageCoordinates(initialLongitude)
        );
        _gatewayLocalisationViewModel.set(_gateway);
        _gatewayLocalisationViewModel.registerLocalisation();
        if(_gateway.get_id() != -1 && _gateway.getSelected() != null && _gateway.getSelected()){
            showLoading();
            Intent intent = new Intent(getActivity(), GeolocService.class);
            intent.putExtra(GeolocService.MODE, GeolocService.MODE_PUSH);
            requireActivity().startService(intent);
        } else {
            onLocationSaved();
        }
    }

    private void showLoading() {
        pbLoading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        pbLoading.setVisibility(View.GONE);
    }

    private void showLocalisationExplaindialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.SETTINGS_NEW_SITE_HOME_SITUATION));
        builder.setMessage(getResources().getString(R.string.SETTINGS_NEW_SITE_HOME_SITUATION_INFORMATION));
        builder.setPositiveButton(getResources().getString(R.string.COMMON_OK), (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onResume() {
        if (isOnline()) {
            if (mapContainerView != null) mapContainerView.setVisibility(View.VISIBLE);
            if (noInternetContainerView != null) noInternetContainerView.setVisibility(View.GONE);
            mapView.onResume();
        } else {
            if (mapContainerView != null) mapContainerView.setVisibility(View.GONE);
            if (noInternetContainerView != null)
                noInternetContainerView.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        Localisation.getInstance().onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager
                .getInstance(requireActivity())
                .unregisterReceiver(mGeoLocationReceiver);
        hideLoading();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        gatewayCreationViewModel.cleanListener();

        //resume connection snackBar if the next step is not GatewayConnectionFailureFragment
        if(getActivity() instanceof HomeActivity && !startsGatewayConnectionFailure) ((HomeActivity)getActivity()).resumeConnectionObserver();

    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;

        googleMap.setOnInfoWindowClickListener(getOnInfoWindowClickListener());
        googleMap.setOnMapLongClickListener(getOnMapLongClickListener());
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setInfoWindowAdapter(getInfoWindowAdapter());
        googleMap.setOnMapLoadedCallback(() -> {
            // Bounds All markers
            if (null == marker) {
                updateMarker();
                if (finePermissionAllowed){
                    if(marker != null)
                       marker.setVisible(false);
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 6f);
                    googleMap.animateCamera(cameraUpdate);
                }else {
                    if(!setAutomaticPosition)
                    {
                        marker.setVisible(true);
                        btnValid.setEnabled(true);
                    }
                    else
                    {
                        marker.setVisible(false);
                        btnValid.setEnabled(false);
                    }
                    animateCamera();
                }
            }

        });

        googleMap.setOnMarkerDragListener(getOnMarkerDragListener());

        googleMap.setOnMapClickListener(latLng -> setAutomaticPosition = false);
    }

    private void animateCamera(){
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 6);
        googleMap.animateCamera(cameraUpdate);
    }

    private GoogleMap.OnMarkerDragListener getOnMarkerDragListener() {

        GoogleMap.OnMarkerDragListener onMarkerDragListener = new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDrag(Marker arg0) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // modify the site according to the new coordinates
            }

            @Override
            public void onMarkerDragStart(Marker arg0) {
            }

        };

        return onMarkerDragListener;
    }

    private GoogleMap.InfoWindowAdapter getInfoWindowAdapter() {
        GoogleMap.InfoWindowAdapter infoWindowAdapter = new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoContents(Marker marker) {
                // Setting up the infoWindow with current's marker info
//                infoTitle.setText(marker.getTitle());
//                infoSnippet.setText(marker.getSnippet());
                //               return infoWindow;
                return null;
            }

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

        };
        return infoWindowAdapter;
    }

    private GoogleMap.OnMapLongClickListener getOnMapLongClickListener() {
        GoogleMap.OnMapLongClickListener onMapLongClickListener = new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng coordinates) {
                setAutomaticPosition = false;
                initialLatitude = coordinates.latitude;
                initialLongitude = coordinates.longitude;
                updateMarker();
                if(marker != null)
                   marker.setVisible(true);
                btnValid.setEnabled(true);
            }
        };

        return onMapLongClickListener;
    }


    private GoogleMap.OnInfoWindowClickListener getOnInfoWindowClickListener() {

        GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
            }

        };

        return onInfoWindowClickListener;

    }


    private void updateMarker() {

        markerOption = new MarkerOptions().position(new LatLng(initialLatitude, initialLongitude))
                .title(_gateway.getName()).snippet(GatewayExtensions.getDisplayableAddress(_gateway)).draggable(false);


        BitmapDescriptor bitmapDescriptorFromVector = bitmapDescriptorFromVector(R.drawable.ic_map_marker);
        if(bitmapDescriptorFromVector != null) {
            markerOption.icon(bitmapDescriptorFromVector);
        }


        markerOption.anchor(0.5f, 0.5f);
        if (marker != null)
            marker.remove();
        marker = googleMap.addMarker(markerOption);

    }

    /**
     * Observer add new gateway
     */
    private void initNewGatewayInstallationListener() {
        if (!isNewGatewayInstallation) {
            return;
        }

        //init connection observer
        connectionExecutive.getConnectionState().observe(getViewLifecycleOwner(),connection -> {
            if (connectionExecutive.getGateway() == null) {
                //supposed to be impossible
                log.debug("no gateway");
            }
            else if (_gateway.get_id() != connectionExecutive.getGateway().get_id()) {
                //not same gateway
                log.debug("not same gateway");
            } else {
                switch (connection.getState()) {
                    case TydomContract.TydomConnectionContract.STATE_CONNECTED: {
                        pbLoading.setVisibility(View.GONE);
                        btnValid.setEnabled(true);
                        //we are connected, so we go back to Settings page or Dashboard
                        closeCreationProcess();
                        break;
                    }
                    case TydomContract.TydomConnectionContract.STATE_CONNECTING: {
                        //show loader
                        pbLoading.setVisibility(View.VISIBLE);
                        btnValid.setEnabled(false);
                        break;
                    }
                    case TydomContract.TydomConnectionContract.STATE_DISCONNECTED: {
                        pbLoading.setVisibility(View.GONE);
                        btnValid.setEnabled(true);
                        //connection failed, so go to GatewayConnectionFailureFragment
                        startGatewayConnectionFailureFragment(connection);
                        break;
                    }
                }
            }
        });

        btnSkip.setOnClickListener(view  -> {
            showSkipStepDialog();
        });
        gatewayCreationViewModel.getAddNewGatewayResult().observe(getViewLifecycleOwner(), result -> {
            switch (result.getStatus()) {
                case NONE: {
                    pbLoading.setVisibility(View.GONE);
                    btnValid.setEnabled(false);
                    btnSkip.setEnabled(true);
                    _backButton.setEnabled(true);
                    locateMe.setEnabled(true);
                    if(googleMap!=null)googleMap.setOnMapLongClickListener(getOnMapLongClickListener());
                    break;
                }
                case LOADING: {
                    pbLoading.setVisibility(View.VISIBLE);
                    btnValid.setEnabled(false);
                    btnSkip.setEnabled(false);
                    _backButton.setEnabled(false);
                    locateMe.setEnabled(false);
                    if(googleMap!=null)googleMap.setOnMapLongClickListener(null);
                    break;
                }
                case SUCCESS: {
                    _backButton.setEnabled(true);
                    //we refresh the gateway, to get the new one:
                    GatewayManager gatewayManager = new GatewayManager(getContext().getContentResolver());
                    _gateway = gatewayManager.getGatewayFromId(result.getGatewayIdL());
                    //we try to connect directly to the new gateway if it's not a tyfom touch:
                    if(_gateway.getImei() == null){
                        connectionExecutive.connect(_gateway,true);
                    }
                    //if this is a tydom touch, go directy to association page via GatewayConnectionFailureFragment
                    else {
                        startGatewayConnectionFailureFragment(null);
                    }
                    break;
                }
                case ERROR_BELONG_TO_USER: {
                    pbLoading.setVisibility(View.GONE);
                    btnValid.setEnabled(true);
                    btnSkip.setEnabled(true);
                    _backButton.setEnabled(true);
                    locateMe.setEnabled(true);
                    if(googleMap!=null)googleMap.setOnMapLongClickListener(getOnMapLongClickListener());
                    showErrorDialog(
                            getContext(),
                            null,
                            getString(R.string.SITE_MAC_ADDRESS_DIALOG_HEADER),
                            getString(R.string.SITE_MAC_ADDRESS_DIALOG_MESSAGE)
                    );
                    break;
                }
                // TODO: Display message according to error (not yet specified in spec)
                default: {
                    pbLoading.setVisibility(View.GONE);
                    btnValid.setEnabled(true);
                    btnSkip.setEnabled(true);
                    _backButton.setEnabled(true);
                    locateMe.setEnabled(true);
                    if(googleMap!=null)googleMap.setOnMapLongClickListener(getOnMapLongClickListener());
                    showErrorDialog(
                            getContext(),
                            null,
                            getString(R.string.ERROR_MESSAGE_FAILED_REQUEST_TITLE),
                            getString(R.string.ERROR_MESSAGE_FAILED_REQUEST_SUBTITLE)
                    );
                    break;
                }
            }
        });
    }

    private void closeCreationProcess(){
        if(getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
        if(getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }

    }

    private void startGatewayConnectionFailureFragment(@Nullable Connection connection){
        startsGatewayConnectionFailure = true;
        GatewayConnectionFailureFragment fragment = new GatewayConnectionFailureFragment(
                _gateway.getImei() == null ? GatewayConnectionFailureFragment.DisplayMode.SETTINGS_WITH_NAVBAR : GatewayConnectionFailureFragment.DisplayMode.SETTINGS_TOUCH_WITH_NAVBAR,
                rolesList,
                false,
                connection != null ? connection.getPlatformInfo() : null,
                true);
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.settings_back_enter, R.anim.settings_exit, R.anim.settings_popenter, R.anim.settings_popexit);
        transaction.replace(R.id.fragment_container, fragment, fragment.getClass().getName());
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();

        //if installer, there is a home toolbar, so let's remove it
        if(fromInstaller && _homeLayout != null){
            _homeLayout.setBottomBarAndToolbarVisibility(false,false);
        }
    }

    private void onAddNewGateway(double longitude, double latitude) {
        rolesList = new ArrayList<>();
        rolesList.add(InvitationRoles.OWNER.name());
        if (fromInstaller) {
            rolesList.add(InvitationRoles.INSTALLER.name());
        }

        String imei = null;
        String mac = null;
        //if length is 15, it's an IMEI
        if(macAddressNumber != null && macAddressNumber.length() == 15) imei = macAddressNumber;
            //should be length 6 (add site btn is disabled when it's not 6 or 15)
        else mac = macAddressNumber;

        gatewayCreationViewModel.addNewGateway(
                mac,
                imei,
                tydomName,
                AppUtils.manageCoordinates(longitude),
                AppUtils.manageCoordinates(latitude),
                rolesList
        );
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(setAutomaticPosition){
            initialLatitude = location.getLatitude();
            initialLongitude = location.getLongitude();
            updateMarker();
            btnValid.setEnabled(true);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 12f);
            googleMap.animateCamera(cameraUpdate);

            setAutomaticPosition = false;

        }
    }

    @Override
    public void onAccessLocationGranted() {
        if (googleMap != null) {
            //noinspection MissingPermission
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void showLocationPermissionDialog() {
        showTwoButtonDialog(
                requireContext(),
                true,
                getString(R.string.SETTINGS_NEW_SITE_LOCATE_ME),
                getString(R.string.SETTINGS_NEW_SITE_LOCATION_DENIED_TEXT),
                getString(R.string.SETTINGS_BOTTOM_TITLE),
                () -> {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(SCHEME_PACKAGE_NAME, getContext().getPackageName(), null)));
                },
                () -> { }
        );
    }

    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), vectorResId);
        Bitmap bitmap = null;
        if(drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
        }
        if(bitmap == null) return null;
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void updateTimeZoneForAllCameras(String timeZone) {
        mExecutor.networkIO().execute(() -> {
            List<CameraEntity> cameras = mRepository.getCamerasForGateway(_gateway.get_id());
            mExecutor.mainThread().execute(() -> updateTimeZoneByCamera(
                    cameras.size(),
                    cameras,
                    timeZone
            ));
        });
    }

    private void updateTimeZoneByCamera(
            int size,
            List<CameraEntity> cameras,
            String timeZone
    ) {
        if (0 < size) {
            CameraEntity camera = cameras.get(size - 1);
            mRepository.updateTimeZoneByCamera(
                    camera.getSerialNumber(),
                    camera.getPassword(),
                    timeZone
            ).observe(getViewLifecycleOwner(), status -> {
                if (status.getState() == Status.State.SUCCESS) {
                    updateTimeZoneByCamera(
                            size - 1,
                            cameras,
                            timeZone
                    );
                } else if (status.getState() == Status.State.ERROR) {
                    showErrorUpdateTimeZone();
                }
            });
        } else {
            onLocationSaved();
        }
    }

    private void showErrorUpdateTimeZone() {
        hideLoading();
        showOneButtonDialog(
                requireContext(),
                getString(R.string.CAMERA_UPDATE_TIMEZONE_ERROR_TITLE),
                getString(R.string.CAMERA_UPDATE_TIMEZONE_ERROR_TEXT),
                null
        );
    }

    private void showSkipStepDialog() {
        showTwoButtonDialog(
                requireContext(),
                false,
                getString(R.string.RE2020_WEATHER_ALERT_TITLE),
                getString(R.string.LOCATION_WARNING),
                getString(R.string.LOCATION_WARNING_SKIP_STEP),
                () -> { onAddNewGateway(GatewayManager.PARIS_LONGITUDE, GatewayManager.PARIS_LATITUDE); },
                () -> { }
        );
    }

    /**
     * After localisation saved, hide loading and navigate back
     */
    private void onLocationSaved() {
        hideLoading();
        if (isNewGatewayInstallation) {
            requireActivity().onBackPressed();
        }
        requireActivity().onBackPressed();
    }

    private void showInformationDialog() {
        if(getContext() == null) return;
        if (null == infoDialog) {
            infoDialog = TydomAlertDialog.Companion.createTydomAlertDialog(
                    getContext(),
                    getString(R.string.USERACCOUNT_SETTINGS_NEW_SITE_LOCATION_TITLE),
                    getString(R.string.USERACCOUNT_SETTINGS_NEW_SITE_HOME_SITUATION_INFORMATION),
                    getString(R.string.COMMON_OK)
            );
        }

        if (!infoDialog.isShowing()) {
            infoDialog.show();
        }
    }

    private final BroadcastReceiver mGeoLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getExtras();
            if (data.getBoolean(JsonConstants.TAG_GEOLOCATION_VALID_RESULT)) {
                updateTimeZoneForAllCameras(
                        data.getString(JsonConstants.TAG_GEOLOCATION_TIMEZONE_RESULT)
                );
            } else if (data.getBoolean(JsonConstants.TAG_GEOLOCATION_SAME)) {
                onLocationSaved();
            } else {
                showErrorUpdateTimeZone();
            }
        }
    };

}
