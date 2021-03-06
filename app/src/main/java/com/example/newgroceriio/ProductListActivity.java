package com.example.newgroceriio;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newgroceriio.Adapters.ProductAdapter;
import com.example.newgroceriio.Models.Product;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductListener{
    Button mProductBackBtn;
    TextView mProductTitleText;

    DatabaseReference ref;
    DatabaseReference productsRef;
    ArrayList<Product> productList;
    private ProductAdapter adapter;
    private RecyclerView recyclerView;
    private TextInputEditText mInputSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_list_activity);
        mInputSearch = findViewById(R.id.searchBarInput);
        mProductBackBtn = findViewById(R.id.productListBackBtn);
        mProductTitleText = findViewById(R.id.productListTitleText);
        recyclerView = findViewById(R.id.productRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get intent from MainActivity
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        mProductTitleText.setText(type);
        productList = new ArrayList<>();

        // Database Ref
        ref = FirebaseDatabase.getInstance().getReference();
        productsRef = ref.child("product_data");

        // Retrieve information of products with ProductType as type
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s: snapshot.getChildren()){
                    String type_DB = s.child("ProductType").getValue(String.class);
                    if (type.equals(type_DB)) {
                        Product product = s.getValue(Product.class);
                        productList.add(product);
                    }
                    System.out.println(s);

                }
                loadToCardView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(ProductListActivity.class.toString(), "Retrieved Data Cancelled");
            }
        });


        mProductBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        // Search Products
        mInputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(ProductListActivity.class.toString(), "beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(String.valueOf(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.i(ProductListActivity.class.toString(), "afterTextChanged");
            }
        });
    }

    private void loadToCardView(){
        // Generating view for each product in productList
        adapter = new ProductAdapter(this, productList, this);
        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onProductClick(int position) {

        Intent intent = new Intent(ProductListActivity.this, ProductPageActivity.class);
        Product p = productList.get(position);

        // Pass product information to intent
        intent.putExtra("product_name", p.getProductName());
        intent.putExtra("product_brand", p.getProductBrand());
        intent.putExtra("product_metric", p.getMetric());
        intent.putExtra("product_price", String.valueOf(p.getPrice()));
        intent.putExtra("product_url", p.getImgUrl());
        intent.putExtra("product_id", p.getProductId());
        startActivity(intent);


    }




}