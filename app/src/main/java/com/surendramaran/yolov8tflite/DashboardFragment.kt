package com.surendramaran.yolov8tflite

import android.content.ContentValues.TAG
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.*
import com.surendramaran.yolov8tflite.EditInventoryFragment.Item
import android.graphics.Color
import android.text.format.DateUtils
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private lateinit var viewModel: InventoryViewModel
    private lateinit var itemContainer: LinearLayout
    private lateinit var databaseRef: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        itemContainer = view.findViewById(R.id.item_container)
        // Initialize Firebase Database reference
        databaseRef = FirebaseDatabase.getInstance().getReference("meds")
        viewModel = ViewModelProvider(requireActivity()).get(InventoryViewModel::class.java)


        // Observe the items LiveData
        viewModel.getItems().observe(viewLifecycleOwner) { items ->
            itemContainer.removeAllViews()
            items?.let {
                for (item in it) {
                    displayItem(item, "")
                }
            }
        }

        // Load meds data from Firebase
        loadMedsFromFirebase()

        arguments?.getParcelableArrayList<Item>("itemList")?.let { receivedItemList ->
            if (receivedItemList.isNotEmpty()) {
                for (item in receivedItemList) {
                    writeItemToFirebase(item)
                    Log.d(TAG, "Received item: ${item.name}")
                }
            } else {
                Log.e(TAG, "Received itemList is empty")
            }
        } ?: run {
            Log.e(TAG, "No arguments received in DashboardFragment")
        }


        return view
    }

    private fun displayItem(item: Item, itemKey: String) {
        if (!isAdded) return  // Check if the fragment is attached to the activity
        val itemView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
            setPadding(16, 16, 16, 16)
        }

        val textView = TextView(requireContext()).apply {
            text = "Name: ${item.name}\nQuantity: ${item.quantity}\nExpiry: ${item.expiry}"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                val fragment = InventoryDetailsFragment()
                val bundle = Bundle().apply {
                    putString("name", item.name)
                    putString("quantity", item.quantity)
                    putString("expiry", item.expiry)
                    putString("itemKey", itemKey) // Pass the itemKey here
                }
                fragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        val expiryDate = parseExpiryDate(item.expiry)
        val currentTime = System.currentTimeMillis()
        val color = when {
            expiryDate < currentTime -> Color.argb(150, 255, 99, 71)
            expiryDate < currentTime + DateUtils.YEAR_IN_MILLIS / 2 -> Color.argb(150, 255, 255, 102)
            else -> Color.argb(150, 144, 238, 144)
        }

        itemView.setBackgroundColor(color)
        itemView.addView(textView)
        itemContainer.addView(itemView)
    }

    private fun loadMedsFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val medicineRef = database.getReference("meds")

        medicineRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (!isAdded) return  // Check if the fragment is attached to the activity

                itemContainer.removeAllViews() // Clear previous data

                for (snapshot in dataSnapshot.children) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val quantity = snapshot.child("quantity").getValue(String::class.java) ?: "0"
                    val expiry = snapshot.child("expiry").getValue(String::class.java) ?: "No Expiry Date"
                    val itemKey = snapshot.key ?: "" // Get the key of each item

                    // Create an Item object
                    val item = Item(name, quantity, expiry)
                    displayItem(item, itemKey) // Pass the itemKey to displayItem
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (!isAdded) return  // Check if the fragment is attached to the activity
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to write an item to Firebase in DashboardFragment
    private fun writeItemToFirebase(item: Item) {
        val key = databaseRef.push().key
        if (key != null) {
            databaseRef.child(key).setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item saved to Firebase", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to save item to Firebase", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e(TAG, "Failed to get Firebase key")
        }
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
