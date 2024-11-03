package com.surendramaran.yolov8tflite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class InventoryDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_item_details, container, false)

        // Get references to TextViews
        val nameTextView = view.findViewById<TextView>(R.id.item_name)
        val quantityTextView = view.findViewById<TextView>(R.id.item_quantity)
        val expiryTextView = view.findViewById<TextView>(R.id.item_expiry)
        val recommendationTextView = view.findViewById<TextView>(R.id.recommendation_text)

        // Retrieve data from arguments
        val name = arguments?.getString("name") ?: "Unknown"
        val quantity = arguments?.getString("quantity") ?: "0"
        val expiry = arguments?.getString("expiry") ?: "No Expiry Date"

        // Set data to TextViews
        nameTextView.text = "Name: $name"
        quantityTextView.text = "Quantity: $quantity"
        expiryTextView.text = "Expiry: $expiry"

        // Check if the medicine is near expiration and recommend actions
        val expiryDate = parseExpiryDate(expiry)
        val currentDate = System.currentTimeMillis()
        val daysToExpire = getDaysDifference(currentDate, expiryDate)

        // Determine and display recommendations based on days to expiration
        when {
            daysToExpire <= 0 -> {
                recommendationTextView.text = "This medicine has expired. Please dispose of it safely."
            }
            daysToExpire <= 30 -> {
                recommendationTextView.text = "This medicine will expire soon (in $daysToExpire days). Consider prioritizing its use or consulting with a healthcare provider if unsure."
            }
            daysToExpire <= 90 -> {
                recommendationTextView.text = "This medicine will expire in a few months (in $daysToExpire days). Make sure to use it before it expires."
            }
            else -> {
                recommendationTextView.text = "This medicine is safe to use. Make a note of the expiration date: $expiry."
            }
        }

        // Set up the back button to go back to the previous fragment
        val backButton = view.findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun parseExpiryDate(expiry: String): Long {
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return dateFormat.parse(expiry)?.time ?: 0L
    }

    private fun getDaysDifference(startDate: Long, endDate: Long): Int {
        val differenceInMillis = endDate - startDate
        return (differenceInMillis / (1000 * 60 * 60 * 24)).toInt() // Convert milliseconds to days
    }
}
