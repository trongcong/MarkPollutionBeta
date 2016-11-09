package com.project.markpollution;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;
import com.project.markpollution.CustomAdapter.CircleTransform;
import com.project.markpollution.CustomAdapter.FeedRecyclerViewAdapter;
import com.project.markpollution.CustomAdapter.PopupInfoWindowAdapter;
import com.project.markpollution.Objects.Category;
import com.project.markpollution.Objects.PollutionPoint;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, View.OnClickListener,
        GoogleMap.OnInfoWindowClickListener {


//    ClusterManager.OnClusterClickListener<PollutionPoint>,
//    ClusterManager.OnClusterInfoWindowClickListener<PollutionPoint>,
//    ClusterManager.OnClusterItemClickListener<PollutionPoint>,
//    ClusterManager.OnClusterItemInfoWindowClickListener<PollutionPoint>

    ClusterManager<PollutionPoint> ListItemsCluster;
    List<PollutionPoint> items;

    private NavigationView navigationView;
    private FloatingActionButton fabCheck, fabCheckSelect;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private LatLng latLong_my_location;
    private Spinner spnCate;
    private ImageView imgGetLocation;
    public static ArrayList<PollutionPoint> listPo;
    private List<Category> listCate;
    private List<PollutionPoint> listPoByCateID;
    private List<PollutionPoint> listSeriousPo;
    private HashMap<String, Uri> images = new HashMap<>();
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private GoogleApiClient mGoogleApiClient;
    private String url_retrive_pollutionPoint = "http://indi.com.vn/dev/markpollution/RetrievePollutionPoint.php";
    private String url_retrieve_cate = "http://indi.com.vn/dev/markpollution/RetrieveCategory.php";
    private String url_RetrievePollutionByCateID = "http://indi.com.vn/dev/markpollution/RetrievePollutionBy_CateID.php?id_cate=";
    private String url_RetrievePollutionOrderByRate = "http://indi.com.vn/dev/markpollution/RetrievePollutionOrderByRate.php";
    private BottomSheetBehavior bottomSheetBehavior;
    private RecyclerView recyclerViewFeed;

    LatLngBounds.Builder Lbuilder;
    CameraUpdate cameraUpdate;
    List<LatLng> latLngCamUpdateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fetchData();
        initView();

        if (checkPlayServices()) {
            if (!isLocationEnabled(this)) {
                openDiaLogCheckGPS();
                buildGoogleApiClient();
            }
            buildGoogleApiClient();
        } else {
            Toast.makeText(this, "Location not supported in this device", Toast.LENGTH_SHORT).show();
        }


        loadSpinnerCate();

        setNavigationHeader();
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        fabCheck = (FloatingActionButton) findViewById(R.id.fabCheck);
        fabCheckSelect = (FloatingActionButton) findViewById(R.id.fabCheckSelect);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        imgGetLocation = (ImageView) findViewById(R.id.imgGetLocation);
        spnCate = (Spinner) findViewById(R.id.spnCateMap);
        recyclerViewFeed = (RecyclerView) findViewById(R.id.recyclerViewFeed);

        // initialize bottomSheet
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // initialize firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        navigationView.setNavigationItemSelectedListener(this);
        mapFragment.getMapAsync(this);
        fabCheck.setOnClickListener(this);
        fabCheckSelect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabCheck:
                imgGetLocation.setVisibility(View.VISIBLE);
                fabCheckSelect.setVisibility(View.VISIBLE);
                fabCheck.setVisibility(View.GONE);
                break;
            case R.id.fabCheckSelect:
                Intent i = new Intent(this, SendReportActivity.class);
                i.putExtra("Lat", mMap.getCameraPosition().target.latitude);
                i.putExtra("Long", mMap.getCameraPosition().target.longitude);
                startActivity(i);

                imgGetLocation.setVisibility(View.GONE);
                fabCheckSelect.setVisibility(View.GONE);
                fabCheck.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ListItemsCluster = new ClusterManager<>(this, mMap);


//        ListItemsCluster.setRenderer(new PersonRenderer());
//        mMap.setOnCameraIdleListener(ListItemsCluster);
//        mMap.setOnMarkerClickListener(ListItemsCluster);
//        mMap.setOnInfoWindowClickListener(ListItemsCluster);
//        ListItemsCluster.setOnClusterClickListener(this);
//        ListItemsCluster.setOnClusterInfoWindowClickListener(this);
//        ListItemsCluster.setOnClusterItemClickListener(this);
//        ListItemsCluster.setOnClusterItemInfoWindowClickListener(this);

        filterPollutionByCate(mMap);
        items = new ArrayList<>();

        loadRecyclerViewFeed();

        mMap.setInfoWindowAdapter(new PopupInfoWindowAdapter(this, LayoutInflater.from(this), images));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Application hasn't granted permission to access your location", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

//        ListItemsCluster.cluster();

        mMap.setOnCameraIdleListener(ListItemsCluster);

        mMap.setOnInfoWindowClickListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    // Xác minh rằng các dịch vụ Google Play có sẵn và đã update trên thiết bị này chưa
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000).show();
            }
            return false;
        }
        return true;
    }

    private void fetchData() {
        Intent intent = getIntent();
        String po_data = intent.getStringExtra("po_data");
        try {
            JSONObject jObj = new JSONObject(po_data);
            JSONArray arr = jObj.getJSONArray("result");
            listPo = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject po = arr.getJSONObject(i);
                String id_po = po.getString("id_po");
                String id_cate = po.getString("id_cate");
                String id_user = po.getString("id_user");
                double lat = po.getDouble("lat");
                double lng = po.getDouble("lng");
                String title = po.getString("title");
                String desc = po.getString("desc");
                String image = po.getString("image");
                String time = po.getString("time");

                listPo.add(new PollutionPoint(id_po, id_cate, id_user, lat, lng, title, desc, image, time));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // simultaneously listening data changed on Firebase
//        listenDataChanged();
    }

    private void setNavigationHeader() {
        View view = navigationView.getHeaderView(0);
        TextView tvNavName = (TextView) view.findViewById(R.id.username);
        TextView tvNavEmail = (TextView) view.findViewById(R.id.email);
        ImageView ivNavAvatar = (ImageView) view.findViewById(R.id.profile_image);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String email = intent.getStringExtra("email");
        String avatar = intent.getStringExtra("avatar");
        tvNavName.setText(name);
        tvNavEmail.setText(email);
        Picasso.with(this).load(Uri.parse(avatar)).resize(250, 250).transform(new CircleTransform()).into(ivNavAvatar);
    }

    private void loadSpinnerCate() {
        StringRequest stringReq = new StringRequest(Request.Method.GET, url_retrieve_cate, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    JSONArray arr = jObj.getJSONArray("result");
                    listCate = new ArrayList<>();
                    listCate.add(0, new Category("0", "Show All"));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject cate = arr.getJSONObject(i);
                        listCate.add(new Category(cate.getString("id_cate"), cate.getString("name_cate")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ArrayAdapter<Category> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout
                        .simple_spinner_item,
                        listCate);
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
                spnCate.setAdapter(adapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        Volley.newRequestQueue(MainActivity.this).add(stringReq);
    }

    private void addMarker(GoogleMap map, PollutionPoint po) {
        Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(po.getLat(), po.getLng()))
                .title(po.getTitle())
                .snippet(po.getDesc()));
        marker.setTag(po.getId());

//        items.add(new PollutionPoint(po.getLat(), po.getLng()));
//        ListItemsCluster.addItems(items);

        images.put(marker.getId(), Uri.parse(po.getImage()));
        latLngCamUpdateList.add(new LatLng(po.getLat(), po.getLng()));
        nearByMeCamUpdate(latLngCamUpdateList);
    }

    private void getPollutionByCateID(String CateID) {
        String completed_RetrievePollutionByCateID = url_RetrievePollutionByCateID + CateID;
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, completed_RetrievePollutionByCateID, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray arr = response.getJSONArray("result");
                            listPoByCateID = new ArrayList<>();
                            latLngCamUpdateList = new ArrayList<>();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject po = arr.getJSONObject(i);
                                String id_po = po.getString("id_po");
                                String id_cate = po.getString("id_cate");
                                String id_user = po.getString("id_user");
                                double lat = po.getDouble("lat");
                                double lng = po.getDouble("lng");
                                String title = po.getString("title");
                                String desc = po.getString("desc");
                                String image = po.getString("image");
                                String time = po.getString("time");

                                listPoByCateID.add(new PollutionPoint(id_po, id_cate, id_user, lat, lng, title, desc, image, time));
                            }
                            // extract markers in list<> markers into the map
                            mMap.clear();
                            for (PollutionPoint po : listPoByCateID) {
                                addMarker(mMap, po);
//                                latLngCamUpdateList.add(new LatLng(po.getLat(), po.getLng()));
//                                nearByMeCamUpdate(latLngCamUpdateList);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        Volley.newRequestQueue(MainActivity.this).add(objReq);
    }

    private void filterPollutionByCate(final GoogleMap googleMap) {
        spnCate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Category cate = (Category) spnCate.getItemAtPosition(i);
                String CateID = cate.getId();
                latLngCamUpdateList = new ArrayList<>();
                if (!CateID.equals("0")) {
                    getPollutionByCateID(CateID);
                } else {
                    googleMap.clear();
                    for (PollutionPoint po : listPo) {
                        addMarker(googleMap, po);
//                        latLngCamUpdateList.add(new LatLng(po.getLat(), po.getLng()));
//                        nearByMeCamUpdate(latLngCamUpdateList);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void getSeriousPollution() {
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, url_RetrievePollutionOrderByRate, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("success")) {
                                listSeriousPo = new ArrayList<>();
                                JSONArray arr = response.getJSONArray("response");
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject po = arr.getJSONObject(i);
                                    listSeriousPo.add(new PollutionPoint(po.getString("id_po"), po.getString
                                            ("id_cate"), po.getString("id_user"), po.getDouble("lat"), po.getDouble
                                            ("lng"), po.getString("title"), po.getString("desc"), po.getString
                                            ("image"), po.getString("time")));
                                }
                                mMap.clear();
                                for (PollutionPoint po : listSeriousPo) {
                                    addMarker(mMap, po);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Volley", error.getMessage());
            }
        });

        Volley.newRequestQueue(this).add(objReq);
    }

    private void loadRecyclerViewFeed() {
        LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewFeed.setLayoutManager(layout);
        recyclerViewFeed.setAdapter(new FeedRecyclerViewAdapter(this, listPo));

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setPeekHeight(36);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_recentPo) {
        } else if (id == R.id.nav_reportMgr) {
            startActivity(new Intent(this, ReportManagementActivity.class));
        } else if (id == R.id.nav_nearbyPo) {
            if (!isLocationEnabled(this)) {
                openDiaLogCheckGPS();
                buildGoogleApiClient();
                nearByReport();
            } else {
                nearByReport();
            }
        } else if (id == R.id.nav_seriousPo) {
            getSeriousPollution();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Dialog yêu cầu kích hoạt GPS
    private void openDiaLogCheckGPS() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.custom_dialog_check_gps, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(alertLayout);
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
        builder.setPositiveButton("OK!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent = new Intent(this, DetailReportActivity.class);
        intent.putExtra("id_po", marker.getTag().toString());
        startActivity(intent);
    }

    protected synchronized void buildGoogleApiClient() {
        // Khởi tạo, cấu hình một GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    //Near By Report
    private void nearByReport() {
        mMap.clear();
        latLngCamUpdateList = new ArrayList<>();
        for (PollutionPoint po : listPo) {
            LatLng latLngR = new LatLng(po.getLat(), po.getLng());
            double distance = SphericalUtil.computeDistanceBetween(latLong_my_location, latLngR);
            // chỉnh khoảng cách trong cài đặt
            if (distance <= 5000) {
                addMarker(mMap, po);
//                latLngCamUpdateList.add(new LatLng(po.getLat(), po.getLng()));
                Log.i("distance add", formatNumber(distance) + " - " + po.toString());
            } else {
                Log.i("distance no add", formatNumber(distance) + " - " + po.toString());
            }
        }
//        nearByMeCamUpdate(latLngCamUpdateList);
    }

    private void nearByMeCamUpdate(List<LatLng> latLngList) {
        /**create for loop for get the latLngbuilder from the marker list*/
        Lbuilder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngList) {
            Lbuilder.include(new LatLng(latLng.latitude, latLng.longitude));
        }
        LatLngBounds bounds = Lbuilder.build();
        /**create the camera with bounds and padding to set into map*/
        cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                /**set animated zoom camera into map*/
                mMap.animateCamera(cameraUpdate);
            }
        });
    }

    private String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }
        return String.format("%4.3f%s", distance, unit);
    }

    // Di chuyển Camera vị trí của người dùng
    private void changeMap(Location location) {
        Log.d("LOG", "Reaching map" + mMap);
        // check if map is created successfully or not
        if (mMap != null) {
            latLong_my_location = new LatLng(location.getLatitude(), location.getLongitude());
            Log.d("latLong_my_location", latLong_my_location.toString() + " ss");
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(latLong_my_location)
//                    .zoom(15f)
//                    .build();
//
//            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            Toast.makeText(this, "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
        }
    }


    //TODO: ==============================================================================================


    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ứng dụng chưa cấp quyền tìm vị trí !!!", Toast.LENGTH_LONG).show();
            return;
        }
        // Xác định vị trí cuối cùng được biết đến của thiết bị người dùng.
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("LOG", "ON connected");
            changeMap(mLastLocation);
        } else
            try {
                // Loại bỏ tất cả các bản cập nhật vị trí cho mục đích chờ nhất định.
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        try {
            // Yêu cầu cập nhật vị trí
            LocationRequest mLocationRequest = new LocationRequest();
            // Thời gian cập nhật vị trí
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            //Độ chính xác của vị trí
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            if (location != null)
                changeMap(location);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // Gọi khi ứng dụng khởi động
    @Override
    public void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public boolean onClusterClick(Cluster<PollutionPoint> cluster) {
//        // Show a toast with some info when the cluster is clicked.
//        String Title = cluster.getItems().iterator().next().getTitle();
//        Toast.makeText(this, cluster.getSize() + " (including " + Title + ")", Toast.LENGTH_SHORT).show();
//
//        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
//        // inside of bounds, then animate to center of the bounds.
//
//        // Create the builder to collect all essential cluster items for the bounds.
//        LatLngBounds.Builder builder = LatLngBounds.builder();
//        for (ClusterItem item : cluster.getItems()) {
//            builder.include(item.getPosition());
//        }
//        // Get the LatLngBounds
//        final LatLngBounds bounds = builder.build();
//
//        // Animate camera to the bounds
//        try {
//            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }
//
//    @Override
//    public void onClusterInfoWindowClick(Cluster<PollutionPoint> cluster) {
//
//    }
//
//    @Override
//    public boolean onClusterItemClick(PollutionPoint pollutionPoint) {
//        return false;
//    }
//
//    @Override
//    public void onClusterItemInfoWindowClick(PollutionPoint pollutionPoint) {
//
//    }
}
