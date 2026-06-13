package com.example.myapplication.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProductTemplate template);

    @Delete
    void delete(ProductTemplate template);

    @Query("DELETE FROM product_templates_table WHERE firestoreId = :fid")
    void deleteByFirestoreId(String fid);

    @Query("SELECT * FROM product_templates_table WHERE ownerId = :userId ORDER BY name ASC")
    LiveData<List<ProductTemplate>> getAllTemplates(String userId);

    @Query("SELECT * FROM product_templates_table WHERE ownerId = :userId AND (name LIKE :query OR sku LIKE :query)")
    List<ProductTemplate> searchTemplates(String userId, String query);

    @Query("SELECT * FROM product_templates_table WHERE ownerId = :userId AND name = :name AND sku = :sku LIMIT 1")
    ProductTemplate getTemplateByNameAndSku(String userId, String name, String sku);

    @Query("DELETE FROM product_templates_table")
    void deleteAllTemplates();
}
