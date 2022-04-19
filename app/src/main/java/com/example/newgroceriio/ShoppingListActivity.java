package com.example.newgroceriio;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newgroceriio.Adapters.ShoppingListItemAdapter;
import com.example.newgroceriio.Models.Product;
import com.example.newgroceriio.Models.ShoppingList;
import com.example.newgroceriio.Models.ShoppingListItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShoppingListActivity extends AppCompatActivity implements ShoppingListItemAdapter.OnShoppingListItemListener{
    private RecyclerView recyclerView;
    private ShoppingListItemAdapter adapter;
    private FirebaseAuth fAuth;
    private FirebaseDatabase database;
    private DatabaseReference mDatabase;
    private DatabaseReference stockRef;
    private Button mConfirmOrder, mBackToHome;
    private TextView mTotalCost;
    private String userID;
    private static ShoppingList shoppingList;
    private static ArrayList<ShoppingListItem>  allItems;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shopping_list_page);
        fAuth = FirebaseAuth.getInstance();
        mConfirmOrder = findViewById(R.id.shopListOrderBtn);
        mBackToHome = findViewById(R.id.shopListBackBtn);
        mTotalCost = findViewById(R.id.shopListTotalCost);

        mBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mConfirmOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShoppingListActivity.this, CollectionLocationActivity.class));
                finish();
            }
        });


        // Link view widgets to objects
        recyclerView = findViewById(R.id.shopListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        userID = sharedPreferences.getString("uid","");
        System.out.println("PRINT USERID");
        System.out.println(userID);



        // Database Ref
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference("shopping_list_data");
        stockRef = FirebaseDatabase.getInstance().getReference("stock_data");

        Intent intent = getIntent();
        String productId = intent.getStringExtra("product_id");
        String storeId = intent.getStringExtra("store_id");
        String productName = intent.getStringExtra("product_name");
        String productUrl = intent.getStringExtra("product_url");
        String productPrice = intent.getStringExtra("product_price");
        String prevActivity = intent.getStringExtra("prev_activity");

        if(shoppingList == null){
            shoppingList = new ShoppingList();
            allItems = new ArrayList<>();
        }

        if(userID != null) {
            if (prevActivity != null){
                if (prevActivity.equals("order_confirmed")){
                    allItems = new ArrayList<>();
                    emptyShoppingList(userID);
                    updateShoppingList();
                }
            }
            else{
                retrieveShoppingList(userID, productId,storeId, productName, productUrl, productPrice);
            }

        }

    }

    private void emptyShoppingList(String userID){
        System.out.println("EMPTYING LIST NOW");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s: snapshot.getChildren()){
                    if (s.getKey().equals(userID)){
                        s.getRef().removeValue();
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void retrieveShoppingList(String userID, String productId, String storeId, String productName, String productUrl, String productPrice) {

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s: snapshot.getChildren()){

                    if(s.getKey().equals(userID)){
                        shoppingList = s.getValue(ShoppingList.class);
                    }
                    //System.out.println(s);
                }

                allItems = shoppingList.getShopListItems();
                if(productId != null && storeId != null){
                    addItemToList(productId, storeId, productName,productUrl, productPrice);
                }
                if(allItems.size() >0){
                    loadToCardView();

                }

                updateShoppingList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void addItemToList(String productId, String storeId, String productName, String productUrl, String productPrice){
        ShoppingListItem shoppingItem = new ShoppingListItem();
        Product p = new Product();
        p.setProductId(productId);
        p.setProductName(productName);
        p.setImgUrl(productUrl);
        p.setPrice(Double.parseDouble(productPrice));
        shoppingItem.setProduct(p);
        shoppingItem.setStoreId(storeId);
        shoppingItem.add1Quantity();


        if(allItems.size() == 0){
            for(ShoppingListItem i: shoppingList.getShopListItems()){
                allItems.add(i);
            }
        }

        boolean check = false;
        ArrayList<ShoppingListItem> allItemsUpdated = new ArrayList<ShoppingListItem>();
        for(ShoppingListItem i: allItems){
            if (i.getProduct().getProductName().equals(productName)) {
                i.add1Quantity();
                check = true;
            }
            allItemsUpdated.add(i);
        }
        allItems = allItemsUpdated;

        if(!check){
            allItems.add(shoppingItem);
        }
//        reduceStock(storeId, productId);
        updateShoppingList();
    }


//    private void reduceStock(String storeId, String productId){
//
//
//        stockRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for(DataSnapshot s : snapshot.getChildren()){
//                    for(DataSnapshot stock : s.getChildren()){
//                        System.out.println(stock);
//                        StockValue sv = stock.getValue(StockValue.class);
//                        System.out.println(sv.getStoreStockId());
//                        System.out.println(storeId);
//                        if(sv.getStoreStockId().equals(storeId) && sv.getProductStockId().equals(productId)){
//                            Map<String, Object> updated = new HashMap<String,Object>();
//                            sv.reduceStockByOne();
//                            updated.put(sv.getProductStockId(), sv);
//                            stockRef.child(sv.getStoreStockId()).updateChildren(updated);
//
//                        }
//                    }
//
//
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//
//    }

//    private void calculateTotal(ArrayList<ShoppingListItem> list){
//        double total = 0;
//        for(ShoppingListItem i: list){
//
//            total = total + i.getProduct().getPrice();
//        }
//        mTotalCost.setText(String.valueOf(total));
//    }

    private void updateShoppingList(){
        Map<String, Object> updated = new HashMap<String,Object>();
        if(adapter == null) {
            adapter = new ShoppingListItemAdapter(this, allItems, this);
        }
        shoppingList.setShopListItems(adapter.getShoppingList());
        updated.put(userID, shoppingList);
        mDatabase.updateChildren(updated);
    }

    private void loadToCardView(){
        adapter = new ShoppingListItemAdapter(this, allItems, this);
        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
        updateShoppingList();
    }

    @Override
    protected void onPause() {
        // call the superclass method first
        super.onPause();
        updateShoppingList();
    }

    @Override
    public void onShoppingListItemClick(int position) {

    }
}
