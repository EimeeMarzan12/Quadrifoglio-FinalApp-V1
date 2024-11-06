
package com.surendramaran.yolov8tflite
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.core.Context

class UserProfileFragment : Fragment() {
    private lateinit var viewLayout: View
    private lateinit var logBtn: ImageView // Assuming log_btn is an ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userUsernameTextView: TextView
    private lateinit var userPasswordTextView: TextView
    private lateinit var signOutLayout: RelativeLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewLayout = inflater.inflate(R.layout.fragment_user_profile, container, false)

        // Get the log button from MainActivity
        logBtn = (activity as MainActivity).findViewById(R.id.logInventory_btn)

        // Initialize TextViews
        userNameTextView = viewLayout.findViewById(R.id.user_name)
        userUsernameTextView = viewLayout.findViewById(R.id.user_username)  // Correct initialization here
        userPasswordTextView = viewLayout.findViewById(R.id.user_password)

        // Retrieve the username and password from the arguments
                val username = arguments?.getString("username_key")
                val password = arguments?.getString("password_key")

        // Set the TextViews with the retrieved values
                userNameTextView.text = username ?.uppercase() ?: "Username not found"  // Set username to user_name TextView
                userUsernameTextView.text = username ?: "Username not found"  // Set username to user_username TextView
        // Set the password text as asterisks
                userPasswordTextView.text = password?.let { "*".repeat(it.length) } ?: "Password not found"

        setupNavigation()

        // Initialize the sign-out RelativeLayout (used for clicking the layout to sign out)
        signOutLayout = viewLayout.findViewById(R.id.user_signout_layout)

        // Set click listener for the sign-out button
        signOutLayout.setOnClickListener {
            signOutUser()
        }

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

    // Sign out the user
    private fun signOutUser() {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Redirect to the Login Activity
        val intent = Intent(activity, Login::class.java)
        startActivity(intent)

        // Optionally, finish the current activity to remove it from the back stack
        activity?.finish()
    }

}