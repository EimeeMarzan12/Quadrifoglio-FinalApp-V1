package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment

class UserProfileFragment : Fragment() {
    private lateinit var viewLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewLayout = inflater.inflate(R.layout.fragment_user_profile, container, false)

        // Find the Sign Out button
        val signOutButton: RelativeLayout = viewLayout.findViewById(R.id.user_signout_layout)

        // Set an OnClickListener for the Sign Out button
        signOutButton.setOnClickListener {
            // Create an intent to navigate to the Login activity
            val intent = Intent(activity, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()  // Optional: Close the current fragment/activity to prevent back navigation
        }

        return viewLayout
    }
}
