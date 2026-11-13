package mx.chambia.app.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {Business.class, Item.class, Publication.class, Draft.class},
    version = 9,
    exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
  public abstract AppDao dao();

  public static AppDatabase create(Context context) {
    return create(context, "chambia.db");
  }

  public static AppDatabase create(Context context, String name) {
    return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, name)
        .addMigrations(
            new androidx.room.migration.Migration(1, 2) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL(
                    "UPDATE items SET priceMode='confirmar' WHERE price=0 AND priceMode='fijo' AND"
                        + " masterId!=''");
              }
            },
            new androidx.room.migration.Migration(2, 3) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("ALTER TABLE publications ADD COLUMN exportPathsJson TEXT");
                db.execSQL("ALTER TABLE publications ADD COLUMN businessJson TEXT");
                db.execSQL("ALTER TABLE publications ADD COLUMN destination TEXT");
                db.execSQL(
                    "ALTER TABLE publications ADD COLUMN highContrast INTEGER NOT NULL DEFAULT 0");
                db.execSQL(
                    "UPDATE publications SET exportPathsJson='[]', businessJson='{}',"
                        + " destination='TV'");
              }
            },
            new androidx.room.migration.Migration(3, 4) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS drafts (businessId TEXT NOT NULL, snapshotJson TEXT"
                        + " NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(businessId), FOREIGN"
                        + " KEY(businessId) REFERENCES businesses(id) ON UPDATE NO ACTION ON DELETE"
                        + " CASCADE)");
                try (android.database.Cursor businesses =
                    database.query("SELECT id FROM businesses")) {
                  while (businesses.moveToNext()) {
                    String businessId = businesses.getString(0);
                    java.util.List<Item> selected = new java.util.ArrayList<>();
                    try (android.database.Cursor rows =
                        database.query(
                            "SELECT * FROM items WHERE businessId=? AND inDraft=1 ORDER BY"
                                + " category,name",
                            new Object[] {businessId})) {
                      while (rows.moveToNext()) {
                        Item item = new Item();
                        item.businessId = businessId;
                        item.id = rows.getString(rows.getColumnIndexOrThrow("id"));
                        item.name = rows.getString(rows.getColumnIndexOrThrow("name"));
                        item.category = rows.getString(rows.getColumnIndexOrThrow("category"));
                        item.unit = rows.getString(rows.getColumnIndexOrThrow("unit"));
                        item.description =
                            rows.getString(rows.getColumnIndexOrThrow("description"));
                        item.masterId = rows.getString(rows.getColumnIndexOrThrow("masterId"));
                        item.price = rows.getDouble(rows.getColumnIndexOrThrow("price"));
                        item.priceMode = rows.getString(rows.getColumnIndexOrThrow("priceMode"));
                        item.state = rows.getString(rows.getColumnIndexOrThrow("state"));
                        selected.add(item);
                      }
                    }
                    try {
                      database.execSQL(
                          "INSERT INTO drafts (businessId,snapshotJson,updatedAt) VALUES (?,?,?)",
                          new Object[] {
                            businessId,
                            mx.chambia.app.PublicationSnapshot.encode(selected),
                            System.currentTimeMillis()
                          });
                    } catch (Exception ex) {
                      throw new IllegalStateException(
                          "No se pudo conservar el borrador de " + businessId, ex);
                    }
                  }
                }
              }
            },
            new androidx.room.migration.Migration(4, 5) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE items ADD COLUMN variants TEXT NOT NULL DEFAULT ''");
                database.execSQL(
                    "ALTER TABLE items ADD COLUMN bundleContents TEXT NOT NULL DEFAULT ''");
              }
            },
            new androidx.room.migration.Migration(5, 6) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL(
                    "CREATE TABLE drafts_new (id TEXT NOT NULL, businessId TEXT NOT NULL, title"
                        + " TEXT NOT NULL, mode TEXT NOT NULL, session TEXT NOT NULL, snapshotJson"
                        + " TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN"
                        + " KEY(businessId) REFERENCES businesses(id) ON UPDATE NO ACTION ON DELETE"
                        + " CASCADE)");
                database.execSQL(
                    "INSERT INTO drafts_new SELECT 'active_' || businessId,businessId,'Mi"
                        + " preparación','Oferta del día','',snapshotJson,updatedAt FROM drafts");
                database.execSQL("DROP TABLE drafts");
                database.execSQL("ALTER TABLE drafts_new RENAME TO drafts");
                database.execSQL("CREATE INDEX index_drafts_businessId ON drafts (businessId)");
              }
            },
            new androidx.room.migration.Migration(6, 7) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL(
                    "ALTER TABLE businesses ADD COLUMN profileJson TEXT NOT NULL DEFAULT '{}'");
              }
            },
            new androidx.room.migration.Migration(7, 8) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL(
                    "ALTER TABLE drafts ADD COLUMN mealJson TEXT NOT NULL DEFAULT '{}' ");
              }
            },
            new androidx.room.migration.Migration(8, 9) {
              @Override
              public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                database.execSQL(
                    "ALTER TABLE publications ADD COLUMN eventsJson TEXT NOT NULL DEFAULT '[]'");
              }
            })
        .build();
  }
}
