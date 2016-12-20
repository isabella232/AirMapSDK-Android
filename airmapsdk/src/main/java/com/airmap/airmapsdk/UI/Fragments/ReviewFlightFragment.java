package com.airmap.airmapsdk.ui.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.AirMapLog;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.models.permits.AirMapAvailablePermit;
import com.airmap.airmapsdk.models.permits.AirMapPilotPermit;
import com.airmap.airmapsdk.models.pilot.AirMapPilot;
import com.airmap.airmapsdk.models.shapes.AirMapPath;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.models.status.AirMapStatus;
import com.airmap.airmapsdk.models.status.AirMapStatusRequirementNotice;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.util.AnnotationsFactory;
import com.mapbox.mapboxsdk.annotations.MultiPoint;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.airmap.airmapsdk.util.Utils.dpToPixels;

/**
 * Created by Vansh Gandhi on 7/25/16.
 * Copyright © 2016 AirMap, Inc. All rights reserved.
 */
public class ReviewFlightFragment extends Fragment implements OnMapReadyCallback {

    private OnFragmentInteractionListener mListener;

    private MapView mapView;
    private ViewPager viewPager;
    private Button submitButton;
    private FrameLayout progressBarContainer;
    private int totalNumberOfPermits;
    private int totalPermitsObtained = 0;
    int sizeInDp;
    float scale;
    int dpAsPixels;

    private MapboxMap map;

    public ReviewFlightFragment() {
        //Required empty constructor
    }

