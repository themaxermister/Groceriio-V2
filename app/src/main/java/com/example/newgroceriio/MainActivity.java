package com.example.newgroceriio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newgroceriio.Adapters.CategoryAdapter;
import com.example.newgroceriio.Models.Category;
import com.example.newgroceriio.Models.Product;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryListener {

    private NavigationBarView mHomeNavBar;
    private TextView userFullName;
    private TextView currentLocationText;
    private CategoryAdapter adapter;

    private RecyclerView recyclerView;
    private DatabaseReference productsRef;
    private DatabaseReference storeRef;

    private ArrayList<String> storeProductId;
    private ArrayList<Category> categories;
    private List<String> locationsList;
    private List<String> storesId;
    private List<LatLng> latLngList;
    private List<LatLng> latLngListSorted;
    private List<Address> addresses;

    private HashMap<String, Integer> mStoreProductId;
    private HashSet<String> categoryTypes;

    private static boolean mLocationBool = false;
    private String currentAddress = null;
    private String nearestStore;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private FusedLocationProviderClient locClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate MainActivity");
        setContentView(R.layout.activity_main);

        currentLocationText = findViewById(R.id.homeUserName);

        mStoreProductId = new HashMap<String,Integer>();
        locationsList = new ArrayList<>();
        storesId = new ArrayList<>();
        storeProductId = new ArrayList<>();

        GetMarketLocations();
        getLocationPermission();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String uid = intent.getStringExtra("uid");

        mHomeNavBar = findViewById(R.id.homeNavBar);
        mHomeNavBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.homePage:

                        break;
                    case R.id.mapPage:
                        startActivityPage();
                        break;
                    case R.id.cartPage:
                        Intent intent = new Intent(getApplicationContext(), ShoppingListActivity.class);
                        intent.putExtra("uid", uid);
                        intent.putExtra("currentAddress", currentAddress);
                        startActivity(intent);
                        break;
                    case R.id.logOut:
                        Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
                        editor.clear();
                        editor.apply();
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finishAffinity();
                        break;
                }
                return false;
            }
        });


        editor.putString("name", name);
        editor.putString("uid", uid);
        editor.apply();

        // Store to preference instead of depending on intent from login activity
        String stored_name = sharedPreferences.getString("name", "");

        userFullName = findViewById(R.id.homeUserName);
        userFullName.setText("Welcome, " + stored_name);
    }

    private void getProductId( String nearestStore){

        DatabaseReference productInfo = FirebaseDatabase.getInstance().getReference("stock_data").child(nearestStore);

        productInfo.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot s : snapshot.getChildren()) {
                    String productId = s.child("ProductId").getValue(String.class);
                    storeProductId.add(productId);
                    mStoreProductId.put(productId, 0);
                }
                filterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(TAG,"Retrieved Data Cancelled");
            }
        });


    }


    private void filterData(){
        productsRef = FirebaseDatabase.getInstance().getReference("product_data");
        categories = new ArrayList<>();
        categoryTypes = new HashSet<>();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    Product product = s.getValue(Product.class);
                    categoryTypes.add(product.getProductType());
                }
                loadToCardView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(TAG,"Retrieved Data Cancelled");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    getDeviceLocation();
                }
            }
        }
    }

    private void loadToCardView() {
        for (String type : categoryTypes) {
            Category c = new Category();
            c.setCategoryType(type);
            categories.add(c);
        }
        adapter = new CategoryAdapter(this, categories, this);
        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
    }


    @Override
    public void onCategoryClick(int position) {
        Category c = categories.get(position);

        Intent intent = new Intent(MainActivity.this, ProductListActivity.class);
        intent.putExtra("type", c.getCategoryType());
        intent.putExtra("currentAddress", currentAddress);
        startActivity(intent);

    }

    ////////////// For Map Below vv //////////////


    private void startActivityPage() {
        Intent intent = new Intent(MainActivity.this, NearestStoreActivity.class);
        intent.putStringArrayListExtra("locations", (ArrayList<String>) locationsList);

        System.out.println(mLocationBool);

        new Timer().schedule(new TimerTask() {
            public void run() {
                startActivity(intent);
            }
        }, 3000);
    }

    private void GetMarketLocations() {

        // Database Ref
        storeRef = FirebaseDatabase.getInstance().getReference().child("store_data");

        storeRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot s : snapshot.getChildren()) {
                    String address = s.child("Address").getValue(String.class);
                    String storeId = s.child("StoreId").getValue(String.class);
                    System.out.println(s);
                    System.out.println(address);

                    locationsList.add(address);
                    storesId.add(storeId);

                    latLngList = new ArrayList<>();
                    convertAddressToLatLng(locationsList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(TAG,"Retrieved Data Cancelled");
            }
        });

    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this,
                permissions,
                LOCATION_PERMISSION_REQUEST_CODE);
    }


    private void getDeviceLocation() {

        locClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        }
        locClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location current_location = (Location) task.getResult();

                // Please use a Real Android Phone, if not there will be an error HERE :')
                if(current_location != null){
                    sortMarkersFromLocation(current_location);
                }
                else{
                    getDeviceLocation();
                }

            }
        });

    }

    private void convertAddressToLatLng(List<String> locations){

        List<Address> addresses = new ArrayList<>();

        // Convert String address to object address
        for(String address: locations) {
            final Geocoder geocoder = new Geocoder(this);

            try {
                List<Address> tempAddresses = geocoder.getFromLocationName(address, 1);
                if (tempAddresses != null && !tempAddresses.isEmpty()) {
                    Address temp = tempAddresses.get(0);
                    // Get all product addresses
                    System.out.println(address);
                    addresses.add(temp);
                    String message = String.format("Latitude: %f, Longitude: %f", temp.getLatitude(), temp.getLongitude());
                } else {
                    // Display appropriate message when Geocoder services are not available
                    Toast.makeText(this, "Unable to geocode zipcode", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Convert object address to object latlng
        for(int i = 0; i < addresses.size(); i++){
            LatLng lat_lng = new LatLng(addresses.get(i).getLatitude(), addresses.get(i).getLongitude());
            latLngList.add(lat_lng);
        }
    }

    private void sortMarkersFromLocation(Location current_location) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(current_location.getLatitude(), current_location.getLongitude(), 1);
            currentAddress = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentLocationText.setText(currentAddress);

        List<Location> markers = new ArrayList<>();

        for(LatLng lat_lng: latLngList){
            Location location = new Location("");
            location.setLatitude(lat_lng.latitude);
            location.setLongitude(lat_lng.longitude);
            markers.add(location);
        }

        // markers Before line 392 should be different from markers AFTER line 400
        Collections.sort(markers, new SortDistance(){
            @Override
            public int compare(Location o1, Location o2) {
                Float dist1 = o1.distanceTo(current_location);
                Float dist2 = o2.distanceTo(current_location);
                return dist1.compareTo(dist2);
            }
        });

        latLngListSorted = new ArrayList<>();
        for(Location m: markers){
            latLngListSorted.add(new LatLng(m.getLatitude(), m.getLongitude()));
        }

        for (int i = 0; i < latLngList.size(); i++) {
            if (latLngListSorted.get(0).latitude == latLngList.get(i).latitude && latLngListSorted.get(0).longitude == latLngList.get(i).longitude){
                nearestStore = storesId.get(i);
                break;
            }

        }

        editor.putString("user_latitude", String.valueOf(current_location.getLatitude()));
        editor.putString("user_longitude", String.valueOf(current_location.getLongitude()));
        editor.putString("currentAddress", currentAddress);
        editor.putString("nearestStoreId", nearestStore);
        editor.commit();

        getProductId(nearestStore);
    }


}