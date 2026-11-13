package mx.chambia.app.data;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class SeedCatalog {
  public static class Suggestion {
    public String masterId, name, category, unit;

    public Suggestion(String id, String name, String category, String unit) {
      this.masterId = id;
      this.name = name;
      this.category = category;
      this.unit = unit;
    }
  }

  public static List<Suggestion> load(Context context, String sector) {
    ArrayList<Suggestion> result = new ArrayList<>();
    try (InputStream in = context.getAssets().open("catalogos/" + sector + ".json");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
      JSONArray arr =
          new JSONObject(out.toString(StandardCharsets.UTF_8.name())).getJSONArray("items");
      for (int i = 0; i < arr.length(); i++) {
        JSONObject o = arr.getJSONObject(i);
        result.add(
            new Suggestion(
                o.getString("id"),
                o.getString("name"),
                o.optString("category", "General"),
                o.optString("unit", "")));
      }
    } catch (Exception ignored) {
      /* La captura manual sigue disponible. */
    }
    return result;
  }

  public static Item toItem(Suggestion s, String businessId) {
    Item item = new Item();
    item.id = UUID.randomUUID().toString();
    item.businessId = businessId;
    item.name = s.name;
    item.category = s.category;
    item.unit = s.unit;
    item.masterId = s.masterId;
    item.inDraft = true;
    return item;
  }
}
