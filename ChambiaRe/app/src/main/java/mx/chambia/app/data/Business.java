package mx.chambia.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "businesses")
public class Business {
  @PrimaryKey @NonNull public String id = "";
  public String name = "";
  public String sector = "fonda";
  public String modality = "Local fijo";
  public String phone = "";
  public String slogan = "Dale vía a tus chambitas";
  public String templateId = "mexicano";
  public boolean archived = false;
  @NonNull public String profileJson = "{}";
  @androidx.room.Ignore public String mealJson = "{}";
  @androidx.room.Ignore public String publicationHeading = "";
  @androidx.room.Ignore public String publicationSession = "";
}
