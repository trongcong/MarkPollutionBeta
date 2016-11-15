package com.project.markpollution;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.project.markpollution.CustomAdapter.CircleTransform;
import com.project.markpollution.CustomAdapter.FeedRecyclerViewAdapter;
import com.project.markpollution.CustomAdapter.PollutionRenderer;
import com.project.markpollution.CustomAdapter.PopupInfoWindowAdapter;
import com.project.markpollution.Interfaces.OnItemClickListener;
import com.project.markpollution.Objects.Category;
import com.project.markpollution.Objects.PollutionPoint;
import com.project.markpollution.Objects.Report;
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
        View.OnClickListener,
//        GoogleMap.OnInfoWindowClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,

        ClusterManager.OnClusterClickListener<PollutionPoint>,
        ClusterManager.OnClusterItemClickListener<PollutionPoint>,
        ClusterManager.OnClusterItemInfoWindowClickListener<PollutionPoint> {


    private final static int REQUEST_CHECK_SETTINGS = 9;
    private final static int PollutionRadius = 5000;
    public static ArrayList<PollutionPoint> listPo;
    public static boolean triggerRefreshData = false;
    private NavigationView navigationView;
    private FloatingActionButton fab;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Spinner spnCate;
    private ImageView imgGetLocation;
    private TextView tvRefresh;
    private SlidingDrawer simpleSlidingDrawer;
    private List<Category> listCate;
    private List<PollutionPoint> listPoByCateID, listSeriousPo, listRecentPo, listNearbyPo;
    private HashMap<String, Uri> images = new HashMap<>();
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String url_retrive_pollutionPoint = "http://indi.com.vn/dev/markpollution/RetrievePollutionPoint.php";
    private String url_retrieve_cate = "http://indi.com.vn/dev/markpollution/RetrieveCategory.php";
    //    private String url_RetrievePollutionByCateID = "http://indi.com.vn/dev/markpollution/RetrievePollutionBy_CateID.php?id_cate=";
    private String url_RetrievePollutionOrderByRate = "http://indi.com.vn/dev/markpollution/RetrievePollutionOrderByRate.php";
    private String url_RetrievePollutionOrderByTime = "http://indi.com.vn/dev/markpollution/RetrievePollutionOrderByTime.php";
    //    private BottomSheetBehavior bottomSheetBehavior;
    private FeedRecyclerViewAdapter feedAdapter;
    private RecyclerView recyclerViewFeed;
    //    private List<Marker> listMarkers;
    private List<LatLng> listLatLngsCamUpdate;
    private boolean isFirstTimeLaunch = true;
    private ArrayAdapter<Category> cateAdapter;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private Location curLocation;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private ClusterManager<PollutionPoint> mClusterManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fetchData();
        initView();
        configCheckLocation();
        setNavigationHeader();
        loadSpinnerCate();
        listenNewPollution();
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        imgGetLocation = (ImageView) findViewById(R.id.imgGetLocation);
        spnCate = (Spinner) findViewById(R.id.spnCateMap);
        tvRefresh = (TextView) findViewById(R.id.textViewRefresh);

        // initiate the SlidingDrawer
        simpleSlidingDrawer = (SlidingDrawer) findViewById(R.id.simpleSlidingDrawer);
        final ImageButton handleButton = (ImageButton) findViewById(R.id.handle);
        simpleSlidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                handleButton.setImageResource(R.drawable.ic_expand_00000);
            }
        });
        simpleSlidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                handleButton.setImageResource(R.drawable.ic_expand_00010);
            }
        });
        recyclerViewFeed = (RecyclerView) findViewById(R.id.recyclerViewFeed);
        LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewFeed.setLayoutManager(layout);

        // initialize firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        navigationView.setNavigationItemSelectedListener(this);
        mapFragment.getMapAsync(this);
        fab.setOnClickListener(this);
    }

    // Fetch data from SigninActivity sent
    private void fetchData() {
        Intent intent1 = getIntent();
        String po_data = intent1.getStringExtra("po_data");
        if (!po_data.isEmpty()) {
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
        }
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
        Picasso.with(this).load(Uri.parse(avatar)).resize(170, 170).transform(new CircleTransform()).into(ivNavAvatar);
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
                cateAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, listCate);
                cateAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
                spnCate.setAdapter(cateAdapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        Volley.newRequestQueue(MainActivity.this).add(stringReq);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Application hasn't granted permission to access your location", Toast.LENGTH_SHORT).show();
            return;
        }
        filterPollutionByCate(googleMap);

        mClusterManager = new ClusterManager<>(this, mMap);
        mClusterManager.setRenderer(new PollutionRenderer(getApplicationContext(), mMap, mClusterManager, images));
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new PopupInfoWindowAdapter(this, LayoutInflater.from(this), images));
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        mClusterManager.addItems(listPo);
        mClusterManager.cluster();

        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    private void addMarker(GoogleMap map, PollutionPoint po) {
        listLatLngsCamUpdate.add(new LatLng(po.getLat(), po.getLng()));
    }

    private void showHeatMaps() {
        mMap.clear();
        mClusterManager.clearItems();
        listLatLngsCamUpdate = new ArrayList<>();
        for (PollutionPoint po : listPo) {
            listLatLngsCamUpdate.add(new LatLng(po.getLat(), po.getLng()));
        }
        mProvider = new HeatmapTileProvider.Builder().data(listLatLngsCamUpdate).radius(15).build();
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));

        cameraViewAllCluster(listLatLngsCamUpdate);
    }

    private void getPollutionByCateID(String CateId) {
        listPoByCateID = new ArrayList<>(); // reinitialize listPoByCateID
        for (PollutionPoint po : listPo) {
            if (po.getId_cate().equals(CateId)) {
                listPoByCateID.add(po);
            }
        }
        // extract listPoByCateID into map. Don't forget reinitialize list markers
        listLatLngsCamUpdate = new ArrayList<>();    // reinitialize list marks
        mMap.clear();   // clear old map
        mClusterManager.clearItems();
        for (PollutionPoint po : listPoByCateID) {
            addMarker(mMap, po);
        }
        mClusterManager.addItems(listPoByCateID);
        // update camera
        cameraViewAllCluster(listLatLngsCamUpdate);

        // load infoPanel (SlidingDrawer)
        loadSlidingDrawerFeed(listPoByCateID);
    }

    private void filterPollutionByCate(final GoogleMap googleMap) {
        spnCate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Category cate = (Category) spnCate.getItemAtPosition(i);
                String CateID = cate.getId();

                if (!CateID.equals("0")) {
                    getPollutionByCateID(CateID);
                } else {
                    googleMap.clear();
                    mClusterManager.clearItems();
                    listLatLngsCamUpdate = new ArrayList<>();    // each time filter category reinitialize List markers
                    for (PollutionPoint po : listPo) {
                        addMarker(googleMap, po);
                    }
                    mClusterManager.addItems(listPo);
                    // update camera
                    cameraViewAllCluster(listLatLngsCamUpdate);
                    // load slidingDrawer
                    loadSlidingDrawerFeed(listPo);
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
                                listLatLngsCamUpdate = new ArrayList<>();    // reinitialize list marks
                                mMap.clear();
                                mClusterManager.clearItems();
                                for (PollutionPoint po : listSeriousPo) {
                                    addMarker(mMap, po);
                                }
                                mClusterManager.addItems(listSeriousPo);
                                // update camera
                                cameraViewAllCluster(listLatLngsCamUpdate);
                                // load infoPanel
                                loadSlidingDrawerFeed(listSeriousPo);
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

    private void getRecentPollution() {
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, url_RetrievePollutionOrderByTime, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("success")) {
                                listRecentPo = new ArrayList<>();
                                JSONArray arr = response.getJSONArray("response");
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject po = arr.getJSONObject(i);
                                    listRecentPo.add(new PollutionPoint(po.getString("id_po"), po.getString
                                            ("id_cate"), po.getString("id_user"), po.getDouble("lat"), po.getDouble
                                            ("lng"), po.getString("title"), po.getString("desc"), po.getString
                                            ("image"), po.getString("time")));
                                }
                                listLatLngsCamUpdate = new ArrayList<>();    // reinitialize list marks
                                mMap.clear();
                                mClusterManager.clearItems();
                                for (PollutionPoint po : listRecentPo) {
                                    addMarker(mMap, po);
                                }
                                mClusterManager.addItems(listRecentPo);
                                // update camera
                                cameraViewAllCluster(listLatLngsCamUpdate);
                                // load infoPanel
                                loadSlidingDrawerFeed(listRecentPo);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });

        Volley.newRequestQueue(MainActivity.this).add(objReq);
    }

    private void getNearByPollution() {
        // getNearbyPollution
        listNearbyPo = new ArrayList<>();
        for (PollutionPoint po : listPo) {
            LatLng latLng = new LatLng(po.getLat(), po.getLng());
            double distance = SphericalUtil
                    .computeDistanceBetween(new LatLng(curLocation.getLatitude(), curLocation.getLongitude()), latLng);
            if (distance <= PollutionRadius) {
                listNearbyPo.add(po);
            }
        }
        // add NearbyPo markers into map
        listLatLngsCamUpdate = new ArrayList<>();
        mMap.clear();
        mClusterManager.clearItems();
        for (PollutionPoint po : listNearbyPo) {
            addMarker(mMap, po);
        }
        mClusterManager.addItems(listNearbyPo);

        // Draw circle around my location
        mMap.addCircle(new CircleOptions()
                .center(new LatLng(curLocation.getLatitude(), curLocation.getLongitude()))
                .radius(PollutionRadius)
                .strokeWidth(2)
                .strokeColor(Color.GREEN)
                .fillColor(Color.parseColor("#75498AF3")));

        // update camera
        LatLng southwest = SphericalUtil.computeOffset(new LatLng(curLocation.getLatitude(), curLocation.getLongitude
                ()), PollutionRadius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(new LatLng(curLocation.getLatitude(), curLocation.getLongitude
                ()), PollutionRadius * Math.sqrt(2.0), 45);
        LatLngBounds latLngBounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 30));

        loadSlidingDrawerFeed(listNearbyPo);
    }

    private void loadSlidingDrawerFeed(List<PollutionPoint> list) {
        simpleSlidingDrawer.open();
        feedAdapter = new FeedRecyclerViewAdapter(this, list);
        recyclerViewFeed.setAdapter(feedAdapter);
        feedAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(PollutionPoint po) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(po.getLat(), po.getLng()), 17));
                simpleSlidingDrawer.close();
            }
        });
    }

    private void listenNewPollution() {
        DatabaseReference refReport = databaseReference.child("NewReports");
        refReport.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Report obj = dataSnapshot.getValue(Report.class);
                if (!obj.getId_user().equals(getUserID())) {
                    if (!isFirstTimeLaunch) {
                        tvRefresh.setVisibility(View.VISIBLE);
                        refreshData();
                    }
                }
                isFirstTimeLaunch = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void refreshData() {
        tvRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringRequest strReq = new StringRequest(Request.Method.GET, url_retrive_pollutionPoint, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (!response.equals("Error when retrieve all pollution points")) {
                            try {
                                JSONObject json = new JSONObject(response);
                                JSONArray arr = json.getJSONArray("result");
                                listPo = new ArrayList<>();     // reinitialize list<Po>
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
                                spnCate.setAdapter(cateAdapter);    // notifyDataSetChanged for spinnerCate
                                tvRefresh.setVisibility(View.INVISIBLE);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Volley", error.getMessage());
                    }
                });
                Volley.newRequestQueue(MainActivity.this).add(strReq);
            }
        });
    }

    private void fetchDataFromServer() {
        if (triggerRefreshData) {
            StringRequest strReq = new StringRequest(Request.Method.GET, url_retrive_pollutionPoint, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (!response.equals("Error when retrieve all pollution points")) {
                        try {
                            JSONObject json = new JSONObject(response);
                            JSONArray arr = json.getJSONArray("result");
                            listPo = new ArrayList<>();     // reinitialize list<Po>
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
                            // restore triggerRefreshData
                            triggerRefreshData = false;
                            // notifyDataSetChanged for spinnerCate
                            spnCate.setAdapter(cateAdapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Volley", error.getMessage());
                }
            });
            Volley.newRequestQueue(MainActivity.this).add(strReq);
        }
    }

    private String getUserID() {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedpref_id_user", MODE_PRIVATE);
        return sharedPreferences.getString("sharedpref_id_user", "");
    }

    private void cameraViewAllCluster(List<LatLng> LatLngList) {
        /**create for loop for get the latLngbuilder from the marker list*/
        LatLngBounds.Builder Lbuilder = new LatLngBounds.Builder();
        for (LatLng latLng : LatLngList) {
            Lbuilder.include(latLng);
        }
        final LatLngBounds bounds = Lbuilder.build();
        /**create the camera with bounds and padding to set into map*/
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                /**set animated zoom camera into map*/
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            }
        });
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
            getRecentPollution();
        } else if (id == R.id.nav_reportMgr) {
            startActivity(new Intent(this, ReportManagementActivity.class));
        } else if (id == R.id.nav_nearbyPo) {
            getNearByPollution();
        } else if (id == R.id.nav_showHeatMaps) {
            showHeatMaps();
        } else if (id == R.id.nav_seriousPo) {
            getSeriousPollution();
        } else if (id == R.id.nav_share) {
        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == fab) {
            Snackbar.make(view, "Mark the pollution point and click OK ", Snackbar.LENGTH_INDEFINITE).setAction("OK",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(MainActivity.this, SendReportActivity.class);
                            i.putExtra("Lat", mMap.getCameraPosition().target.latitude);
                            i.putExtra("Long", mMap.getCameraPosition().target.longitude);
                            startActivity(i);
                        }
                    }).show();
        }
    }

    private void configCheckLocation() {
        // Config googleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();

        // Config connectionRequest
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
    }

    //    Runs when a GoogleApiClient object successfully connects.
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Config locationSettingRequest | Need to reference locationRequest
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        // Retrieve result of locationSettingRequest
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // NO need to show the dialog;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //  GPS turned off, Show the user a dialog
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are unavailable so not possible to show any dialog now
                        break;
                }
            }
        });

        // get Current Location
        if (curLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            curLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        curLocation = location;
    }

    // Handle ResolutionRequired
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Location enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location is not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    // Whenever comeback this Activity. RefreshData
    @Override
    protected void onResume() {
        super.onResume();
        fetchDataFromServer();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    // Disconnect googleApiClient when activity stopped
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onClusterClick(Cluster<PollutionPoint> cluster) {
        String firstName = cluster.getItems().iterator().next().getTitle();
        Toast.makeText(this, cluster.getSize() + " (including " + firstName + ")", Toast.LENGTH_SHORT).show();

        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        final LatLngBounds bounds = builder.build();
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onClusterItemClick(PollutionPoint pollutionPoint) {
        // Does nothing, but you could go into the user's profile page, for example.
        Snackbar.make(findViewById(R.id.drawer_layout), "2 onClusterItemClick(Items item)!!", Snackbar.LENGTH_LONG).show();
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(PollutionPoint pollutionPoint) {
        Snackbar.make(findViewById(R.id.drawer_layout), "3 onClusterItemInfoWindowClick(Items item)!!" + pollutionPoint.getTitle(), Snackbar.LENGTH_LONG).show();
        Toast.makeText(this, pollutionPoint.getId().toString(), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, DetailReportActivity.class);
        intent.putExtra("id_po", pollutionPoint.getId().toString());
        startActivity(intent);
    }
}