    public static ReviewFlightFragment newInstance() {
        return new ReviewFlightFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_flight, container, false);
        initializeViews(view);
        setupMap(savedInstanceState);
        setupViewPager();
        sizeInDp = 16;
        scale = getResources().getDisplayMetrics().density;
        dpAsPixels = (int) (sizeInDp * scale + 0.5f);
        return view;
    }

    private void initializeViews(View view) {
        mapView = (MapView) view.findViewById(R.id.airmap_map);
        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        progressBarContainer = (FrameLayout) view.findViewById(R.id.progress_bar_container);
        submitButton = (Button) view.findViewById(R.id.airmap_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBarContainer.setVisibility(View.VISIBLE);
                applyPermitsToFlight();
                submitButton.setEnabled(false);
            }
        });
        PagerTabStrip strip = (PagerTabStrip) view.findViewById(R.id.tab_strip);
        strip.setTabIndicatorColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    private void applyPermitsToFlight() {
        totalNumberOfPermits = mListener.getSelectedPermits().size() + mListener.getPermitsToApplyFor().size();
        if (mListener.getPermitsToApplyFor().size() == 0) {
            submitFlight();
        } else {
            applyForPermits(); //Will call submitFlight after last permit is applied for
        }
    }

    private void applyForPermits() {
        ArrayList<AirMapAvailablePermit> copy = new ArrayList<>(mListener.getPermitsToApplyFor()); //We iterate over the copy, remove from the original (because we are in a different thread)
        for (final AirMapAvailablePermit permitToApplyFor : copy) {
            AirMap.applyForPermit(permitToApplyFor, new AirMapCallback<AirMapPilotPermit>() {
                @Override
                public void onSuccess(AirMapPilotPermit response) {
                    mListener.getFlight().addPermitId(response.getApplicationId());
                    mListener.getPermitsToApplyFor().remove(permitToApplyFor);
                    totalPermitsObtained++;
                    if (totalPermitsObtained == totalNumberOfPermits) {
                        submitFlight();
                    }
                }

                @Override
                public void onError(final AirMapException e) {
                    submitButton.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBarContainer.setVisibility(View.GONE);
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            submitButton.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    private void submitFlight() {
        for (AirMapPilotPermit selectedPermit : mListener.getSelectedPermits()) {
            mListener.getFlight().addPermitId(selectedPermit.getApplicationId());
        }

        if (mListener.getFlight().shouldNotify()) {
            String phone = mListener.getPilot().getPhone();
            if (phone == null || phone.isEmpty() || !mListener.getPilot().getVerificationStatus().isPhone()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //the phone dialog will submit flight once verified
                        showPhoneDialog();
                    }
                });
            } else {
                doSubmitFlight();
            }
        } else {
            doSubmitFlight();
        }

    }

    void doSubmitFlight() {
        if (mListener.getFlight().getStartsAt() != null) {
            if (mListener.getFlight().getStartsAt().before(new Date())) { //If the startsAt date is in past
                long duration = mListener.getFlight().getEndsAt().getTime() - mListener.getFlight().getStartsAt().getTime();
                Date currentTime = new Date();
                mListener.getFlight().setStartsAt(currentTime);
                mListener.getFlight().setEndsAt(new Date(currentTime.getTime() + duration));
            }
        }
        AirMap.createFlight(mListener.getFlight(), new AirMapCallback<AirMapFlight>() {
            @Override
            public void onSuccess(AirMapFlight response) {
                mListener.onFlightSubmitted(response);
            }

            @Override
            public void onError(final AirMapException e) {
                submitButton.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBarContainer.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                AirMapLog.e("AirMapReviewFlight", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void setupMap(Bundle savedInstanceState) {
        mapView.getMapAsync(this);
        mapView.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        if (mListener != null) {
            AirMapFlight flight = mListener.getFlight();
            MultiPoint multiPoint;
            if (flight.getGeometry() instanceof AirMapPolygon) {
                PolygonOptions polygonOptions = mListener.getAnnotationsFactory().getDefaultPolygonOptions();
                PolylineOptions polylineOptions = mListener.getAnnotationsFactory().getDefaultPolylineOptions();
                for (Coordinate coordinate : ((AirMapPolygon) flight.getGeometry()).getCoordinates()) {
                    polygonOptions.add(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()));
                    polylineOptions.add(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()));
                }
                map.addPolygon(polygonOptions);
                multiPoint = map.addPolyline(polylineOptions.add(polylineOptions.getPoints().get(0)));
            } else if (flight.getGeometry() instanceof AirMapPath) {
                PolylineOptions polylineOptions = mListener.getAnnotationsFactory().getDefaultPolylineOptions();
                for (Coordinate coordinate : ((AirMapPath) flight.getGeometry()).getCoordinates()) {
                    polylineOptions.add(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()));
                }

                for (List<LatLng> polygonPoints : mListener.getPathBuffers()) {
                    PolygonOptions polygonOptions = mListener.getAnnotationsFactory().getDefaultPolygonOptions().addAll(polygonPoints);
                    map.addPolygon(polygonOptions); //Add polygon first, then line for proper z ordering
                }
                multiPoint = map.addPolyline(polylineOptions);
            } else {
                List<LatLng> circlePoints = mListener.getAnnotationsFactory().polygonCircleForCoordinate(new LatLng(flight.getCoordinate().getLatitude(), flight.getCoordinate().getLongitude()), flight.getBuffer());
                map.addPolygon(mListener.getAnnotationsFactory().getDefaultPolygonOptions().addAll(circlePoints));
                multiPoint = map.addPolyline(mListener.getAnnotationsFactory().getDefaultPolylineOptions().addAll(circlePoints).add(circlePoints.get(0)));
            }
            LatLngBounds bounds = new LatLngBounds.Builder().includes(multiPoint.getPoints()).build();
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPixels(getActivity(), 20).intValue()));
        }
    }

    private void setupViewPager() {
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(ReviewDetailsFragment.newInstance(mListener.getFlight()));
        if (!mListener.getPermitsToShowInReview().isEmpty()) {
            fragments.add(ReviewPermitsFragment.newInstance(mListener.getPermitsToShowInReview(), mListener.getSelectedPermits()));
        }
        if (!mListener.getNotices().isEmpty()) {
            fragments.add(ReviewNoticeFragment.newInstance(mListener.getFlightStatus(), mListener.getFlight().shouldNotify()));
        }
        viewPager.setAdapter(new SectionsPagerAdapter(fragments, getChildFragmentManager()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    private void showPhoneDialog() {
        final DialogInterface.OnClickListener dismissOnClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                submitButton.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBarContainer.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                    }
                });
            }
        };
        final TextInputLayout phoneLayout = new TextInputLayout(getContext()); //The phone EditText
        phoneLayout.setHint("Phone Number");
        TextInputEditText editText = new TextInputEditText(getContext());
        editText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        editText.setMaxLines(1);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        phoneLayout.addView(editText);
        phoneLayout.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, 0);
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setMessage(R.string.airmap_phone_number_disclaimer)
                .setTitle("Phone Number")
                .setView(phoneLayout)
                .setNegativeButton("Cancel", dismissOnClickListener)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() { //Display dialog to enter the verification token
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onSubmitPhoneNumber(phoneLayout);
                        dialog.dismiss();
                    }
                })
                .show();
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onSubmitPhoneNumber(phoneLayout);
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    private void onSubmitPhoneNumber(final TextInputLayout phoneLayout) {
        //noinspection ConstantConditions,ConstantConditions
        AirMap.updatePhoneNumber(phoneLayout.getEditText().getText().toString(), new AirMapCallback<Void>() {
            @Override
            public void onSuccess(Void response) {
                AirMap.sendVerificationToken(new AirMapCallback<Void>() {
                    @Override
                    public void onSuccess(Void response) {
                        showVerifyDialog();
                    }

                    @Override
                    public void onError(AirMapException e) {
                        e.printStackTrace();
                        toast(e.getMessage());
                        submitButton.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBarContainer.setVisibility(View.GONE);
                                submitButton.setEnabled(true);
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(AirMapException e) {
                e.printStackTrace();
                toast(e.getMessage());
                submitButton.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBarContainer.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void showVerifyDialog() {
        final TextInputLayout verifyLayout = new TextInputLayout(getContext()); //The verify token EditText
        verifyLayout.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, 0);
        final TextInputEditText editText = new TextInputEditText(getContext());
        editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        editText.setMaxLines(1);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        verifyLayout.addView(editText);
        mapView.post(new Runnable() { //run on UI thread
            @Override
            public void run() {
                final AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setView(verifyLayout)
                        .setMessage("Enter the verification token that was sent to your phone number")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                                submitButton.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBarContainer.setVisibility(View.GONE);
                                        submitButton.setEnabled(true);
                                    }
                                });
                            }
                        })
                        .setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                onSubmitVerificationToken(verifyLayout);
                            }
                        })
                        .show();
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            dialog.dismiss();
                            onSubmitVerificationToken(verifyLayout);
                            return true;
                        }
                        return false;
                    }
                });
            }
        });
    }

    private void onSubmitVerificationToken(TextInputLayout verifyLayout) {
        //noinspection ConstantConditions,ConstantConditions
        AirMap.verifyPhoneToken(verifyLayout.getEditText().getText().toString(), new AirMapCallback<Void>() {
            @Override
            public void onSuccess(Void response) {
                toast("Successfully verified new number");
                doSubmitFlight();
            }

            @Override
            public void onError(AirMapException e) {
                toast("Error verifying number");
                e.printStackTrace();
                submitButton.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBarContainer.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void toast(final String message) {
        //noinspection ConstantConditions
        getView().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public interface OnFragmentInteractionListener {
        AirMapFlight getFlight();

        AirMapStatus getFlightStatus();

        ArrayList<AirMapAvailablePermit> getPermitsToApplyFor();

        ArrayList<AirMapPilotPermit> getSelectedPermits();

        ArrayList<AirMapAvailablePermit> getPermitsToShowInReview();

        ArrayList<AirMapStatusRequirementNotice> getNotices();

        void onFlightSubmitted(AirMapFlight response);

        AirMapPilot getPilot();

        AnnotationsFactory getAnnotationsFactory();

        List<LatLng>[] getPathBuffers();
    }

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> data;

        public SectionsPagerAdapter(List<Fragment> fragments, FragmentManager fm) {
            super(fm);
            this.data = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return data.get(position);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Fragment fragment = getItem(position);
            if (fragment instanceof ReviewDetailsFragment) {
                return "Flight Details";
            } else if (fragment instanceof ReviewPermitsFragment) {
                return "Permits";
            } else if (fragment instanceof ReviewNoticeFragment) {
                return "Flight Notice";
            } else {
                return "Review";
            }
        }
    }
}
