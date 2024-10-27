package com.surendramaran.yolov8tflite;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                updateUIForFragment("Profile", R.drawable.ic_inventory_grayo, "#4D4D4D",
                        R.drawable.ic_home_grayo, "#4D4D4D", R.drawable.ic_profile_bluef, "#5075E8");

                useFragment(new UserProfileFragment(), R.id.main_fragment);
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






}
