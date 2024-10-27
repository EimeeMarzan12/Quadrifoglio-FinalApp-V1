package com.surendramaran.yolov8tflite;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

public class EditInventoryFragment extends Fragment {

    private static final String TAG = "EditInventoryFragment"; // Tag for logging
    private LinearLayout itemContainer; // Container to hold item views
    private Button completePreviewButton;

    private List<Item> itemList;
    private InventoryViewModel viewModel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment created");
        View view = inflater.inflate(R.layout.fragment_edit_inventory, container, false);
        itemContainer = view.findViewById(R.id.item_container);
        completePreviewButton = view.findViewById(R.id.completePreviewButton);
        itemList = new ArrayList<>();
        viewModel = new ViewModelProvider(requireActivity()).get(InventoryViewModel.class);
        Log.d(TAG, "ViewModel initialized");

        // Check for arguments and handle nulls
        if (getArguments() != null) {
            String quantity = getArguments().getString("quantity");
            String expiry = getArguments().getString("expiry");

            if (quantity != null && !quantity.isEmpty() && expiry != null && !expiry.isEmpty()) {
                Log.d(TAG, "Detected item: Quantity = " + quantity + ", Expiry = " + expiry);
                Item detectedItem = new Item("Detected Item", quantity, expiry);
                itemList.add(detectedItem);
                addItemView(detectedItem);
                viewModel.addItem(detectedItem);
            } else {
                Log.e(TAG, "Quantity or expiry is null or empty in arguments");
            }
        } else {
            Log.e(TAG, "Arguments are null");
        }

        // Observe the items in the ViewModel
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "Observing items from ViewModel. Items received: " + (items != null ? items.size() : "null"));
            itemList.clear();
            itemContainer.removeAllViews();

            if (items != null && !items.isEmpty()) {
                for (Item item : items) {
                    if (item != null) {
                        addItemView(item);
                    } else {
                        Log.e(TAG, "Received a null item from ViewModel");
                    }
                }
            } else {
                Log.e(TAG, "Received null or empty items from ViewModel");
            }
        });

        // Set up button click listener
        completePreviewButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            for (Item item : itemList) {
                viewModel.addItem(item);
            }
            navigateToDashboardFragment();
        });

        return view;
    }

    private void navigateToDashboardFragment() {
        if (isAdded()) { // Check if fragment is currently added to the activity
            DashboardFragment dashboardFragment = new DashboardFragment();
            Bundle bundle = new Bundle();
            // Check if itemList is not empty before passing it
            if (!itemList.isEmpty()) {
                bundle.putParcelableArrayList("itemList", (ArrayList<? extends Parcelable>) itemList);
            } else {
                Log.e(TAG, "itemList is empty, not passing to DashboardFragment");
            }
            dashboardFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, dashboardFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Log.e(TAG, "Fragment is not added, cannot navigate to DashboardFragment");
        }
    }

    void addItemView(Item item) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_layout, itemContainer, false);

        // Reference to individual item views
        TextView itemName = itemView.findViewById(R.id.item_name);
        TextView itemQty = itemView.findViewById(R.id.item_qty);
        TextView itemExpiry = itemView.findViewById(R.id.item_expiry);

        // Use ternary operator to avoid null values
        itemName.setText(item.getName() != null ? item.getName() : "Unknown Item");
        itemQty.setText(item.getQuantity() != null ? item.getQuantity() : "0");
        itemExpiry.setText(item.getExpiry() != null ? item.getExpiry() : "No Expiry Date");

        // Add the item view to the container
        itemContainer.addView(itemView);
        Log.d(TAG, "Item added: " + item.getName());
    }


    // Override onDestroyView to clean up any resources
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // Data class for an Item
    public static class Item implements Parcelable {
        private String name;
        private String quantity;
        private String expiry;

        // Constructor
        public Item(String name, String quantity, String expiry) {
            this.name = name;
            this.quantity = quantity;
            this.expiry = expiry;
        }

        // Constructor used for Parcelable
        protected Item(Parcel in) {
            name = in.readString();
            quantity = in.readString();
            expiry = in.readString();
        }

        public static final Parcelable.Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(quantity);
            dest.writeString(expiry);
        }

        public String getName() {
            return name;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getExpiry() {
            return expiry;
        }
    }
}
