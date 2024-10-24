package com.surendramaran.yolov8tflite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.surendramaran.yolov8tflite.EditInventoryFragment.Item

import android.graphics.Color
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private lateinit var viewModel: InventoryViewModel
    private lateinit var itemContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize the item container
        itemContainer = view.findViewById(R.id.item_container)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(InventoryViewModel::class.java)

        // Observe the items LiveData
        viewModel.getItems().observe(viewLifecycleOwner) { items ->
            // Clear the current views in the container
            itemContainer.removeAllViews()

            // Display the items
            items?.let {
                for (item in it) {
                    displayItem(item)
                }
            }
        }

        return view
    }

    private fun displayItem(item: Item) {
        // Create a LinearLayout for each item
        val itemView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8) // Margin around the item view
            }
            setPadding(16, 16, 16, 16) // Add padding for spacing
        }

        // Create a TextView for item details
        val textView = TextView(requireContext()).apply {
            text = "Name: ${item.name}\nQuantity: ${item.quantity}\nExpiry: ${item.expiry}"
            textSize = 16f
            setTextColor(Color.BLACK) // Set text color
            // Add padding to the text for better readability
            setPadding(8, 8, 8, 8)
        }

        // Determine the expiry color with pastel shades and reduced opacity
        val expiryDate = parseExpiryDate(item.expiry)
        val currentTime = System.currentTimeMillis()
        val color = when {
            expiryDate < currentTime -> Color.argb(150, 255, 99, 71) // Pastel red (Tomato)
            expiryDate < currentTime + DateUtils.YEAR_IN_MILLIS / 2 -> Color.argb(150, 255, 255, 102) // Pastel yellow
            else -> Color.argb(150, 144, 238, 144) // Pastel green (Light Green)
        }

        // Set the background color with reduced opacity
        itemView.setBackgroundColor(color)

        // Add TextView to itemView
        itemView.addView(textView)

        // Add itemView to the container
        itemContainer.addView(itemView)
    }




    private fun parseExpiryDate(expiry: String): Long {
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return dateFormat.parse(expiry)?.time ?: 0L
    }

    private fun getMonthsDifference(startDate: Date, endDate: Date): Int {
        val calendarStart = Calendar.getInstance().apply { time = startDate }
        val calendarEnd = Calendar.getInstance().apply { time = endDate }
        val yearDifference = calendarEnd.get(Calendar.YEAR) - calendarStart.get(Calendar.YEAR)
        val monthDifference = calendarEnd.get(Calendar.MONTH) - calendarStart.get(Calendar.MONTH)
        return yearDifference * 12 + monthDifference
    }

}
