package mx.chambia.app.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class PriceLabel {
  private PriceLabel() {}

  public static String of(Item item) {
    if ("agotado".equals(item.state)) return "Agotado";
    if ("cotizar".equals(item.priceMode)) return "A cotizar";
    if ("oculto".equals(item.priceMode)) return "";
    if ("confirmar".equals(item.priceMode)) return "Precio por confirmar";
    String value = NumberFormat.getCurrencyInstance(new Locale("es", "MX")).format(item.price);
    return ("desde".equals(item.priceMode) ? "Desde " : "")
        + value
        + (item.unit.isEmpty() ? "" : " / " + item.unit);
  }

  public static String spoken(Item item) {
    if ("agotado".equals(item.state)
        || "cotizar".equals(item.priceMode)
        || "confirmar".equals(item.priceMode)
        || "oculto".equals(item.priceMode)) return of(item);
    long cents =
        BigDecimal.valueOf(item.price)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValue();
    long pesos = cents / 100, remainder = cents % 100;
    return ("desde".equals(item.priceMode) ? "desde " : "")
        + pesos
        + (pesos == 1 ? " peso" : " pesos")
        + (remainder == 0 ? "" : " con " + remainder + (remainder == 1 ? " centavo" : " centavos"))
        + (item.unit.isEmpty() ? "" : " por " + item.unit);
  }
}
