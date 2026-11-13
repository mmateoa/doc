package mx.chambia.app;

import static org.junit.Assert.*;

import mx.chambia.app.data.*;
import org.junit.Test;

public class PriceLabelTest {
  @Test
  public void seedHasNoInventedPrice() {
    Item item = new Item();
    assertEquals("Precio por confirmar", PriceLabel.of(item));
  }

  @Test
  public void roundsNarrationWithoutHundredCentavos() {
    Item item = new Item();
    item.priceMode = "fijo";
    item.price = 1.999;
    assertEquals("2 pesos", PriceLabel.spoken(item));
  }

  @Test
  public void stockStatusOverridesPrice() {
    Item item = new Item();
    item.state = "agotado";
    item.price = 400;
    assertEquals("Agotado", PriceLabel.of(item));
  }

  @Test
  public void quoteAndHiddenPriceDoNotLeakAmount() {
    Item item = new Item();
    item.price = 900;
    item.priceMode = "cotizar";
    assertEquals("A cotizar", PriceLabel.spoken(item));
    item.priceMode = "oculto";
    assertEquals("", PriceLabel.of(item));
  }

  @Test
  public void unitAndStartingPriceArePreserved() {
    Item item = new Item();
    item.priceMode = "desde";
    item.price = 1.5;
    item.unit = "kg";
    assertEquals("desde 1 peso con 50 centavos por kg", PriceLabel.spoken(item));
  }
}
