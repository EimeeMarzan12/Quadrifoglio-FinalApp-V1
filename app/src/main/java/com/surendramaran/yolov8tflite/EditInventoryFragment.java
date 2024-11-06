package com.surendramaran.yolov8tflite;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class EditInventoryFragment extends Fragment {

    private static final String TAG = "EditInventoryFragment"; // Tag for logging
    private LinearLayout itemContainer; // Container to hold item views
    private Button completePreviewButton;

    private List<Item> itemList;
    private InventoryViewModel viewModel;

    // Firebase Database reference
    private DatabaseReference databaseRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment created");
        View view = inflater.inflate(R.layout.fragment_edit_inventory, container, false);

        // Initialize Firebase Database reference
        databaseRef = FirebaseDatabase.getInstance().getReference("meds");

        itemContainer = view.findViewById(R.id.item_container);
        completePreviewButton = view.findViewById(R.id.completePreviewButton);
        itemList = new ArrayList<>();
        viewModel = new ViewModelProvider(requireActivity()).get(InventoryViewModel.class);
        Log.d(TAG, "ViewModel initialized");

        // Check for arguments and handle nulls
        if (getArguments() != null) {
            String itemName = getArguments().getString("itemName");
            String quantity = getArguments().getString("quantity");
            String expiry = getArguments().getString("expiry");

            if (itemName != null && !itemName.isEmpty() && quantity != null && !quantity.isEmpty() && expiry != null && !expiry.isEmpty()) {
                Log.d(TAG, "Detected item: Name = " + itemName + ", Quantity = " + quantity + ", Expiry = " + expiry);
                Item detectedItem = new Item(itemName, quantity, expiry);
                itemList.add(detectedItem);
                addItemView(detectedItem);
                viewModel.addItem(detectedItem);

                // Write the detected item to Firebase
                writeItemToFirebase(detectedItem);
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
                writeItemToFirebase(item); // Save each item to Firebase
            }
            navigateToDashboardFragment();
        });

        // Call the updateUIForFragment method from MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.updateUIForFragment("Inventory", R.drawable.ic_inventory_bluef, "#5075E8",
                    R.drawable.ic_home_grayo, "#4D4D4D",
                    R.drawable.ic_profile_grayo, "#4D4D4D");
        }

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

        ImageView itemPic = itemView.findViewById(R.id.item_pic);
        TextView itemName = itemView.findViewById(R.id.item_name);
        TextView itemQty = itemView.findViewById(R.id.item_qty);
        TextView itemExpiry = itemView.findViewById(R.id.item_expiry);

        itemName.setText(item.getName() != null ? item.getName() : "Unknown Item");
        itemQty.setText(item.getQuantity() != null ? item.getQuantity() : "0");
        itemExpiry.setText(item.getExpiry() != null ? item.getExpiry() : "No Expiry Date");

        // Set image based on item name
        switch (item.getName()) {
            case "Biogesic":
                itemPic.setImageResource(R.drawable.biogesic);
                break;
            case "Dolan":
                itemPic.setImageResource(R.drawable.dolanfp);
                break;
            case "Decolgen":
                itemPic.setImageResource(R.drawable.decolgen_nodrowse);
                break;
            case "Enervon":
                itemPic.setImageResource(R.drawable.enervon);
                break;
            default:
                itemPic.setImageResource(R.drawable.img_1); // Fallback image
                break;
        }

        // Set a long click listener for deletion
        itemView.setOnLongClickListener(v -> {
            showDeleteConfirmationDialog(item, itemView);
            return true; // Return true to indicate the event is consumed
        });

        itemContainer.addView(itemView);
        Log.d(TAG, "Item added: " + item.getName());
    }

    private void showDeleteConfirmationDialog(Item item, View itemView) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete this item?")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    itemList.remove(item);
                    viewModel.removeItem(item);
                    itemContainer.removeView(itemView);
                    Log.d(TAG, "Item deleted: " + item.getName());
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Function to write an item to Firebase
    private void writeItemToFirebase(Item item) {
        String key = databaseRef.push().getKey();
        if (key != null) {
            databaseRef.child(key).setValue(item)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Item saved to Firebase", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to save item to Firebase", Toast.LENGTH_SHORT).show());
        } else {
            Log.e(TAG, "Failed to get Firebase key");
        }
    }

    // Data class for an Item
    public static class Item implements Parcelable {
        private String name;
        private String quantity;
        private String expiry;

        public Item(String name, String quantity, String expiry) {
            this.name = name;
            this.quantity = quantity;
            this.expiry = expiry;
        }

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
