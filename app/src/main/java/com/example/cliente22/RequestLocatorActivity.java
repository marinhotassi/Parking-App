package com.example.cliente22;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cliente22.Common.Common;
import com.example.cliente22.Model.EventBus.SelectePlaceEvent;
import com.example.cliente22.Model.LocatorGeoModel;
import com.example.cliente22.Remote.IGoogleAPI;
import com.example.cliente22.Remote.RetrofitClient;
import com.example.cliente22.Utils.UserUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.ui.IconGenerator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class RequestLocatorActivity extends FragmentActivity implements OnMapReadyCallback {


    TextView txt_origin;

    //Slowly camera spinning
    private ValueAnimator animator;
    private static final int DESIRED_NUM_OF_SPINS = 5;
    private static int DESIRED_SECONDS_PER_ONE_FULL_360_SPIN=40;

    //Effect
    private Circle lastUserCircle;
    private long duration = 1000;
    private ValueAnimator lastPulseAnimator;


    //View
    @BindView(R.id.main_layout)
    RelativeLayout main_layout;
    @BindView(R.id.finding_your_parking_layout)
    CardView finding_your_parking_layout;
    @BindView(R.id.layout_confirm_parking)
    CardView layout_confirm_parking;
    @BindView(R.id.btn_confirm_parking)
    Button btn_confirm_parking;
    @BindView(R.id.confirm_pickup_layout)
    CardView confirm_pickup_layout;
    @BindView(R.id.btn_confirm_pickup)
    Button btn_confirm_pickup;
    @BindView(R.id.txt_address_pickup)
    TextView txt_address_pickup;

    @BindView(R.id.fill_maps)
    View fill_maps;

    @OnClick(R.id.btn_confirm_parking)
    void onCorfirmParking(){
        confirm_pickup_layout.setVisibility(View.VISIBLE);
        layout_confirm_parking.setVisibility(View.GONE);

        setDataPickup();
    }

    @OnClick(R.id.btn_confirm_pickup)
    void onCorfirmPickup(){
        if(mMap == null) return;
        if(selectePlaceEvent == null) return;

        //Clear map
        mMap.clear();
        //Tilt
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(selectePlaceEvent.getOrigin())
                .tilt(45f)
                .zoom(16f)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        //Start animation
        addMarkerWithPulseAnimation();

    }

    private void addMarkerWithPulseAnimation() {
        confirm_pickup_layout.setVisibility(View.GONE);
        fill_maps.setVisibility(View.VISIBLE);
        finding_your_parking_layout.setVisibility(View.VISIBLE);

        originMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker())
                .position(selectePlaceEvent.getOrigin()));

        addPulsatingEffect(selectePlaceEvent.getOrigin());
    }

