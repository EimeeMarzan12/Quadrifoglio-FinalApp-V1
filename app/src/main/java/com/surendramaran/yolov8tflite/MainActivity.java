package com.surendramaran.yolov8tflite;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private ImageView log_btn;
    private ImageView dashboard_ic;
    private TextView dashboard_text;
    private TextView fragment_title;
    private ImageView profile_ic;
    private TextView profile_text;
    private ImageView inventory_ic;
    private TextView inventory_text;
    private RelativeLayout dashboard_btn;
    private RelativeLayout inventory_btn;
    private RelativeLayout profile_btn;
    private InventoryViewModel viewModel;

    // Firebase Database reference
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        databaseRef = FirebaseDatabase.getInstance().getReference("meds");

        // Initialize UI elements
        log_btn = findViewById(R.id.logInventory_btn);
        fragment_title = findViewById(R.id.fragment_title);
        dashboard_ic = findViewById(R.id.block_dashboard_ic);
        dashboard_text = findViewById(R.id.block_dashboard_text);
        profile_ic = findViewById(R.id.block_profile_ic);
        profile_text = findViewById(R.id.block_profile_text);
        inventory_ic = findViewById(R.id.block_inventory_ic);
        inventory_text = findViewById(R.id.block_inventory_text);
        dashboard_btn = findViewById(R.id.block_dashboard);
        inventory_btn = findViewById(R.id.block_inventory);
        profile_btn = findViewById(R.id.block_profile);

        // Initialize the ViewModel
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Load the initial fragment (DashboardFragment) if no saved instance state
        if (savedInstanceState == null) {
            useFragment(new DashboardFragment(), R.id.main_fragment);
        }

        // Handle back press to move task to background instead of closing the app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        // Retrieve the username and password passed from LoginActivity
        String username = getIntent().getStringExtra("username_key");
        String password = getIntent().getStringExtra("password_key");

        // Save these values to be accessible when you load UserProfileFragment
        Bundle bundle = new Bundle();
        bundle.putString("username_key", username);
        bundle.putString("password_key", password);

        // Use the bundle to set arguments on UserProfileFragment, but don’t display it yet
        UserProfileFragment userProfileFragment = new UserProfileFragment();
        userProfileFragment.setArguments(bundle);

        // Set up click listeners for bottom navigation
        setupNavigation();


    }

    private void setupNavigation() {
        // Log (CameraFragment) button click
        log_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIForFragment("Scanning", R.drawable.ic_inventory_grayo, "#4D4D4D",
                        R.drawable.ic_home_grayo, "#4D4D4D", R.drawable.ic_profile_grayo, "#4D4D4D");

                // Switch to CameraFragment for scanning
                useFragment(new CameraFragment(), R.id.fragment_container);
            }
        });

        // Dashboard button click
        dashboard_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIForFragment("Dashboard", R.drawable.ic_inventory_grayo, "#4D4D4D",
                        R.drawable.ic_home_bluef, "#5075E8", R.drawable.ic_profile_grayo, "#4D4D4D");

                useFragment(new DashboardFragment(), R.id.main_fragment);
            }
        });

        // Profile button click
        profile_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the UI for Profile
                updateUIForFragment("Profile", R.drawable.ic_inventory_grayo, "#4D4D4D",
                        R.drawable.ic_home_grayo, "#4D4D4D", R.drawable.ic_profile_bluef, "#5075E8");

                // Retrieve username and password from the Intent
                String username = getIntent().getStringExtra("username_key");
                String password = getIntent().getStringExtra("password_key");

                // Bundle the data
                Bundle bundle = new Bundle();
                bundle.putString("username_key", username);
                bundle.putString("password_key", password);

                // Create a new UserProfileFragment instance and set the bundle as its arguments
                UserProfileFragment userProfileFragment = new UserProfileFragment();
                userProfileFragment.setArguments(bundle);

                // Display UserProfileFragment without replacing the current fragment permanently
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment, userProfileFragment)
                        .addToBackStack(null) // Allows returning to the previous fragment
                        .commit();
            }
        });


        // Inventory button click
        inventory_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIForFragment("Inventory", R.drawable.ic_inventory_bluef, "#5075E8",
                        R.drawable.ic_home_grayo, "#4D4D4D", R.drawable.ic_profile_grayo, "#4D4D4D");

                useFragment(new EditInventoryFragment(), R.id.main_fragment);
            }
        });
    }

    // Update UI for the fragment being shown (icon colors and title)
    void updateUIForFragment(String title, int inventoryIcon, String inventoryTextColor,
                             int dashboardIcon, String dashboardTextColor, int profileIcon, String profileTextColor) {
        fragment_title.setText(title);
        inventory_ic.setImageResource(inventoryIcon);
        inventory_text.setTextColor(Color.parseColor(inventoryTextColor));
        dashboard_ic.setImageResource(dashboardIcon);
        dashboard_text.setTextColor(Color.parseColor(dashboardTextColor));
        profile_ic.setImageResource(profileIcon);
        profile_text.setTextColor(Color.parseColor(profileTextColor));
    }

    // Function to switch fragments
    public void useFragment(Fragment fragment, int fragId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(fragId, fragment);
        fragmentTransaction.commit();
    }

    // Example method to write data to Firebase Realtime Database
    private void writeSampleMedicineData(String name, String quantity, String expiry) {
        String key = databaseRef.push().getKey(); // Generate a unique key for each entry
        if (key != null) {
            Medicine medicine = new Medicine(name, quantity, expiry);
            databaseRef.child(key).setValue(medicine)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Data saved to Firebase", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Medicine class to define the data structure for Firebase
    public static class Medicine {
        public String name;
        public String quantity;
        public String expiry;

        public Medicine() { }

        public Medicine(String name, String quantity, String expiry) {
            this.name = name;
            this.quantity = quantity;
            this.expiry = expiry;
        }
    }
}
