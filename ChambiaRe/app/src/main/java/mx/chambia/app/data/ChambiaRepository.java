package mx.chambia.app.data;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single serialized queue: reads observe all preceding autosaves. */
public final class ChambiaRepository {
  private static volatile ChambiaRepository instance;
  public final AppDatabase database;
  public final ExecutorService io = Executors.newSingleThreadExecutor();

  private ChambiaRepository(Context context) {
    database = AppDatabase.create(context);
  }

  public void deleteBusiness(String id) {
    database.runInTransaction(
        () -> {
          database.dao().deleteBusinessPublications(id);
          database.dao().deleteBusinessItems(id);
          database.dao().deleteBusiness(id);
        });
  }

  public static synchronized ChambiaRepository get(Context context) {
    if (instance == null) instance = new ChambiaRepository(context.getApplicationContext());
    return instance;
  }
}
