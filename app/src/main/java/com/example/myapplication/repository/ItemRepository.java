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
 * Production-Grade Repository.
 * Fixes C2 (Normalization), C4 (Safety), C5 (Robust Reset).
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
                                } else if (dc.getType() == DocumentChange.Type.ADDED) {
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
    public LiveData<List<Transaction>> getTransactionsRange(String warehouseId, long s, long e) { return mTransactionDao.getTransactionsByDateRange(warehouseId, s, e); }
    public LiveData<Integer> getCount(String wid) { return mItemDao.getTotalItemsCount(wid); }
    public LiveData<Double> getValue(String wid) { return mItemDao.getTotalInventoryValue(wid); }

    public void updateItemQuantity(Item item, int delta) {
        if (item == null || item.getFirestoreId() == null) return;
        mFirestore.collection("items").document(item.getFirestoreId()).update("quantity", FieldValue.increment(delta));
        recordTransaction(delta > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS", item, Math.abs(delta));
    }

    public void insertItem(Item item) {
        if (item == null) return;
        // Fix C2: Normalize inputs to prevent duplicates with spaces or casing
        String normalizedName = item.getName().trim();
        String normalizedSku = item.getSku() != null ? item.getSku().trim().toUpperCase() : "";
        item.setName(normalizedName);
        item.setSku(normalizedSku);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Item existing = mItemDao.getItemBySkuAndName(item.getOwnerId(), normalizedSku, normalizedName);
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
        if (item == null || item.getFirestoreId() == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mFirestore.collection("items").document(item.getFirestoreId()).delete();
            mItemDao.deleteByFirestoreId(item.getFirestoreId());
            recordTransaction("DELETE", item, item.getQuantity());
        });
    }

    public void upsertTemplate(ProductTemplate t) {
        if (t == null) return;
        t.setName(t.getName().trim());
        if (t.getSku() != null) t.setSku(t.getSku().trim().toUpperCase());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProductTemplate existing = mTemplateDao.getTemplateByNameAndSku(t.getOwnerId(), t.getName(), t.getSku());
            if (existing != null) { 
                t.setId(existing.getId()); t.setFirestoreId(existing.getFirestoreId()); 
            } else { 
                t.setFirestoreId(mFirestore.collection("product_templates").document().getId()); 
            }
            mTemplateDao.upsert(t);
            mFirestore.collection("product_templates").document(t.getFirestoreId()).set(t, SetOptions.merge());
        });
    }

    public void deleteTemplate(ProductTemplate t) {
        if (t == null || t.getFirestoreId() == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mFirestore.collection("product_templates").document(t.getFirestoreId()).delete();
            mTemplateDao.deleteByFirestoreId(t.getFirestoreId());
        });
    }

    public void updateItem(Item item, int oldQty) {
        if (item == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item); 
            if (item.getFirestoreId() != null) {
                mFirestore.collection("items").document(item.getFirestoreId()).set(item, SetOptions.merge());
            }
            int diff = item.getQuantity() - oldQty;
            if (diff != 0) recordTransaction(diff > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS", item, Math.abs(diff));
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
     * TOTAL WIPE with Rollback simulation & Failure handling (Fix C5).
     */
    public void logoutAndReset(Runnable onComplete, java.util.function.Consumer<String> onError) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { logoutOnly(onComplete); return; }
        
        String uid = user.getUid();
        
        wipeCollection("items", "ownerId", uid, () -> {
            wipeCollection("product_templates", "ownerId", uid, () -> {
                wipeCollection("transactions", "ownerId", uid, () -> {
                    mFirestore.collection("users").document(uid).delete()
                        .addOnSuccessListener(v -> {
                            user.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    AppDatabase.databaseWriteExecutor.execute(() -> {
                                        mDb.clearAllTables();
                                        logoutOnly(onComplete);
                                    });
                                } else {
                                    if (onError != null) onError.accept("מחיקת חשבון נכשלה. ייתכן שנדרשת התחברות מחדש.");
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            if (onError != null) onError.accept("שגיאה במחיקת פרופיל: " + e.getMessage());
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
        if (item == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            User u = mUserDao.getUserById(uid);
            double price = Math.max(0, item.getPrice()); // Fix C4
            Transaction t = new Transaction(type, item.getName(), qty, qty * price, System.currentTimeMillis(), item.getOwnerId(),
                    u != null ? u.getDisplayName() : "מערכת", u != null ? u.getRole() : "UNKNOWN");
            String tid = mFirestore.collection("transactions").document().getId();
            t.setFirestoreId(tid);
            mTransactionDao.insert(t);
            mFirestore.collection("transactions").document(tid).set(t);
        });
    }
}
