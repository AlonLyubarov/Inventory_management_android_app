package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.Item;
import com.example.myapplication.repository.ItemRepository;

import java.util.List;

/**
 * The ViewModel's role is to provide data to the UI and survive configuration changes.
 * It acts as a communication center between the Repository and the UI.
 */
public class ItemViewModel extends AndroidViewModel {

    private ItemRepository mRepository;

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);

    }

    /**
     * The UI will observe this LiveData to get updates.
     */
    public LiveData<List<Item>> getAllItems(String userId) {
        // We call the repository and pass the userId to get the filtered list
        return mRepository.getAllItems(userId);
    }
    /**
     * Wrapper insert method that calls the repository's insert method.
     * This keeps the implementation of the database hidden from the UI.
     */
    public void insert(Item item) {
        mRepository.insert(item);
    }

    public void update(Item item) {
        mRepository.update(item);
    }

    public void delete(Item item) {
        mRepository.delete(item);
    }
}