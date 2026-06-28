package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.myapplication.model.*;
import com.example.myapplication.repository.ItemRepository;
import java.util.List;

/**
 * Senior Level Reactive ViewModel.
 */
public class ItemViewModel extends AndroidViewModel {

    private final ItemRepository mRepository;
    private final MutableLiveData<String> warehouseIdTrigger = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> dashboardSearchQuery = new MutableLiveData<>("");
    
    // B-09 Fix: Date filtering state
    private final MutableLiveData<Pair<Long, Long>> dashboardDateRange = new MutableLiveData<>();
    private boolean isFilteringByDate = false;

    private final LiveData<List<Item>> inventoryStream;
    private final LiveData<List<ProductTemplate>> templateStream;
    private final LiveData<List<Item>> searchStream;
    
    private final MediatorLiveData<List<Transaction>> dashboardSearchStream = new MediatorLiveData<>();

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);
        
        inventoryStream = Transformations.switchMap(warehouseIdTrigger, mRepository::getInventory);
        templateStream = Transformations.switchMap(warehouseIdTrigger, mRepository::getTemplates);
        
        searchStream = Transformations.switchMap(searchQuery, query -> {
            String wid = warehouseIdTrigger.getValue();
            if (wid == null) return new MutableLiveData<>();
            // B-07 Fix: Escape special SQL characters
            String safe = query.replace("%", "\\%").replace("_", "\\_");
            return mRepository.searchDatabase(wid, safe);
        });

        // dashboardSearchStream reacts to warehouse, search query, and date range
        dashboardSearchStream.addSource(warehouseIdTrigger, wid -> refreshDashboard());
        dashboardSearchStream.addSource(dashboardSearchQuery, q -> {
            isFilteringByDate = false;
            refreshDashboard();
        });
        dashboardSearchStream.addSource(dashboardDateRange, range -> {
            isFilteringByDate = (range != null);
            refreshDashboard();
        });
    }

    private void refreshDashboard() {
        String wid = warehouseIdTrigger.getValue();
        if (wid == null) return;

        LiveData<List<Transaction>> source;
        if (isFilteringByDate && dashboardDateRange.getValue() != null) {
            Pair<Long, Long> range = dashboardDateRange.getValue();
            source = mRepository.getTransactionsRange(wid, range.first, range.second);
        } else {
            String q = dashboardSearchQuery.getValue();
            if (q == null || q.isEmpty()) {
                source = mRepository.getAllTransactions(wid);
            } else {
                String safe = q.replace("%", "\\%").replace("_", "\\_");
                source = mRepository.searchTransactions(wid, safe);
            }
        }

        dashboardSearchStream.addSource(source, transactions -> {
            dashboardSearchStream.setValue(transactions);
            dashboardSearchStream.removeSource(source);
        });
    }

    public void startSync(String userId) { mRepository.startReactiveSync(userId); }
    
    public void setWarehouseContext(String warehouseId) {
        if (warehouseId != null && !warehouseId.equals(warehouseIdTrigger.getValue())) {
            warehouseIdTrigger.setValue(warehouseId);
        }
    }

    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public void setDashboardSearchQuery(String query) { dashboardSearchQuery.setValue(query); }
    
    public void setDashboardDateRange(Long start, Long end) {
        if (start == null || end == null) dashboardDateRange.setValue(null);
        else dashboardDateRange.setValue(new Pair<>(start, end));
    }

    public LiveData<User> getUserProfile(String userId) { return mRepository.getUserProfile(userId); }
    public LiveData<List<Item>> getInventoryStream() { return inventoryStream; }
    public LiveData<List<ProductTemplate>> getTemplateStream() { return templateStream; }
    public LiveData<List<Item>> getSearchStream() { return searchStream; }
    public LiveData<List<Transaction>> getDashboardSearchStream() { return dashboardSearchStream; }

    public void insert(Item item) { mRepository.insertItem(item); }
    public void updateItemQuantity(Item item, int delta) { mRepository.updateItemQuantity(item, delta); }
    public void update(Item item, int oldQty) { mRepository.updateItem(item, oldQty); }
    public void delete(Item item) { mRepository.deleteItem(item); }
    public void upsertTemplate(ProductTemplate t) { mRepository.upsertTemplate(t); }
    public void deleteTemplate(ProductTemplate t) { mRepository.deleteTemplate(t); }

    public LiveData<List<Transaction>> getAllTransactions(String wid) { return mRepository.getAllTransactions(wid); }
    public LiveData<List<Transaction>> getTransactionsRange(String wid, long s, long e) { return mRepository.getTransactionsRange(wid, s, e); }
    
    public LiveData<Integer> getTotalItemsCount(String wid) { return mRepository.getCount(wid); }
    public LiveData<Double> getTotalInventoryValue(String wid) { return mRepository.getValue(wid); }

    public void logoutAndReset(Runnable onComplete, java.util.function.Consumer<String> onError) {
        mRepository.logoutAndReset(onComplete, onError);
    }
    public void logoutOnly(Runnable onComplete) { mRepository.logoutOnly(onComplete); }

    // Bridges
    public LiveData<List<Item>> getInventory(String wid) { return mRepository.getInventory(wid); }
    public LiveData<List<ProductTemplate>> getTemplates(String wid) { return mRepository.getTemplates(wid); }
    public LiveData<List<Item>> search(String wid, String q) { return mRepository.searchDatabase(wid, q); }

    private static class Pair<A, B> {
        public final A first;
        public final B second;
        public Pair(A a, B b) { this.first = a; this.second = b; }
    }
}
