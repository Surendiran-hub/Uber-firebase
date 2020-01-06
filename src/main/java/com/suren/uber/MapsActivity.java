package com.suren.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.suren.uber.Common.Common;
import com.suren.uber.Remote.IGoogleApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;

    private static final int  PERMISSION_REQUEST_CODE = 7000;
    private static final int SERVICE_REQUEST_CODE = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient googleApiClient = null;
    private Location lastLocation;


    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;

    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    //animation

     List polyLineList;
     Marker carMarker;
     float v;
     double lat,lng;
     Handler handler;
     LatLng startPosition,endPosition,currentPosotion;
     int index,next;
     Button BtnGo;
     EditText edtPlace;
     String destination;
     PolylineOptions polylineOptions, blackpolylineOptions;
     Polyline blackpolyline,greypolyline;


    private IGoogleApi mService;

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
         if (index<polyLineList.size()-1){
             index++;
             next = index +1;
         }
         if (index<polyLineList.size()-1){
             startPosition= (LatLng) polyLineList.get(index);
         endPosition= (LatLng) polyLineList.get(next);
         }
         ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
         valueAnimator.setDuration(2000);
         valueAnimator.setInterpolator(new LinearInterpolator());
         valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
             @Override
             public void onAnimationUpdate(ValueAnimator animation) {
                 v=animation.getAnimatedFraction();
                 lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                 lat = v*endPosition.latitude+(1-v)*startPosition.latitude;

                 LatLng newPos = new LatLng(lat,lng);
                 carMarker.setPosition(newPos);
                 carMarker.setAnchor(0.5f,0.5f);
                 carMarker.setRotation(getBearing(startPosition,newPos));
                 mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                         .target(newPos)
                         .zoom(15.5f)
                         .build()
                 ));
             }
         });
         valueAnimator.start();
         handler.postDelayed(this,3000);
        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {

        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lng = Math.abs(startPosition.longitude - endPosition.longitude);

        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (startPosition.latitude < endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);

        return -1;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
         mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        location_switch = findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline)
                {
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(),"You are online",Snackbar.LENGTH_SHORT).show();
                }
                else
                {
                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(),"You are offline",Snackbar.LENGTH_SHORT).show();

                }
            }
        });

        polyLineList = new ArrayList<>();
        BtnGo=findViewById(R.id.user);
        edtPlace=findViewById(R.id.edtPlace);

        BtnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination=edtPlace.getText().toString();
                destination=destination.replace(" ","+");
                Log.d("EMDTV",destination);
                getDirection();
            }
        });


        //Geofire
        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire = new GeoFire(drivers);
        setUpLocation();
        mService= Common.getGoogleApi();
    }

    private void getDirection() {

        currentPosotion=new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        String requestApi = null;
        try {
             requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                     "mode=driving&"+
                        "transit_routing_preferences=less_driving"+
                     "&origin="+currentPosotion.latitude+","+currentPosotion.longitude+"&"+
                     "destination="+destination+"&"+
                     "key="+getResources().getString(R.string.google_direction_key);

             Log.d("emdt",requestApi);

             mService.getPath(requestApi).enqueue(new Callback<String>() {
                 @Override
                 public void onResponse(Call<String> call, Response<String> response) {

                     try {
                         JSONObject jsonObject = new JSONObject(response.body().toString());
                         JSONArray jsonArray = jsonObject.getJSONArray("routes");
                         for (int i=0;i<jsonArray.length();i++)
                         {
                             JSONObject route = jsonArray.getJSONObject(i);
                             JSONObject poly =route.getJSONObject("overview_polyline");
                             String polyline = poly.getString("points");
                             polyLineList = decodePoly(polyline);
                         }

                         LatLngBounds.Builder builder = new LatLngBounds.Builder();
                         for (Object latLng:polyLineList)builder.include((LatLng) latLng);

                         LatLngBounds bounds =builder.build();
                         CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,2);
                         mMap.animateCamera(mCameraUpdate);


                         polylineOptions =new PolylineOptions();
                         polylineOptions.color(Color.BLUE);
                         polylineOptions.width(5);
                         polylineOptions.startCap(new SquareCap());
                         polylineOptions.endCap(new SquareCap());
                         polylineOptions.jointType(JointType.ROUND);
                         polylineOptions.addAll(polyLineList);
                         greypolyline=mMap.addPolyline(polylineOptions);

                         blackpolylineOptions =new PolylineOptions();
                         blackpolylineOptions.color(Color.BLUE);
                         blackpolylineOptions.width(5);
                         blackpolylineOptions.startCap(new SquareCap());
                         blackpolylineOptions.endCap(new SquareCap());
                         blackpolylineOptions.jointType(JointType.ROUND);
                         blackpolylineOptions.addAll(polyLineList);
                         blackpolyline=mMap.addPolyline(blackpolylineOptions);


                         mMap.addMarker(new MarkerOptions().position(
                                 (LatLng) polyLineList.get(polyLineList.size()-1))
                                 .title("pickup"));


                         ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                         polylineAnimator.setDuration(2000);
                         polylineAnimator.setInterpolator(new LinearInterpolator());
                         polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                             @Override
                             public void onAnimationUpdate(ValueAnimator animation) {
                                 List<LatLng> points = greypolyline.getPoints();
                                 int percentValue = (int) animation.getAnimatedValue();
                                 int size = points.size();
                                 int newPoints = (int)(size * (percentValue/100.0f));
                                 List<LatLng> p =points.subList(0,newPoints);
                                 blackpolyline.setPoints(p);

                             }
                         });

                         polylineAnimator.start();

                         carMarker = mMap.addMarker(new MarkerOptions().position(currentPosotion).flat(true)
                         .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));

                         handler = new Handler();
                         index = -1;
                         next = 1;

                         handler.postDelayed(drawPathRunnable,3000);

                     } catch (Exception e) {
                         e.printStackTrace();
                     }

                 }

                 @Override
                 public void onFailure(Call<String> call, Throwable t) {
                     Toast.makeText(MapsActivity.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();

                 }
             });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private List decodePoly(String encoded) {

            List poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case    PERMISSION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPLayServices()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked())
                            displayLocation();
                    }
                }
        }    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_CODE);
        }else{
            if (checkPLayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                if(location_switch.isChecked()){
                    displayLocation();
                }
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest =new LocationRequest();
        int UPDATE_INTERVAL = 5000;
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        int fastest_interval = 3000;
        mLocationRequest.setFastestInterval(fastest_interval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        int displacement = 10;
        mLocationRequest.setSmallestDisplacement(displacement);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private boolean checkPLayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,SERVICE_REQUEST_CODE).show();
            else{
                Toast.makeText(getApplicationContext(),"not supported",Toast.LENGTH_SHORT).show();
                finish();
            }
            return  false;
        }
        return true;
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ))
        {
            return;
        }
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation!=null) {

            if (location_switch.isChecked()) {

                final double latitude = lastLocation.getLatitude();
                final double longitude = lastLocation.getLongitude();

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent!=null){
                            mCurrent.remove();
                            mCurrent=mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude,longitude)).title("Your location"));

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));

                            rotateMarker(mCurrent,-360,mMap);
                        }
                    }
                });
            }else{
                Log.d("Error","Can't get your location");
            }
        }
    }

    private void rotateMarker(final Marker mCurrent, final int i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startrotation = mCurrent.getRotation();
        final  long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis()-start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot = t*i+(1-t)*startrotation;
                mCurrent.setRotation(-rot > -180?rot/2:rot);
                if (t<1.0){
                    handler.postDelayed(this,16);
                }
            }
        });

    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ))
        {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ))
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,mLocationRequest,this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                .position(new LatLng(12.990142, 80.260806)).title("You"));
//        LatLng icm = new LatLng(12.990142, 80.260806);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(icm));
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(icm,10));
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        createLocationRequest();
    }


}