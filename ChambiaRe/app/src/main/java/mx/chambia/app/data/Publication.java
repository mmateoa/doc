package mx.chambia.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "publications",
    indices = {@Index("businessId")})
public class Publication {
  @androidx.annotation.NonNull
  @androidx.room.ColumnInfo(defaultValue = "'[]'")
  public String eventsJson = "[]";

  @PrimaryKey @NonNull public String id = "";
  public String businessId = "";
  public String title = "";
  public String date = "";
  public String snapshotJson = "[]";
  public String templateId = "mexicano";
  public String note = "";
  public long createdAt = 0;
  public String exportPathsJson = "[]";
  public String businessJson = "{}";
  public String destination = "TV";
  public boolean highContrast = false;
}
