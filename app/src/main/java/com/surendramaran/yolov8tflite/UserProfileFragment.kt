
package com.surendramaran.yolov8tflite
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class UserProfileFragment : Fragment() {
    private lateinit var viewLayout: View
    private lateinit var logBtn: ImageView // Assuming log_btn is an ImageView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewLayout = inflater.inflate(R.layout.fragment_user_profile, container, false)

        // Get the log button from MainActivity
        logBtn = (activity as MainActivity).findViewById(R.id.logInventory_btn)

        setupNavigation()

        return viewLayout
    }

    private fun setupNavigation() {
        logBtn.setOnClickListener {
            // Call the method to update UI for the CameraFragment
            (activity as? MainActivity)?.updateUIForFragment(
                "Scanning", R.drawable.ic_inventory_grayo, "#4D4D4D",
                R.drawable.ic_home_grayo, "#4D4D4D", R.drawable.ic_profile_grayo, "#4D4D4D"
            )

            // Create and navigate to CameraFragment
            val cameraFragment = CameraFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .addToBackStack(null) // Add to back stack if you want to return
                .commit()
        }
    }

}