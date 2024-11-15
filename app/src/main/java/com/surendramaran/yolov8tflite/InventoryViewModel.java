package com.surendramaran.yolov8tflite;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

public class InventoryViewModel extends ViewModel {
    private final MutableLiveData<List<EditInventoryFragment.Item>> items = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<List<EditInventoryFragment.Item>> getItems() {
        return items;
    }

    public void addItem(EditInventoryFragment.Item item) {
        List<EditInventoryFragment.Item> currentItems = items.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>(); // Initialize if null
        }
        currentItems.add(item);
        items.setValue(currentItems);
    }

    public void removeItem(EditInventoryFragment.Item item) {
        List<EditInventoryFragment.Item> currentItems = items.getValue();
        if (currentItems != null) {
            currentItems.remove(item);
            items.setValue(currentItems); // Update the LiveData
        }
    }


    public void clearItems() {
        items.setValue(new ArrayList<>());
    }
}
