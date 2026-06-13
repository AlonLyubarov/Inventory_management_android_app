package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.myapplication.model.*;
import com.example.myapplication.repository.ItemRepository;
import java.util.List;

/**
 * High-Performance stable ViewModel.
 * Uses switchMap to ensure only one active stream per data type.
 */
public class ItemViewModel extends AndroidViewModel {

    private final ItemRepository mRepository;
    private final MutableLiveData<String> warehouseIdTrigger = new MutableLiveData<>();
    
    // Stable Streams
    private final LiveData<List<Item>> inventoryStream;
    private final LiveData<List<ProductTemplate>> templateStream;

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);
        
        // switchMap ensures that when warehouseId changes, old observers are automatically swapped
        inventoryStream = Transformations.switchMap(warehouseIdTrigger, mRepository::getInventory);
        templateStream = Transformations.switchMap(warehouseIdTrigger, mRepository::getTemplates);
    }

    public void startSync(String userId) { mRepository.startReactiveSync(userId); }
    
    /**
     * Updates the active warehouse context. 
     * This triggers the reactive streams to point to the new data source.
     */
    public void setWarehouseContext(String warehouseId) {
        if (warehouseId != null && !warehouseId.equals(warehouseIdTrigger.getValue())) {
            warehouseIdTrigger.setValue(warehouseId);
        }
    }

    public LiveData<User> getUserProfile(String userId) { return mRepository.getUserProfile(userId); }
    
    public LiveData<List<Item>> getInventoryStream() { return inventoryStream; }
    
    public LiveData<List<ProductTemplate>> getTemplateStream() { return templateStream; }

    public void insert(Item item) { mRepository.insertItem(item); }
    public void update(Item item, int oldQty) { mRepository.updateItem(item, oldQty); }
    public void delete(Item item) { mRepository.deleteItem(item); }
    public void upsertTemplate(ProductTemplate t) { mRepository.upsertTemplate(t); }
    public void deleteTemplate(ProductTemplate t) { mRepository.deleteTemplate(t); }

    public LiveData<List<Transaction>> getTransactions(String wid) { return mRepository.getAllTransactions(wid); }
    public LiveData<List<Transaction>> searchTransactions(String wid, String q) { return mRepository.searchTransactions(wid, q); }
    public LiveData<List<Transaction>> getTransactionsRange(String wid, long s, long e) { return mRepository.getTransactionsByRange(wid, s, e); }
    
    public LiveData<Integer> getTotalItemsCount(String wid) { return mRepository.getCount(wid); }
    public LiveData<Double> getTotalInventoryValue(String wid) { return mRepository.getValue(wid); }

    public LiveData<List<Item>> search(String wid, String q) { return mRepository.searchDatabase(wid, q); }

    public void logoutAndReset(Runnable onComplete) { mRepository.logoutAndReset(onComplete); }
    public void logoutOnly(Runnable onComplete) { mRepository.logoutOnly(onComplete); }

    // Legacy support
    public LiveData<List<Item>> getInventory(String warehouseId) { return mRepository.getInventory(warehouseId); }
    public LiveData<List<ProductTemplate>> getTemplates(String warehouseId) { return mRepository.getTemplates(warehouseId); }
}
