package com.example.newgroceriio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newgroceriio.Adapters.CategoryAdapter;
import com.example.newgroceriio.Models.Category;
import com.example.newgroceriio.Models.Product;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryListener{
    NavigationBarView mHomeNavBar;

    TextView userFullName;
    FirebaseAuth fAuth;
    FirebaseDatabase database;
    DatabaseReference mDatabase, productsRef;

    private SharedPreferences mPreferences;

    private RecyclerView recyclerView;
    HashSet<String> categoryTypes;
    ArrayList<Category> categories;
    CategoryAdapter adapter;
    SharedPreferences sharedPreferences;

    DatabaseReference storeRef;
    private List<String> locationsList;
    private LocationRequest locationRequest;
    public static final int REQUEST_CHECK_SETTING = 1001;
    private static boolean mLocationBool = false;

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHomeNavBar = findViewById(R.id.homeNavBar);
        mHomeNavBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.homePage:
                        break;
                    case R.id.mapPage:
                        locationsList = new ArrayList<>();
                        GetLocations();

                        locationRequest = LocationRequest.create();
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locationRequest.setFastestInterval(2000);

                        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest);
                        builder.setAlwaysShow(true);

                        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                                .checkLocationSettings(builder.build());

                        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                                try {
                                    LocationSettingsResponse response = task.getResult(ApiException.class);

                                    startActivityPage();
                                    Toast.makeText(MainActivity.this, "Gps is on", Toast.LENGTH_SHORT).show();
                                } catch (ApiException e) {
                                    switch(e.getStatusCode()){
                                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                                            try {
                                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                                resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTING);
                                            } catch (IntentSender.SendIntentException sendIntentException) {
                                                Log.d(TAG, "Send intent exception");
                                            }
                                            break;

                                        //when user device does not have location functionality
                                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                            break;
                                    }
                                }

                            }
                        });
                        break;
                    case R.id.cartPage:
//                        startActivity(new Intent(getApplicationContext(), ShoppingListActivity.class));
                        break;
                    case R.id.logOut:
                        Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        break;
                }
                return false;
            }
        });

        Intent intent = getIntent();
        String emailFrmLogin = intent.getStringExtra("email");




        fAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference("users_data");

        userFullName = findViewById(R.id.homeUserName);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String name = sharedPreferences.getString("name", null);
        if(name != null){
            userFullName.setText("Welcome, " + name);
        }

        // Retrieve User name
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s: snapshot.getChildren()){
                    String email = s.child("email").getValue(String.class);
                    if(name == null){
                        if(emailFrmLogin.equals(email)){
                            String name = s.child("name").getValue(String.class);
                            Toast.makeText(
                                    MainActivity.this,
                                    "Found Username",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            storeNameToSharePreference(name);
                            userFullName.setText("Welcome, " + name);
                        }
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        productsRef = database.getReference("product_data");
        categories = new ArrayList<>();
        categoryTypes = new HashSet<>();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s: snapshot.getChildren()){
                    Product product = s.getValue(Product.class);
                    categoryTypes.add(product.getProductType());

                }
                loadToCardView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }

    private void storeNameToSharePreference(String name){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.apply();
    }

    private void loadToCardView(){
        for(String type : categoryTypes){
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
        startActivity(intent);

    }

    ////////////// For Map Below vv //////////////


    private void startActivityPage(){
        Intent intent = new Intent(MainActivity.this, NearestStoreActivity.class);
        intent.putStringArrayListExtra("locations", (ArrayList<String>) locationsList);

        System.out.println(mLocationBool);

        new Timer().schedule(new TimerTask(){
            public void run() {
                startActivity(intent);
            }
        }, 3000);


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTING){
            switch (resultCode){
                case Activity.RESULT_OK:
                    Toast.makeText(this, "GPS is turned on. Press the button one more time to continue", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, "GPS is required to be turned on", Toast.LENGTH_SHORT).show();

            }
        }

    }

    private void GetLocations(){

        // Database Ref
        storeRef = FirebaseDatabase.getInstance().getReference().child("store_data");

        storeRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot s: snapshot.getChildren()){
                    String address = s.child("Address").getValue(String.class);
                    System.out.println(s);
                    System.out.println(address);
                    locationsList.add(address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}
