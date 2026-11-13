package mx.chambia.app.data;

import java.util.ArrayList;
import java.util.List;

/** Validated editable variants and package choices, shared by all output formats. */
public final class ItemOptions {
  private ItemOptions() {}

  public static final class Variant {
    public final String name;
    public final double price;

    Variant(String name, double price) {
      this.name = name;
      this.price = price;
    }
  }

  public static List<Variant> variants(String text) {
    List<Variant> result = new ArrayList<>();
    for (String line : text.split("\\r?\\n")) {
      if (line.trim().isEmpty()) continue;
      int separator = line.lastIndexOf('=');
      if (separator < 1)
        throw new IllegalArgumentException("Cada variante debe indicar Nombre = precio");
      String name = line.substring(0, separator).trim();
      double price;
      try {
        price = Double.parseDouble(line.substring(separator + 1).trim());
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Precio de variante no válido");
      }
      if (name.isEmpty() || !Double.isFinite(price) || price < 0 || price > 999999)
        throw new IllegalArgumentException("Variante fuera de rango");
      for (Variant previous : result)
        if (previous.name.equalsIgnoreCase(name))
          throw new IllegalArgumentException("Variante repetida: " + name);
      result.add(new Variant(name, price));
    }
    if (result.size() > 30) throw new IllegalArgumentException("Máximo 30 variantes por artículo");
    return result;
  }

  public static String details(Item item, boolean spoken) {
    StringBuilder result = new StringBuilder(item.description);
    if (!item.bundleContents.trim().isEmpty())
      result
          .append(result.length() == 0 ? "" : "\n")
          .append("Incluye: ")
          .append(item.bundleContents.trim());
    for (Variant variant : variants(item.variants)) {
      Item option = new Item();
      option.price = variant.price;
      option.priceMode = "fijo";
      option.unit = item.unit;
      result.append(result.length() == 0 ? "" : "\n").append(variant.name);
      if (!"oculto".equals(item.priceMode) && !"agotado".equals(item.state))
        result.append(": ").append(spoken ? PriceLabel.spoken(option) : PriceLabel.of(option));
    }
    return result.toString();
  }
}
