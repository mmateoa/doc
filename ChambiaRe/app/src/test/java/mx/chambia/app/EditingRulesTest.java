package mx.chambia.app;

import static org.junit.Assert.*;

import java.util.*;
import mx.chambia.app.accessibility.SpeechSegments;
import mx.chambia.app.data.*;
import mx.chambia.app.publication.DraftEditor;
import org.junit.Test;

public class EditingRulesTest {
  private Item item(String id) {
    Item item = new Item();
    item.id = id;
    item.businessId = "fonda";
    item.priceMode = "fijo";
    item.price = 80;
    return item;
  }

  @Test
  public void catalogueAndOfferAreIndependent() {
    Item source = item("one");
    DraftEditor offer = new DraftEditor("fonda", Arrays.asList(source));
    source.price = 200;
    assertEquals(80, offer.entries().get(0).price, 0);
    Item edited = offer.entries().get(0);
    edited.price = 90;
    offer.update(edited);
    assertEquals(200, source.price, 0);
    List<Item> leaked = offer.entries();
    leaked.get(0).price = 999;
    assertEquals(90, offer.entries().get(0).price, 0);
  }

  @Test
  public void orderVisibilityAndDeduplication() {
    Item a = item("a"), b = item("b");
    DraftEditor offer = new DraftEditor("fonda", Arrays.asList(a, b));
    offer.include(a);
    assertEquals(2, offer.entries().size());
    offer.move("b", -1);
    assertEquals("b", offer.entries().get(0).id);
    b.state = "fuera";
    offer.update(b);
    assertEquals("a", offer.visible().get(0).id);
    offer.remove("a");
    assertTrue(offer.visible().isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void refusesForeignBusiness() {
    Item foreign = item("x");
    foreign.businessId = "other";
    new DraftEditor("fonda", Arrays.asList(foreign));
  }

  @Test
  public void variantsAndPackageAreCopied() {
    Item pizza = item("pizza");
    pizza.variants = "Chica = 80\nGrande = 130";
    pizza.bundleContents = "Una bebida y un complemento";
    Item snapshot = PublicationSnapshot.copyItems(Arrays.asList(pizza)).get(0);
    pizza.variants = "";
    assertEquals(2, ItemOptions.variants(snapshot.variants).size());
    assertTrue(ItemOptions.details(snapshot, true).contains("130 pesos"));
    assertTrue(ItemOptions.details(snapshot, false).contains("Una bebida"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidVariantPriceIsRejected() {
    ItemOptions.variants("Grande = NaN");
  }

  @Test
  public void hiddenPricesAlsoHideVariantAmounts() {
    Item pizza = item("pizza");
    pizza.variants = "Grande = 130";
    pizza.priceMode = "oculto";
    assertEquals("Grande", ItemOptions.details(pizza, false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateVariantIsRejected() {
    ItemOptions.variants("Grande = 80\ngrande = 90");
  }

  @Test
  public void speechSplittingPreservesEveryCharacter() {
    String input = "Tacos, café y pizza. ".repeat(500) + "🍕";
    List<String> segments = SpeechSegments.split(input, 97);
    assertTrue(segments.size() > 1);
    assertEquals(input, String.join("", segments));
    for (String segment : segments) {
      assertTrue(segment.length() <= 97);
      assertFalse(Character.isHighSurrogate(segment.charAt(segment.length() - 1)));
    }
  }
}
