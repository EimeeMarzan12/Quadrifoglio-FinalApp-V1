package com.surendramaran.yolov8tflite;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import androidx.lifecycle.ViewModelProvider;

public class EditInventoryFragment extends Fragment {

    // Define your item views as variables
    private TextView itemName1, itemQty1, itemExpiry1;
    private TextView itemName2, itemQty2, itemExpiry2;
    private TextView itemName3, itemQty3, itemExpiry3;
    private TextView itemName4, itemQty4, itemExpiry4;
    private TextView itemName5, itemQty5, itemExpiry5;
    private Button completePreviewButton;

    private InventoryViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_inventory, container, false);

        // Initialize your item views
        itemName1 = view.findViewById(R.id.item_name1);
        itemQty1 = view.findViewById(R.id.item_qty1);
        itemExpiry1 = view.findViewById(R.id.item_expiry1);

        itemName2 = view.findViewById(R.id.item_name2);
        itemQty2 = view.findViewById(R.id.item_qty2);
        itemExpiry2 = view.findViewById(R.id.item_expiry2);

        itemName3 = view.findViewById(R.id.item_name3);
        itemQty3 = view.findViewById(R.id.item_qty3);
        itemExpiry3 = view.findViewById(R.id.item_expiry3);

        itemName4 = view.findViewById(R.id.item_name4);
        itemQty4 = view.findViewById(R.id.item_qty4);
        itemExpiry4 = view.findViewById(R.id.item_expiry4);

        itemName5 = view.findViewById(R.id.item_name5);
        itemQty5 = view.findViewById(R.id.item_qty5);
        itemExpiry5 = view.findViewById(R.id.item_expiry5);

        completePreviewButton = view.findViewById(R.id.completePreviewButton);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(InventoryViewModel.class);
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            // Update UI with the saved items if available
            if (!items.isEmpty()) {
                itemName1.setText(items.get(0).getName());
                itemQty1.setText(items.get(0).getQuantity());
                itemExpiry1.setText(items.get(0).getExpiry());

                // Repeat for other items (itemName2, itemName3, etc.)
                // Make sure to add null checks to avoid IndexOutOfBoundsException
                if (items.size() > 1) {
                    itemName2.setText(items.get(1).getName());
                    itemQty2.setText(items.get(1).getQuantity());
                    itemExpiry2.setText(items.get(1).getExpiry());
                }
                if (items.size() > 2) {
                    itemName3.setText(items.get(2).getName());
                    itemQty3.setText(items.get(2).getQuantity());
                    itemExpiry3.setText(items.get(2).getExpiry());
                }
                if (items.size() > 3) {
                    itemName4.setText(items.get(3).getName());
                    itemQty4.setText(items.get(3).getQuantity());
                    itemExpiry4.setText(items.get(3).getExpiry());
                }
                if (items.size() > 4) {
                    itemName5.setText(items.get(4).getName());
                    itemQty5.setText(items.get(4).getQuantity());
                    itemExpiry5.setText(items.get(4).getExpiry());
                }
            }
        });

        // Set up button click listener
        completePreviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("EditInventoryFragment", "Save button clicked");
                Log.d("EditInventoryFragment", "Item 1 Name: " + itemName1.getText().toString());
                Log.d("EditInventoryFragment", "Item 2 Name: " + itemName2.getText().toString());
                Log.d("EditInventoryFragment", "Item 3 Name: " + itemName3.getText().toString());
                Log.d("EditInventoryFragment", "Item 4 Name: " + itemName4.getText().toString());
                Log.d("EditInventoryFragment", "Item 5 Name: " + itemName5.getText().toString());

                // Add items to ViewModel
                viewModel.addItem(new Item(itemName1.getText().toString(), itemQty1.getText().toString(), itemExpiry1.getText().toString()));
                viewModel.addItem(new Item(itemName2.getText().toString(), itemQty2.getText().toString(), itemExpiry2.getText().toString()));
                viewModel.addItem(new Item(itemName3.getText().toString(), itemQty3.getText().toString(), itemExpiry3.getText().toString()));
                viewModel.addItem(new Item(itemName4.getText().toString(), itemQty4.getText().toString(), itemExpiry4.getText().toString()));
                viewModel.addItem(new Item(itemName5.getText().toString(), itemQty5.getText().toString(), itemExpiry5.getText().toString()));

                // Pass data to DashboardFragment (if needed)
                DashboardFragment dashboardFragment = new DashboardFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("itemList", (ArrayList<? extends Parcelable>) viewModel.getItems().getValue());
                dashboardFragment.setArguments(bundle);

                // Navigate to DashboardFragment
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment, dashboardFragment)
                        .addToBackStack(null)
                        .commit();

                // Directly call MainActivity's method to update the title
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateUIForFragment("Dashboard", R.drawable.ic_inventory_grayo, "#4D4D4D",
                            R.drawable.ic_home_bluef, "#5075E8", R.drawable.ic_profile_grayo, "#4D4D4D");
                }
            }
        });

        return view;
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

        // Parcelable.Creator to create instances of Item from a Parcel
        public static final Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        // Required method to describe contents
        @Override
        public int describeContents() {
            return 0;
        }

        // Method to write the object's data to the Parcel
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(quantity);
            dest.writeString(expiry);
        }

        // Getters for the fields
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