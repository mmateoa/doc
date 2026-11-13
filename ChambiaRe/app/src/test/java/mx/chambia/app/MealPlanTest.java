package mx.chambia.app;

import static org.junit.Assert.*;

import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.publication.MealPlan;
import org.junit.Test;

public class MealPlanTest {
  private Item item(String id) {
    Item i = new Item();
    i.id = id;
    i.name = id;
    i.businessId = "fonda";
    return i;
  }

  @Test
  public void aLaCarteCanCoexistWithPackage() {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.priceCents = 11000;
    MealPlan.Section extra = p.addSection("A la carta");
    Item dish = item("especial");
    dish.priceMode = "desde";
    dish.price = 145;
    p.assign(extra, dish.id, true);
    MealPlan restored = MealPlan.decode(p.encode());
    assertEquals(7, restored.sections.size());
    Item rendered = restored.project(Arrays.asList(dish)).get(0);
    assertTrue(rendered.category.startsWith("A la carta"));
    assertEquals("desde", rendered.priceMode);
    assertTrue(rendered.category.contains("precio propio"));
  }

  @Test
  public void oldDraftDoesNotBecomeMealAutomatically() {
    assertFalse(MealPlan.decode("{}").enabled);
  }

  @Test
  public void aggregateRoundTripRetainsCommercialRules() {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.priceCents = 11000;
    p.sections.get(0).ids.add("sopa");
    p.sections.get(0).choices = 2;
    p.sections.get(5).policy = "adicional";
    p.notes = "Tortillas incluidas";
    MealPlan q = MealPlan.decode(p.encode());
    assertEquals(11000, q.priceCents);
    assertEquals(2, q.sections.get(0).choices);
    assertEquals("sopa", q.sections.get(0).ids.get(0));
    assertEquals(p.notes, q.notes);
  }

  @Test
  public void packageHidesIndividualUnconfirmedPricesButExecutiveKeepsOwnPrice() {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.priceCents = 11000;
    Item soup = item("sopa"), executive = item("milanesa");
    executive.priceMode = "fijo";
    executive.price = 145;
    p.assign(p.sections.get(0), soup.id, true);
    p.sections.get(3).enabled = true;
    p.assign(p.sections.get(3), executive.id, true);
    List<Item> rendered = p.project(Arrays.asList(soup, executive));
    assertEquals("oculto", rendered.get(0).priceMode);
    assertEquals(145, rendered.get(1).price, 0);
    assertEquals("confirmar", soup.priceMode);
  }

  @Test
  public void stockAndAssignmentsAreValidatedWithoutChangingCatalogue() {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.priceCents = 11000;
    Item i = item("pollo");
    i.state = "agotado";
    p.assign(p.sections.get(2), i.id, true);
    assertFalse(p.validate(Arrays.asList(i)).isEmpty());
    assertEquals("agotado", p.project(Arrays.asList(i)).get(0).state);
    p.assign(p.sections.get(0), i.id, true);
    assertTrue(p.sections.get(2).ids.isEmpty());
  }

  @Test
  public void explicitNoPriceAndOptionalSectionsAllowIncompleteDraft() {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.withoutPrices = true;
    for (MealPlan.Section s : p.sections) s.required = false;
    assertTrue(p.validate(Collections.emptyList()).isEmpty());
    assertEquals("Comida corrida", p.heading());
  }

  @Test
  public void exactCentsAcceptComma() {
    assertEquals(11050, MealPlan.parsePrice("110,50"));
    assertEquals(-1, MealPlan.parsePrice(""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsNegative() {
    MealPlan.parsePrice("-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsNan() {
    MealPlan.parsePrice("NaN");
  }
}
