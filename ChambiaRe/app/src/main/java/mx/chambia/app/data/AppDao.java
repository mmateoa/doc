package mx.chambia.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
  @Query("SELECT * FROM businesses WHERE archived=0 ORDER BY name")
  List<Business> businesses();

  @Query("SELECT * FROM businesses ORDER BY archived,name")
  List<Business> allBusinesses();

  @Query("UPDATE businesses SET archived=:archived WHERE id=:id")
  void archiveBusiness(String id, boolean archived);

  @Query("DELETE FROM businesses WHERE id=:id")
  void deleteBusiness(String id);

  @Query("DELETE FROM items WHERE businessId=:id")
  void deleteBusinessItems(String id);

  @Query("DELETE FROM publications WHERE businessId=:id")
  void deleteBusinessPublications(String id);

  @androidx.room.Upsert
  void saveBusiness(Business b);

  @Query("SELECT * FROM drafts WHERE businessId=:businessId AND id=:draftId LIMIT 1")
  Draft draft(String businessId, String draftId);

  @Query("SELECT * FROM drafts WHERE businessId=:businessId ORDER BY updatedAt DESC")
  List<Draft> drafts(String businessId);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void saveDraft(Draft draft);

  @Query("SELECT * FROM items WHERE businessId=:id ORDER BY category,name")
  List<Item> items(String id);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void saveItem(Item item);

  @Query("DELETE FROM items WHERE id=:id")
  void deleteItem(String id);

  @Query("SELECT * FROM publications WHERE businessId=:businessId ORDER BY createdAt DESC")
  List<Publication> publications(String businessId);

  @Query("SELECT * FROM publications WHERE id=:id LIMIT 1")
  Publication publication(String id);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void savePublication(Publication p);

  @Query("UPDATE publications SET eventsJson=:events WHERE id=:id")
  void updatePublicationEvents(String id, String events);

  @Query("DELETE FROM publications WHERE id=:id AND businessId=:businessId")
  void deletePublication(String id, String businessId);
}
