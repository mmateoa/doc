package mx.chambia.app;

import java.util.ArrayList;
import java.util.List;
import mx.chambia.app.data.Business;
import mx.chambia.app.data.Item;
import org.json.JSONArray;
import org.json.JSONObject;

/** Copies mutable editor state before dispatching background work. */
public final class PublicationSnapshot {
  private PublicationSnapshot() {}

  public static Business copyBusiness(Business source) {
    Business b = new Business();
    b.id = source.id;
    b.name = source.name;
    b.sector = source.sector;
    b.modality = source.modality;
    b.phone = source.phone;
    b.slogan = source.slogan;
    b.templateId = source.templateId;
    b.archived = source.archived;
    b.mealJson = source.mealJson;
    b.profileJson = source.profileJson;
    b.publicationHeading = source.publicationHeading;
    b.publicationSession = source.publicationSession;
    return b;
  }

  public static List<Item> copyItems(List<Item> source) {
    List<Item> result = new ArrayList<>();
    for (Item i : source) {
      Item c = new Item();
      c.id = i.id;
      c.businessId = i.businessId;
      c.name = i.name;
      c.category = i.category;
      c.unit = i.unit;
      c.description = i.description;
      c.price = i.price;
      c.priceMode = i.priceMode;
      c.state = i.state;
      c.inDraft = i.inDraft;
      c.masterId = i.masterId;
      c.variants = i.variants;
      c.bundleContents = i.bundleContents;
      result.add(c);
    }
    return result;
  }

  public static List<Item> decode(String json, String businessId) throws Exception {
    JSONArray array = new JSONArray(json);
    List<Item> result = new ArrayList<>();
    for (int n = 0; n < array.length(); n++) {
      JSONObject o = array.getJSONObject(n);
      Item item = new Item();
      item.id = o.optString("id", java.util.UUID.randomUUID().toString());
      item.businessId = businessId;
      item.name = o.getString("name");
      item.category = o.optString("category", "General");
      item.unit = o.optString("unit", "");
      item.description = o.optString("description", "");
      item.masterId = o.optString("masterId", "");
      item.price = o.optDouble("price", 0);
      item.priceMode = o.optString("priceMode", "confirmar");
      item.state = o.optString("state", "disponible");
      item.variants = o.optString("variants", "");
      item.bundleContents = o.optString("bundleContents", "");
      result.add(item);
    }
    return result;
  }

  public static String encode(List<Item> items) throws Exception {
    JSONArray array = new JSONArray();
    for (Item i : items) {
      JSONObject o = new JSONObject();
      o.put("id", i.id);
      o.put("name", i.name);
      o.put("category", i.category);
      o.put("unit", i.unit);
      o.put("description", i.description);
      o.put("masterId", i.masterId);
      o.put("price", i.price);
      o.put("priceMode", i.priceMode);
      o.put("state", i.state);
      o.put("variants", i.variants);
      o.put("bundleContents", i.bundleContents);
      array.put(o);
    }
    return array.toString();
  }
}
