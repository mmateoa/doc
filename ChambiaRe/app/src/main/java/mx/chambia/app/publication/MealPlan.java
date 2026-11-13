package mx.chambia.app.publication;

import java.text.NumberFormat;
import java.util.*;
import mx.chambia.app.PublicationSnapshot;
import mx.chambia.app.data.*;
import org.json.*;

/** Versioned commercial aggregate. A draft snapshot and this aggregate are saved atomically. */
public final class MealPlan {
  public boolean enabled = false, withoutPrices = false;
  public long priceCents = -1;
  public String notes = "";
  public final List<Section> sections = new ArrayList<>();

  public static final class Section {
    public String key, label, policy = "incluido";
    public boolean enabled = true, required = true;
    public int choices = 1;
    public final List<String> ids = new ArrayList<>();

    Section(String k, String l) {
      key = k;
      label = l;
    }
  }

  public static MealPlan defaults() {
    MealPlan p = new MealPlan();
    String[] keys = {"entrada", "segundo", "fuerte", "ejecutivo", "bebida", "postre"};
    String[] labels = {
      "Entrada", "Segundo tiempo", "Plato fuerte", "Plato ejecutivo", "Bebida", "Postre"
    };
    for (int n = 0; n < keys.length; n++) {
      Section s = new Section(keys[n], labels[n]);
      s.required = n < 3;
      s.enabled = n != 3 && n != 5;
      s.policy = n == 3 ? "independiente" : "incluido";
      p.sections.add(s);
    }
    return p;
  }

  public static MealPlan decode(String json) {
    MealPlan p = defaults();
    if (json == null || json.equals("{}") || json.isEmpty()) return p;
    try {
      JSONObject o = new JSONObject(json);
      if (o.getInt("version") != 1)
        throw new IllegalArgumentException("Versión de comida corrida incompatible");
      p.enabled = o.getBoolean("enabled");
      p.withoutPrices = o.optBoolean("withoutPrices");
      p.priceCents = o.optLong("priceCents", -1);
      p.notes = o.optString("notes", "");
      JSONArray rows = o.getJSONArray("sections");
      p.sections.clear();
      Set<String> keys = new HashSet<>();
      for (int n = 0; n < rows.length(); n++) {
        JSONObject a = rows.getJSONObject(n);
        Section s = new Section(a.getString("key"), a.getString("label"));
        if (!keys.add(s.key)) throw new IllegalArgumentException("Sección duplicada");
        s.enabled = a.getBoolean("enabled");
        s.required = a.getBoolean("required");
        s.choices = a.getInt("choices");
        s.policy = a.getString("policy");
        if (s.choices < 1
            || s.choices > 20
            || !Arrays.asList("incluido", "independiente", "adicional", "no_incluido")
                .contains(s.policy))
          throw new IllegalArgumentException("Reglas de sección no válidas");
        JSONArray ids = a.getJSONArray("ids");
        for (int q = 0; q < ids.length(); q++)
          if (!s.ids.contains(ids.getString(q))) s.ids.add(ids.getString(q));
        p.sections.add(s);
      }
      if (p.priceCents < -1 || p.priceCents > 99999900)
        throw new IllegalArgumentException("Revisa el precio");
      return p;
    } catch (JSONException ex) {
      throw new IllegalArgumentException("No se pudo recuperar la comida corrida", ex);
    }
  }

  public Section addSection(String label) {
    if (label == null || label.trim().isEmpty())
      throw new IllegalArgumentException("Nombre de sección requerido");
    Section s = new Section("custom_" + java.util.UUID.randomUUID(), label.trim());
    s.required = false;
    s.policy = "independiente";
    sections.add(s);
    return s;
  }

  public String encode() {
    try {
      JSONObject o = new JSONObject();
      o.put("version", 1)
          .put("enabled", enabled)
          .put("withoutPrices", withoutPrices)
          .put("priceCents", priceCents)
          .put("notes", notes);
      JSONArray rows = new JSONArray();
      for (Section s : sections) {
        JSONObject a = new JSONObject();
        a.put("key", s.key)
            .put("label", s.label)
            .put("enabled", s.enabled)
            .put("required", s.required)
            .put("choices", s.choices)
            .put("policy", s.policy)
            .put("ids", new JSONArray(s.ids));
        rows.put(a);
      }
      return o.put("sections", rows).toString();
    } catch (JSONException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static long parsePrice(String raw) {
    if (raw.trim().isEmpty()) return -1;
    try {
      java.math.BigDecimal value = new java.math.BigDecimal(raw.trim().replace(',', '.'));
      if (value.signum() < 0
          || value.compareTo(new java.math.BigDecimal("999999")) > 0
          || value.scale() > 2) throw new IllegalArgumentException("Revisa el precio");
      return value.movePointRight(2).longValueExact();
    } catch (ArithmeticException | NumberFormatException ex) {
      throw new IllegalArgumentException("Revisa el precio", ex);
    }
  }

  public String heading() {
    return "Comida corrida" + (withoutPrices || priceCents < 0 ? "" : " · " + money(priceCents));
  }

  public static String money(long cents) {
    return NumberFormat.getCurrencyInstance(new Locale("es", "MX")).format(cents / 100d);
  }

  public List<String> validate(List<Item> items) {
    List<String> errors = new ArrayList<>();
    if (!enabled) return errors;
    if (!withoutPrices && priceCents < 0)
      errors.add("Falta definir el precio de la comida corrida");
    Set<String> assigned = new HashSet<>();
    for (Section s : sections)
      if (s.enabled) {
        int available = 0;
        for (Item i : items)
          if (s.ids.contains(i.id) && !i.state.equals("fuera")) {
            assigned.add(i.id);
            if (!i.state.equals("agotado")) available++;
            if ((s.policy.equals("adicional") || s.policy.equals("independiente"))
                && !withoutPrices
                && i.priceMode.equals("confirmar"))
              errors.add(s.label + ": define el precio de " + i.name);
          }
        if (s.required && available == 0)
          errors.add(s.label + ": agrega al menos una opción disponible");
      }
    for (Item i : items)
      if (!i.state.equals("fuera") && !assigned.contains(i.id))
        errors.add("Asigna una sección a " + i.name);
    return errors;
  }

  public List<Item> project(List<Item> items) {
    if (!enabled) return PublicationSnapshot.copyItems(items);
    List<Item> result = new ArrayList<>();
    Set<String> assigned = new HashSet<>();
    for (Section s : sections)
      if (s.enabled && !s.policy.equals("no_incluido")) {
        String category =
            s.label
                + " · "
                + (s.policy.equals("adicional")
                    ? "suplemento"
                    : s.policy.equals("independiente") ? "precio propio" : "incluido")
                + " · elige "
                + s.choices;
        for (Item source : items)
          if (s.ids.contains(source.id) && !source.state.equals("fuera")) {
            Item i = PublicationSnapshot.copyItems(Collections.singletonList(source)).get(0);
            i.id = "mealview:" + source.id;
            i.category = category;
            if (s.policy.equals("incluido") || withoutPrices) i.priceMode = "oculto";
            result.add(i);
            assigned.add(source.id);
          }
      }
    return result;
  }

  public static List<Item> content(Business b, List<Item> items) {
    if (!items.isEmpty() && items.get(0).id.startsWith("mealview:")) return items;
    return decode(b.mealJson).project(items);
  }

  public void assign(Section target, String itemId, boolean selected) {
    for (Section s : sections) s.ids.remove(itemId);
    if (selected) target.ids.add(itemId);
  }
}
