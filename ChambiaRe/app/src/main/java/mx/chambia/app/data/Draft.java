package mx.chambia.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/** Editable offer owned by one business; independent of catalogue rows and generated versions. */
@Entity(
    tableName = "drafts",
    indices = @androidx.room.Index("businessId"),
    foreignKeys =
        @ForeignKey(
            entity = Business.class,
            parentColumns = "id",
            childColumns = "businessId",
            onDelete = ForeignKey.CASCADE))
public class Draft {
  @PrimaryKey @NonNull public String id = "";
  @NonNull public String businessId = "";
  @NonNull public String title = "Mi preparación";
  @NonNull public String mode = "Oferta del día";
  @NonNull public String session = "";
  @NonNull public String snapshotJson = "[]";

  @NonNull
  @androidx.room.ColumnInfo(defaultValue = "'{}'")
  public String mealJson = "{}";

  public long updatedAt = 0;
}
