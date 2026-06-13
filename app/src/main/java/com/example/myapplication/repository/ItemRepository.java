package com.example.myapplication.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.myapplication.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import java.util.List;

/**
 * High-Performance Reactive Repository.
 * Uses Delta-Sync to prevent UI jumping and lags.
 */
public class ItemRepository {

    private final ItemDao mItemDao;
    private final TransactionDao mTransactionDao;
    private final ProductTemplateDao mTemplateDao;
    private final UserDao mUserDao;
    private final FirebaseFirestore mFirestore;
    private final AppDatabase mDb;
    private final Application mApplication;

    private ListenerRegistration inventoryListener, templateListener, transactionListener, userListener;

    public ItemRepository(Application application) {
        this.mApplication = application;
        this.mDb = AppDatabase.getDatabase(application);
        this.mItemDao = mDb.itemDao();
        this.mTransactionDao = mDb.transactionDao();
        this.mTemplateDao = mDb.productTemplateDao();
        this.mUserDao = mDb.userDao();
        mFirestore = FirebaseFirestore.getInstance();
    }

    public void startReactiveSync(String userId) {
        if (userListener != null) userListener.remove();
        userListener = mFirestore.collection("users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user != null) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                mUserDao.insert(user);
                                setupWarehouseListeners(user.getEmployerId());
                            });
                        }
                    }
                });
    }

    private void setupWarehouseListeners(String warehouseId) {
        if (warehouseId == null || "PENDING".equals(warehouseId)) return;

        // 1. SMART INVENTORY SYNC (Delta changes only)
        if (inventoryListener != null) inventoryListener.remove();
        inventoryListener = mFirestore.collection("items")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                Item item = dc.getDocument().toObject(Item.class);
                                item.setFirestoreId(dc.getDocument().getId());
                                switch (dc.getType()) {
                                    case ADDED:
                                    case MODIFIED:
                                        mItemDao.upsert(item);
                                        break;
                                    case REMOVED:
                                        mItemDao.delete(item);
                                        break;
                                }
                            }
                        });
                    }
                });

        // 2. SMART TEMPLATES SYNC
        if (templateListener != null) templateListener.remove();
        templateListener = mFirestore.collection("product_templates")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                ProductTemplate t = dc.getDocument().toObject(ProductTemplate.class);
                                t.setFirestoreId(dc.getDocument().getId());
                                if (dc.getType() != DocumentChange.Type.REMOVED) mTemplateDao.upsert(t);
                                else mTemplateDao.delete(t);
                            }
                        });
                    }
                });

        // 3. SMART TRANSACTIONS SYNC
        if (transactionListener != null) transactionListener.remove();
        transactionListener = mFirestore.collection("transactions")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                Transaction t = dc.getDocument().toObject(Transaction.class);
                                t.setFirestoreId(dc.getDocument().getId());
                                if (dc.getType() == DocumentChange.Type.ADDED) mTransactionDao.insert(t);
                            }
                        });
                    }
                });
    }

    public LiveData<User> getUserProfile(String userId) { return mUserDao.getUserProfileLiveData(userId); }
    public LiveData<List<Item>> getInventory(String warehouseId) { return mItemDao.getAllItems(warehouseId); }
    public LiveData<List<ProductTemplate>> getTemplates(String warehouseId) { return mTemplateDao.getAllTemplates(warehouseId); }

    public void updateItem(Item item, int oldQty) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item); 
            if (item.getFirestoreId() != null) {
                mFirestore.collection("items").document(item.getFirestoreId()).set(item, SetOptions.merge());
            }
            int diff = item.getQuantity() - oldQty;
            if (diff != 0) recordTransaction(diff > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS", item, Math.abs(diff));
        });
    }

    public void insertItem(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Item existing = mItemDao.getItemBySku(item.getOwnerId(), item.getSku());
            if (existing != null) {
                int old = existing.getQuantity();
                existing.setQuantity(old + item.getQuantity());
                updateItem(existing, old);
            } else {
                String id = mFirestore.collection("items").document().getId();
                item.setFirestoreId(id);
                mItemDao.insert(item);
                mFirestore.collection("items").document(id).set(item);
                recordTransaction("ADD", item, item.getQuantity());
            }
            ProductTemplate pt = new ProductTemplate(item.getName(), item.getSku(), item.getPrice(), item.getOwnerId(), item.getBrand());
            pt.setLowStockThreshold(item.getLowStockThreshold());
            upsertTemplate(pt);
        });
    }

    private void recordTransaction(String type, Item item, int qty) {
        String uid = FirebaseAuth.getInstance().getUid();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            User u = mUserDao.getUserById(uid);
            Transaction t = new Transaction(type, item.getName(), qty, qty * item.getPrice(), System.currentTimeMillis(), item.getOwnerId(), 
                    u != null ? u.getDisplayName() : "מערכת", u != null ? u.getRole() : "UNKNOWN");
            String tid = mFirestore.collection("transactions").document().getId();
            t.setFirestoreId(tid);
            mTransactionDao.insert(t);
            mFirestore.collection("transactions").document(tid).set(t);
        });
    }

    public void deleteItem(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (item.getFirestoreId() != null) mFirestore.collection("items").document(item.getFirestoreId()).delete();
            mItemDao.delete(item);
            recordTransaction("DELETE", item, item.getQuantity());
        });
    }

    public void upsertTemplate(ProductTemplate t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProductTemplate existing = mTemplateDao.getTemplateByNameAndSku(t.getOwnerId(), t.getName(), t.getSku());
            if (existing != null) { t.setId(existing.getId()); t.setFirestoreId(existing.getFirestoreId()); }
            else { t.setFirestoreId(mFirestore.collection("product_templates").document().getId()); }
            mTemplateDao.upsert(t);
            mFirestore.collection("product_templates").document(t.getFirestoreId()).set(t, SetOptions.merge());
        });
    }

    public void deleteTemplate(ProductTemplate t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (t.getFirestoreId() != null) mFirestore.collection("product_templates").document(t.getFirestoreId()).delete();
            mTemplateDao.delete(t);
        });
    }

    public void logoutAndReset(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (userListener != null) userListener.remove();
                if (inventoryListener != null) inventoryListener.remove();
                if (templateListener != null) templateListener.remove();
                if (transactionListener != null) transactionListener.remove();
                mDb.clearAllTables();
                FirebaseAuth.getInstance().signOut();
                if (onComplete != null) onComplete.run();
            } catch (Exception ignored) {}
        });
    }

    public LiveData<List<Item>> searchDatabase(String warehouseId, String q) { return mItemDao.searchDatabase(warehouseId, "%" + q + "%"); }
    public LiveData<List<Transaction>> getAllTransactions(String wid) { return mTransactionDao.searchTransactions(wid, "%%"); }
    public LiveData<List<Transaction>> searchTransactions(String wid, String q) { return mTransactionDao.searchTransactions(wid, "%" + q + "%"); }
    public LiveData<List<Transaction>> getTransactionsByRange(String wid, long s, long e) { return mTransactionDao.getTransactionsByDateRange(wid, s, e); }
    public LiveData<Integer> getCount(String wid) { return mItemDao.getTotalItemsCount(wid); }
    public LiveData<Double> getValue(String wid) { return mItemDao.getTotalInventoryValue(wid); }
}
