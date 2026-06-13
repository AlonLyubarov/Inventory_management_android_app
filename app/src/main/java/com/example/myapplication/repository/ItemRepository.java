package com.example.myapplication.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.myapplication.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.List;

/**
 * Advanced Reactive Repository with Full Cloud Wipe.
 */
public class ItemRepository {

    private final ItemDao mItemDao;
    private final TransactionDao mTransactionDao;
    private final ProductTemplateDao mTemplateDao;
    private final UserDao mUserDao;
    private final FirebaseFirestore mFirestore;
    private final AppDatabase mDb;

    private ListenerRegistration inventoryListener, templateListener, transactionListener, userListener;
    private String currentWarehouseId;

    public ItemRepository(Application application) {
        this.mDb = AppDatabase.getDatabase(application);
        this.mItemDao = mDb.itemDao();
        this.mTransactionDao = mDb.transactionDao();
        this.mTemplateDao = mDb.productTemplateDao();
        this.mUserDao = mDb.userDao();
        mFirestore = FirebaseFirestore.getInstance();
    }

    public void startReactiveSync(String userId) {
        if (userId == null) return;
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

    private synchronized void setupWarehouseListeners(String warehouseId) {
        if (warehouseId == null || "PENDING".equals(warehouseId)) return;
        if (warehouseId.equals(currentWarehouseId)) return;
        currentWarehouseId = warehouseId;

        // 1. INVENTORY SYNC
        if (inventoryListener != null) inventoryListener.remove();
        inventoryListener = mFirestore.collection("items")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                String fid = dc.getDocument().getId();
                                if (dc.getType() == DocumentChange.Type.REMOVED) {
                                    mItemDao.deleteByFirestoreId(fid);
                                } else {
                                    Item item = dc.getDocument().toObject(Item.class);
                                    if (item != null) {
                                        item.setFirestoreId(fid);
                                        Item local = mItemDao.getItemByFirestoreId(fid);
                                        if (local != null) item.setId(local.getId());
                                        mItemDao.smartUpsert(item);
                                    }
                                }
                            }
                        });
                    }
                });

        // 2. TEMPLATES SYNC
        if (templateListener != null) templateListener.remove();
        templateListener = mFirestore.collection("product_templates")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                String fid = dc.getDocument().getId();
                                if (dc.getType() == DocumentChange.Type.REMOVED) {
                                    mTemplateDao.deleteByFirestoreId(fid);
                                } else {
                                    ProductTemplate t = dc.getDocument().toObject(ProductTemplate.class);
                                    if (t != null) {
                                        t.setFirestoreId(fid);
                                        mTemplateDao.upsert(t);
                                    }
                                }
                            }
                        });
                    }
                });

        // 3. TRANSACTIONS SYNC
        if (transactionListener != null) transactionListener.remove();
        transactionListener = mFirestore.collection("transactions")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                String fid = dc.getDocument().getId();
                                if (dc.getType() == DocumentChange.Type.REMOVED) {
                                    mTransactionDao.deleteByFirestoreId(fid);
                                } else {
                                    Transaction t = dc.getDocument().toObject(Transaction.class);
                                    if (t != null) {
                                        t.setFirestoreId(fid);
                                        Transaction local = mTransactionDao.getByFirestoreId(fid);
                                        if (local != null) t.setId(local.getId());
                                        mTransactionDao.insert(t);
                                    }
                                }
                            }
                        });
                    }
                });
    }

    public LiveData<User> getUserProfile(String userId) { return mUserDao.getUserProfileLiveData(userId); }
    public LiveData<List<Item>> getInventory(String warehouseId) { return mItemDao.getAllItems(warehouseId); }
    public LiveData<List<ProductTemplate>> getTemplates(String warehouseId) { return mTemplateDao.getAllTemplates(warehouseId); }
    public LiveData<List<Item>> searchDatabase(String warehouseId, String q) { return mItemDao.searchDatabase(warehouseId, "%" + q + "%"); }
    public LiveData<List<Transaction>> getAllTransactions(String wid) { return mTransactionDao.searchTransactions(wid, "%%"); }
    public LiveData<List<Transaction>> searchTransactions(String wid, String q) { return mTransactionDao.searchTransactions(wid, "%" + q + "%"); }
    public LiveData<List<Transaction>> getTransactionsByRange(String wid, long s, long e) { return mTransactionDao.getTransactionsByDateRange(wid, s, e); }
    public LiveData<List<Transaction>> getTransactionsRange(String warehouseId, long s, long e) { return mTransactionDao.getTransactionsByDateRange(warehouseId, s, e); }
    public LiveData<Integer> getCount(String wid) { return mItemDao.getTotalItemsCount(wid); }
    public LiveData<Double> getValue(String wid) { return mItemDao.getTotalInventoryValue(wid); }

    public void updateItemQuantity(Item item, int delta) {
        if (item.getFirestoreId() == null) return;
        mFirestore.collection("items").document(item.getFirestoreId()).update("quantity", FieldValue.increment(delta));
        recordTransaction(delta > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS", item, Math.abs(delta));
    }

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
            // Fix: Check for exact match (SKU + Name) to prevent accidental merging of different items
            Item existing = mItemDao.getItemBySkuAndName(item.getOwnerId(), item.getSku(), item.getName());
            if (existing != null) {
                updateItemQuantity(existing, item.getQuantity());
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

    public void deleteItem(Item item) {
        if (item.getFirestoreId() == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mFirestore.collection("items").document(item.getFirestoreId()).delete();
            mItemDao.deleteByFirestoreId(item.getFirestoreId());
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
            mTemplateDao.deleteByFirestoreId(t.getFirestoreId());
        });
    }

    public void logoutOnly(Runnable onComplete) {
        if (userListener != null) userListener.remove();
        if (inventoryListener != null) inventoryListener.remove();
        if (templateListener != null) templateListener.remove();
        if (transactionListener != null) transactionListener.remove();
        currentWarehouseId = null;
        FirebaseAuth.getInstance().signOut();
        if (onComplete != null) onComplete.run();
    }

    /**
     * TOTAL WIPE: Deletes all cloud data associated with the user before account removal.
     */
    public void logoutAndReset(Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { logoutOnly(onComplete); return; }
        
        String uid = user.getUid();
        
        // 1. Wipe all related cloud collections first
        wipeCollection("items", "ownerId", uid, () -> {
            wipeCollection("product_templates", "ownerId", uid, () -> {
                wipeCollection("transactions", "ownerId", uid, () -> {
                    // 2. Delete user profile
                    mFirestore.collection("users").document(uid).delete().addOnCompleteListener(t1 -> {
                        // 3. Delete Auth Account
                        user.delete().addOnCompleteListener(t2 -> {
                            // 4. Clear Local Room
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                mDb.clearAllTables();
                                logoutOnly(onComplete);
                            });
                        });
                    });
                });
            });
        });
    }

    private void wipeCollection(String coll, String field, String val, Runnable next) {
        mFirestore.collection(coll).whereEqualTo(field, val).get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) { next.run(); return; }
            WriteBatch batch = mFirestore.batch();
            for (DocumentSnapshot doc : snaps) batch.delete(doc.getReference());
            batch.commit().addOnCompleteListener(t -> next.run());
        }).addOnFailureListener(e -> next.run());
    }

    private void recordTransaction(String type, Item item, int qty) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
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
}
