package com.example.myapplication.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.AppDatabase;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ItemDao;
import com.example.myapplication.model.ProductTemplate;
import com.example.myapplication.model.ProductTemplateDao;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.model.TransactionDao;
import com.example.myapplication.model.User;
import com.example.myapplication.model.UserDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

/**
 * Repository class that handles data operations for Room, Firestore, and Team-based sync.
 */
public class ItemRepository {

    private ItemDao mItemDao;
    private TransactionDao mTransactionDao;
    private ProductTemplateDao mTemplateDao;
    private UserDao mUserDao;
    private FirebaseFirestore mFirestore;
    private AppDatabase mDb;
    private Application mApplication;
    private static final String COLLECTION_ITEMS = "items";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_TEMPLATES = "product_templates";

    private User currentUserProfile; 

    public ItemRepository(Application application) {
        mApplication = application;
        mDb = AppDatabase.getDatabase(application);
        mItemDao = mDb.itemDao();
        mTransactionDao = mDb.transactionDao();
        mTemplateDao = mDb.productTemplateDao();
        mUserDao = mDb.userDao();
        mFirestore = FirebaseFirestore.getInstance();
        
        loadCurrentUserProfile();
    }

    private void loadCurrentUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                currentUserProfile = mUserDao.getUserById(uid);
            });
        }
    }

    public void syncFromCloud(String userId) {
        mFirestore.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    String warehouseId = user.getEmployerId();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        mUserDao.insert(user);
                        currentUserProfile = user;
                    });
                    pullInventory(warehouseId);
                }
            }
        });
    }

    private void pullInventory(String warehouseId) {
        if (warehouseId == null) return;

        mFirestore.collection(COLLECTION_ITEMS)
                .whereEqualTo("ownerId", warehouseId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Item item = doc.toObject(Item.class);
                            item.setFirestoreId(doc.getId());
                            mItemDao.upsert(item);
                        }
                    });
                });

        mFirestore.collection(COLLECTION_TEMPLATES)
                .whereEqualTo("ownerId", warehouseId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ProductTemplate template = doc.toObject(ProductTemplate.class);
                            template.setFirestoreId(doc.getId());
                            ProductTemplate localT = mTemplateDao.getTemplateByNameAndSku(warehouseId, template.getName(), template.getSku());
                            if (localT != null) {
                                template.setId(localT.getId());
                                mTemplateDao.upsert(template);
                            } else {
                                mTemplateDao.upsert(template);
                            }
                        }
                    });
                });

        mFirestore.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("ownerId", warehouseId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Transaction t = doc.toObject(Transaction.class);
                            t.setFirestoreId(doc.getId());
                            mTransactionDao.insert(t); 
                        }
                    });
                });
    }

    public LiveData<List<Item>> getAllItems(String userId) {
        return mItemDao.getAllItems(userId);
    }

    public void insert(Item newItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String warehouseId = (currentUserProfile != null) ? currentUserProfile.getEmployerId() : newItem.getOwnerId();
            String brand = (newItem.getBrand() != null) ? newItem.getBrand() : "";
            
            newItem.setOwnerId(warehouseId);

            Item existingItem = mItemDao.getItemBySku(warehouseId, newItem.getSku());

            if (existingItem != null) {
                int oldQty = existingItem.getQuantity();
                existingItem.setQuantity(oldQty + newItem.getQuantity());
                existingItem.setPrice(newItem.getPrice()); 
                existingItem.setBrand(brand);
                mItemDao.update(existingItem);
                
                recordTransaction("UPDATE_PLUS", existingItem.getName(), newItem.getQuantity(), 
                        newItem.getQuantity() * newItem.getPrice(), warehouseId);

                if (existingItem.getFirestoreId() != null) {
                    mFirestore.collection(COLLECTION_ITEMS).document(existingItem.getFirestoreId())
                            .set(existingItem, SetOptions.merge());
                }
            } else {
                String cloudId = mFirestore.collection(COLLECTION_ITEMS).document().getId();
                newItem.setFirestoreId(cloudId);
                mItemDao.insert(newItem);
                
                recordTransaction("ADD", newItem.getName(), newItem.getQuantity(), 
                        newItem.getQuantity() * newItem.getPrice(), warehouseId);

                mFirestore.collection(COLLECTION_ITEMS).document(cloudId).set(newItem);
            }
            
            ProductTemplate template = new ProductTemplate(newItem.getName(), newItem.getSku(), newItem.getPrice(), warehouseId, brand);
            template.setLowStockThreshold(newItem.getLowStockThreshold());
            upsertTemplate(template);
        });
    }

    public void upsertTemplate(ProductTemplate template) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProductTemplate existing = mTemplateDao.getTemplateByNameAndSku(template.getOwnerId(), template.getName(), template.getSku());
            if (existing != null) {
                template.setId(existing.getId());
                template.setFirestoreId(existing.getFirestoreId());
                mTemplateDao.upsert(template);
                if (template.getFirestoreId() != null) {
                    mFirestore.collection(COLLECTION_TEMPLATES).document(template.getFirestoreId()).set(template, SetOptions.merge());
                }
            } else {
                String cloudId = mFirestore.collection(COLLECTION_TEMPLATES).document().getId();
                template.setFirestoreId(cloudId);
                mTemplateDao.upsert(template);
                mFirestore.collection(COLLECTION_TEMPLATES).document(cloudId).set(template);
            }
        });
    }

    public void deleteTemplate(ProductTemplate template) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (template.getFirestoreId() != null) {
                mFirestore.collection(COLLECTION_TEMPLATES).document(template.getFirestoreId()).delete();
            }
            mTemplateDao.delete(template);
        });
    }

    public LiveData<List<ProductTemplate>> getAllTemplates(String userId) {
        return mTemplateDao.getAllTemplates(userId);
    }

    public void update(Item item, int oldQuantity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item);
            int diff = item.getQuantity() - oldQuantity;
            if (diff != 0) {
                String type = diff > 0 ? "UPDATE_PLUS" : "UPDATE_MINUS";
                recordTransaction(type, item.getName(), Math.abs(diff), 
                        Math.abs(diff) * item.getPrice(), item.getOwnerId());
            }

            if (item.getFirestoreId() != null) {
                mFirestore.collection(COLLECTION_ITEMS).document(item.getFirestoreId())
                        .set(item, SetOptions.merge());
            }
        });
    }

    public void delete(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (item.getFirestoreId() != null) {
                mFirestore.collection(COLLECTION_ITEMS).document(item.getFirestoreId()).delete();
            }
            mItemDao.delete(item);
            recordTransaction("DELETE", item.getName(), item.getQuantity(), 
                    item.getQuantity() * item.getPrice(), item.getOwnerId());
        });
    }

    private void recordTransaction(String type, String itemName, int qty, double amount, String warehouseId) {
        long now = System.currentTimeMillis();
        String name = "מערכת";
        String role = "UNKNOWN";
        if (currentUserProfile != null) {
            name = currentUserProfile.getDisplayName();
            role = currentUserProfile.getRole();
        }
        
        Transaction t = new Transaction(type, itemName, qty, amount, now, warehouseId, name, role);
        String cloudId = mFirestore.collection(COLLECTION_TRANSACTIONS).document().getId();
        t.setFirestoreId(cloudId);
        mTransactionDao.insert(t);
        mFirestore.collection(COLLECTION_TRANSACTIONS).document(cloudId).set(t);
    }

    public LiveData<User> getUserProfile(String userId) {
        return mUserDao.getUserProfileLiveData(userId);
    }

    public LiveData<List<Item>> searchDatabase(String searchQuery) {
        return mItemDao.searchDatabase(searchQuery);
    }

    public LiveData<List<Transaction>> getAllTransactions(String userId) {
        return mTransactionDao.searchTransactions(userId, "%%");
    }

    public LiveData<List<Transaction>> searchTransactions(String userId, String query) {
        return mTransactionDao.searchTransactions(userId, "%" + query + "%");
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long start, long end) {
        return mTransactionDao.getTransactionsByDateRange(userId, start, end);
    }

    public LiveData<Integer> getTotalItemsCount(String userId) {
        return mItemDao.getTotalItemsCount(userId);
    }

    public LiveData<Double> getTotalInventoryValue(String userId) {
        return mItemDao.getTotalInventoryValue(userId);
    }

    public void cleanOldTransactions(long threshold, String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTransactionDao.deleteOldTransactions(threshold);
            mFirestore.collection(COLLECTION_TRANSACTIONS)
                    .whereEqualTo("ownerId", userId)
                    .whereLessThan("timestamp", threshold)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        WriteBatch batch = mFirestore.batch();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit();
                    });
        });
    }

    public void logoutOnly(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                mDb.clearAllTables();
                FirebaseAuth.getInstance().signOut();
                currentUserProfile = null;
                if (onComplete != null) onComplete.run();
            } catch (Exception e) {
                Log.e("Logout", "Logout failed", e);
            }
        });
    }

    public void logoutAndReset(Runnable onComplete) {
        String uid = FirebaseAuth.getInstance().getUid();
        
        // 1. Delete user document from Cloud so they MUST re-register
        if (uid != null) {
            mFirestore.collection("users").document(uid).delete()
                .addOnCompleteListener(task -> {
                    // 2. Clear local data and sign out regardless of cloud success
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            mDb.clearAllTables();
                            FirebaseAuth.getInstance().signOut();
                            currentUserProfile = null;
                            Log.d("Logout", "Cloud profile deleted and local DB cleared");
                            
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        } catch (Exception e) {
                            Log.e("Logout", "Reset failed", e);
                        }
                    });
                });
        } else {
            // Not logged in anyway, just clear and finish
            if (onComplete != null) onComplete.run();
        }
    }
}