//    ValueAnimator valueAnimator(long duration,ValueAnimator.AnimatorUpdateListener listener){
//
//    }

    private void addPulsatingEffect(LatLng origin) {
        if(lastPulseAnimator != null) lastPulseAnimator.cancel();
        if(lastUserCircle != null) lastUserCircle.setCenter(origin);

        lastPulseAnimator = Common.valueAnimate(duration,animation -> {
            if(lastUserCircle != null) lastUserCircle.setRadius((Float)animation.getAnimatedValue());
            else{
                lastUserCircle = mMap.addCircle(new CircleOptions()
                .center(origin)
                .radius((Float)animation.getAnimatedValue())
                .strokeColor(Color.WHITE)
                        .fillColor(Color.parseColor("#33333333"))
                );
            }
        });

        startMapCameraSpinningAnimation(origin);
    }

    private void startMapCameraSpinningAnimation(LatLng target) {
        if(animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0,DESIRED_NUM_OF_SPINS*360);
        animator.setDuration(DESIRED_NUM_OF_SPINS*DESIRED_NUM_OF_SPINS*1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setStartDelay(100);
        animator.addUpdateListener(valueAnimator -> {
            Float newBearingValue = (Float) valueAnimator.getAnimatedValue();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
            .target(target)
            .zoom(16f)
            .tilt(45f)
            .bearing(newBearingValue)
            .build()));
        });
        animator.start();


        //After start animation. find parking
        findNearbyLocator(target);
    }

    private void findNearbyLocator(LatLng target) {
        if(Common.locatorsFound.size() > 0)
        {
            float min_distance = 0; //default min distance = 0;
            LocatorGeoModel foundLocator = Common.locatorsFound.get(Common.locatorsFound.keySet().iterator().next());//Default set first locator is locator found
            Location currentClientLocation = new Location("");
            currentClientLocation.setLatitude(target.latitude);
            currentClientLocation.setLongitude(target.longitude);
            for(String key:Common.locatorsFound.keySet())
            {
                Location locatorLocation = new Location("");
                locatorLocation.setLatitude(Common.locatorsFound.get(key).getGeoLocation().latitude);
                locatorLocation.setLongitude(Common.locatorsFound.get(key).getGeoLocation().longitude);

                //Compare 2 location
                if(min_distance == 0)
                {
                    min_distance = locatorLocation.distanceTo(currentClientLocation); //First default min_distance
                    foundLocator = Common.locatorsFound.get(key);
                }
                else if(locatorLocation.distanceTo(currentClientLocation) < min_distance)
                {
                    //If have any locator smaller min_distance, just get it!
                    min_distance = locatorLocation.distanceTo(currentClientLocation); //First default min_distance
                    foundLocator = Common.locatorsFound.get(key);
                }
//                Snackbar.make(main_layout,new StringBuilder("Found locator: ")
//                .append(foundLocator.getLocatorInfoModel().getPhoneNumber()),
//                        Snackbar.LENGTH_LONG).show();

                UserUtils.sendRequestToLocator(this,main_layout,foundLocator,target);
            }
        }
        else
        {
            //Not found
            Snackbar.make(main_layout,getString(R.string.locators_not_found),Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if(animator != null) animator.end();
        super.onDestroy();
    }

    private void setDataPickup() {
        txt_address_pickup.setText(txt_origin != null ? txt_origin.getText() : "None");
        mMap.clear();//Clean all on map
        //Add PickupMarker
        addPickupMarker();
    }

    private void addPickupMarker() {
        View view = getLayoutInflater().inflate(R.layout.pickup_info_windows,null);
        //Create Icon for Marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();

        originMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectePlaceEvent.getOrigin()));

    }

    private GoogleMap mMap;

    private SelectePlaceEvent selectePlaceEvent;

    //Routes
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private Polyline blackPolyline,greyPolyLine;
    private PolylineOptions polylineOptions,blackPolylineOptions;
    private List<LatLng> polylineList;

    private Marker originMarker, destinationMarker;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
        if(EventBus.getDefault().hasSubscriberForEvent(SelectePlaceEvent.class))
            EventBus.getDefault().removeStickyEvent(SelectePlaceEvent.class);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onSelectePlaceEvent(SelectePlaceEvent event)
    {
        selectePlaceEvent = event;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_locator);

        init();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void init() {
        ButterKnife.bind(this);
        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        drawPath(selectePlaceEvent);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.parking_maps_style));
            if (!success)
                Toast.makeText(this, "Load map style failed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPath(SelectePlaceEvent selectePlaceEvent) {
        compositeDisposable.add(iGoogleAPI.getDirections("locating",
                "less_locating",
                selectePlaceEvent.getOriginString(),selectePlaceEvent.getDestinationString(),
                getString(R.string.google_api_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(returnResult -> {
                    Log.d("API_RETURN",returnResult);

                    try{
                        JSONObject jsonObject = new JSONObject(returnResult);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for(int i=0;i<jsonArray.length();i++)
                        {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);

                        }

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);
                        greyPolyLine = mMap.addPolyline(polylineOptions);

                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polylineList);
                        blackPolyline = mMap.addPolyline(blackPolylineOptions);


                        //Animator
                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,100);
                        valueAnimator.setDuration(1100);
                        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(value -> {
                           List<LatLng> points = greyPolyLine.getPoints();
                           int percentValue = (int)value.getAnimatedValue();
                           int size = points.size();
                           int newPoints = (int)(size*(percentValue/100.0f));
                           List<LatLng> p = points.subList(0,newPoints);
                           blackPolyline.setPoints(p);
                        });

                        valueAnimator.start();

                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                .include(selectePlaceEvent.getOrigin())
                                .include(selectePlaceEvent.getDestination())
                                .build();

                        //Add car icon for origin
                        JSONObject object = jsonArray.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObjects = legs.getJSONObject(0);

                        JSONObject time = legObjects.getJSONObject("duration");
                        String duration =  time.getString("text");

                        String start_address = legObjects.getString("start_address");
                        String end_address = legObjects.getString("end_address");

                       addOriginMarker(duration,start_address);

                        addDestinationMarker(end_address);

                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,160));
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom-1));

                    }catch (Exception e)
                    {
                        //Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_LONG).show();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    private void addDestinationMarker(String end_address) {
        View view = getLayoutInflater().inflate(R.layout.destination_info_windows,null);
        TextView txt_destination = (TextView)view.findViewById(R.id.txt_destination);

        txt_destination.setText(Common.formatAddress(end_address));

        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();

        destinationMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectePlaceEvent.getDestination()));

    }

    private void addOriginMarker(String duration, String start_address){
        View view = getLayoutInflater().inflate(R.layout.origin_info_windows,null);

        TextView txt_time = (TextView)view.findViewById(R.id.txt_time);
        txt_origin = (TextView)view.findViewById(R.id.txt_origin);

        txt_time.setText(Common.formatDuration(duration));
        txt_origin.setText(Common.formatAddress(start_address));

        //Create Icon for Marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();

        originMarker = mMap.addMarker(new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromBitmap(icon))
        .position(selectePlaceEvent.getOrigin()));


    }


}
