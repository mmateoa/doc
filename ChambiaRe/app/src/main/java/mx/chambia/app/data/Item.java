package mx.chambia.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "items",
    indices = {@Index("businessId")})
public class Item {
  @PrimaryKey @NonNull public String id = "";
  public String businessId = "";
  public String name = "";
  public String category = "General";
  public String unit = "";
  public String description = "";
  public double price = 0;
  public String priceMode = "confirmar"; // fijo, desde, cotizar
  public String state = "disponible"; // disponible, agotado, fuera
  public boolean inDraft = true;
  public String masterId = "";
  @NonNull public String variants = "";
  @NonNull public String bundleContents = "";
}
