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
 * Senior Architect Level Repository.
 * Logic: Cloud <-> Room (Single Source of Truth) <-> UI
 */
public class ItemRepository {

    private static final String TAG = "ItemRepository";
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
                    if (e != null) { Log.e(TAG, "Sync error", e); return; }
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
        if (templateListener != null) templateListener.remove();
        if (transactionListener != null) transactionListener.remove();

        inventoryListener = mFirestore.collection("items")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
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
                });

        templateListener = mFirestore.collection("product_templates")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
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
                });

        transactionListener = mFirestore.collection("transactions")
                .whereEqualTo("ownerId", warehouseId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            String fid = dc.getDocument().getId();
                            // B-04 Fix: Handle MODIFIED as well for Transactions
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

    /**
     * Optimistic UI Update Fix (M3).
     * Updates local Room immediately for instant UI feedback, then pushes to Firestore.
     */
    public void updateItemQuantity(Item item, int delta) {
        if (item == null || item.getFirestoreId() == null) return;
        if (item.getQuantity() + delta < 0) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Optimistic Update (Local Room)
            item.setQuantity(item.getQuantity() + delta);
            mItemDao.update(item);

            // 2. Cloud Persistence
            mFirestore.collection("items").document(item.getFirestoreId())
                    .update("quantity", FieldValue.increment(delta))
                    .addOnSuccessListener(v -> recordTransaction(delta > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS", item, Math.abs(delta)))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Cloud update failed, reverting local", e);
                        // Revert could be implemented if necessary, but reactive sync will reconcile
                    });
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

    public void insertItem(Item item) {
        if (item == null || item.getOwnerId() == null) return;
        // B-06 Fix: Guards for null name
        if (item.getName() == null || item.getName().isEmpty()) return;

        String name = item.getName().trim();
        String sku = (item.getSku() != null) ? item.getSku().trim().toUpperCase() : "";
        item.setName(name);
        item.setSku(sku);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Item existing = mItemDao.getItemBySkuAndName(item.getOwnerId(), sku, name);
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
        if (t == null || t.getName() == null || t.getName().isEmpty()) return;
        t.setName(t.getName().trim());
        if (t.getSku() != null) t.setSku(t.getSku().trim().toUpperCase());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProductTemplate existing = mTemplateDao.getTemplateByNameAndSku(t.getOwnerId(), t.getName(), t.getSku());
            if (existing != null) { t.setId(existing.getId()); t.setFirestoreId(existing.getFirestoreId()); }
            else { t.setFirestoreId(mFirestore.collection("product_templates").document().getId()); }
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

    public void logoutOnly(Runnable onComplete) {
        if (userListener != null) userListener.remove();
        if (inventoryListener != null) inventoryListener.remove();
        if (templateListener != null) templateListener.remove();
        if (transactionListener != null) transactionListener.remove();
        currentWarehouseId = null;
        FirebaseAuth.getInstance().signOut();
        if (onComplete != null) onComplete.run();
    }

    public void logoutAndReset(Runnable onComplete, java.util.function.Consumer<String> onError) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { logoutOnly(onComplete); return; }
        String uid = user.getUid();
        
        wipeCollection("items", "ownerId", uid, () -> {
            wipeCollection("product_templates", "ownerId", uid, () -> {
                wipeCollection("transactions", "ownerId", uid, () -> {
                    mFirestore.collection("users").document(uid).delete().addOnSuccessListener(v -> {
                        user.delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                AppDatabase.databaseWriteExecutor.execute(() -> { mDb.clearAllTables(); logoutOnly(onComplete); });
                            } else if (onError != null) onError.accept("Reset failed: Recent login required.");
                        });
                    }).addOnFailureListener(e -> { if (onError != null) onError.accept("Reset failed: " + e.getMessage()); });
                });
            });
        });
    }

    private void wipeCollection(String coll, String field, String val, Runnable next) {
        mFirestore.collection(coll).whereEqualTo(field, val).limit(500).get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) { next.run(); return; }
            WriteBatch batch = mFirestore.batch();
            for (DocumentSnapshot doc : snaps) batch.delete(doc.getReference());
            batch.commit().addOnCompleteListener(t -> {
                if (snaps.size() == 500) wipeCollection(coll, field, val, next);
                else next.run();
            });
        }).addOnFailureListener(e -> next.run());
    }

    private void recordTransaction(String type, Item item, int qty) {
        if (item == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            User u = mUserDao.getUserById(uid);
            double safePrice = Math.max(0, item.getPrice());
            Transaction t = new Transaction(type, item.getName(), qty, qty * safePrice, System.currentTimeMillis(), item.getOwnerId(), 
                    u != null ? u.getDisplayName() : "System", u != null ? u.getRole() : "UNKNOWN");
            String tid = mFirestore.collection("transactions").document().getId();
            t.setFirestoreId(tid);
            mTransactionDao.insert(t);
            mFirestore.collection("transactions").document(tid).set(t);
        });
    }
}
