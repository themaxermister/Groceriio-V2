package com.example.newgroceriio.Adapters;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.newgroceriio.Models.ShoppingListItem;
import com.example.newgroceriio.R;
import com.example.newgroceriio.ShoppingListActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ShoppingListItemHolder> implements Filterable{

    private Context context;
    private ArrayList<ShoppingListItem> shoppingItems;
    ArrayList<ShoppingListItem> allShoppingItems;
    private OnShoppingListItemListener mOnShoppingListItemListener;
    Filter filter;
    private double total = 0;


    public ShoppingListItemAdapter(Context context, ArrayList<ShoppingListItem> _shoppingItems, OnShoppingListItemListener OnShoppingListItemListener){
        this.mOnShoppingListItemListener = OnShoppingListItemListener;
        this.context = context;
        this.shoppingItems = _shoppingItems;
        allShoppingItems = new ArrayList<>(_shoppingItems);
        filter = new Filter() {

            //runs on background thread
            @Override
            protected FilterResults performFiltering(CharSequence charSequence){
                System.out.println("performFiltering: " + charSequence.toString());
                ArrayList<ShoppingListItem> filteredList = new ArrayList<ShoppingListItem>();

                if (charSequence.length() == 0){
                    System.out.println("is empty");
                    filteredList.addAll(allShoppingItems);
                    System.out.println(filteredList);
                } else {
                    for (ShoppingListItem shoppingItem: allShoppingItems ){
                    }
                }
                FilterResults filterResults = new FilterResults();

                filterResults.values = filteredList;
                return filterResults;

            }

            //runs on ui thread
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                System.out.println("publishResults::filterResults");
                System.out.println(filterResults.values);
                shoppingItems.clear();
                shoppingItems.addAll((Collection<? extends ShoppingListItem>) filterResults.values);

                notifyDataSetChanged();
            }



        };



    }
    @NonNull
    @Override
    public ShoppingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // Get shopping item view from layout
        View view = LayoutInflater.from(context).inflate(R.layout.shopping_list_item, parent, false);
        return new ShoppingListItemHolder(view, mOnShoppingListItemListener).linkAdapter(this);
    }


    @Override
    public void onBindViewHolder(@NonNull ShoppingListItemHolder holder, int position){

        ShoppingListItem shoppingItem = shoppingItems.get(position);
        holder.setDetails(shoppingItem);
        holder.setTotal();
    }

    @Override
    public int getItemCount() {
        return shoppingItems.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }


    // Holder Class

    class ShoppingListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        OnShoppingListItemListener onShoppingListItemListener;
        ImageView shopItemImage;
        TextView shopItemQuantity, shopItemPrice, shopItemName, mTotalShopList;
        Button removeButton, addButton, minusButton;
        private ShoppingListItemAdapter adapter;



        ShoppingListItemHolder(View shoppingItemView, OnShoppingListItemListener onShoppingListItemListener){
            super(shoppingItemView);

            shopItemImage = shoppingItemView.findViewById(R.id.listItemImage);
            shopItemQuantity = shoppingItemView.findViewById(R.id.listItemQuant);
            shopItemPrice = shoppingItemView.findViewById(R.id.listItemPrice);
            shopItemName = shoppingItemView.findViewById(R.id.listItemName);
            mTotalShopList = ((Activity)context).findViewById(R.id.shopListTotalCost);

            removeButton = shoppingItemView.findViewById(R.id.listItemRemove);
            addButton = shoppingItemView.findViewById(R.id.listItemPlus);
            minusButton = shoppingItemView.findViewById(R.id.listItemMinus);

            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // minus the price
                    ShoppingListItem item =  allShoppingItems.get(getAdapterPosition());
                    int quantity = Integer.parseInt(shopItemQuantity.getText().toString());
                    total -= item.getProduct().getPrice() * quantity;
                    setTotal();

                    // delete list
                    deleteShoppingListItem(adapter, getAdapterPosition());
                }
            });


            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // add view quantity
                    int quantity = Integer.parseInt(shopItemQuantity.getText().toString());
                    shopItemQuantity.setText(String.valueOf(quantity+1));

                    // add one item price
                    ShoppingListItem item =  allShoppingItems.get(getAdapterPosition());
                    total += item.getProduct().getPrice();
                    setTotal();
                    updateItems(getAdapterPosition(), quantity+1);
                }
            });


            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // minus view quantity
                    int quantity = Integer.parseInt(shopItemQuantity.getText().toString());
                    shopItemQuantity.setText(String.valueOf(quantity-1));

                    //minus one item price
                    ShoppingListItem item =  allShoppingItems.get(getAdapterPosition());
                    total -= item.getProduct().getPrice();

                    setTotal();
                    updateItems(getAdapterPosition(), quantity-1);

                    // if only one item remove the item
                    if(quantity == 1){
                        deleteShoppingListItem(adapter, getAdapterPosition());
                    }
                }
            });

            this.onShoppingListItemListener = onShoppingListItemListener;
            shoppingItemView.setOnClickListener(this);

        }
        void setDetails(ShoppingListItem shoppingItem){
            shopItemName.setText(shoppingItem.getProduct().getProductName());
            shopItemPrice.setText(String.valueOf(shoppingItem.getProduct().getPrice()));
            shopItemQuantity.setText(String.valueOf(shoppingItem.getQuantity()));

            total += shoppingItem.getQuantity() * shoppingItem.getProduct().getPrice();


            Glide.with(context)
                    .load(shoppingItem.getProduct().getImgUrl())
                    .into(shopItemImage);
        }

        void updateItems(int position, int quantity){
            ShoppingListItem item = allShoppingItems.get(position);
            item.setQuantity(quantity);
            allShoppingItems.set(position, item);
        }

        void setTotal(){
            BigDecimal p = new BigDecimal(total);
            p = p.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            mTotalShopList.setText(String.valueOf(p));
        }

        private ShoppingListItemHolder linkAdapter(ShoppingListItemAdapter adapter){
            this.adapter = adapter;
            return this;
        }

        @Override
        public void onClick(View view) {
            onShoppingListItemListener.onShoppingListItemClick(getAdapterPosition());
        }
    }

    private void deleteShoppingListItem(ShoppingListItemAdapter adapter, int position){
        adapter.allShoppingItems.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, allShoppingItems.size());
        context.startActivity(new Intent(context,ShoppingListActivity.class));
        ((Activity)context).finish();
    }

    public interface OnShoppingListItemListener{
        void onShoppingListItemClick(int position);
    }

    public ArrayList<ShoppingListItem> getShoppingList(){
        return allShoppingItems;
    }



}