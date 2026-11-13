package mx.chambia.app;

import static org.junit.Assert.*;

import java.util.*;
import mx.chambia.app.data.*;
import org.junit.Test;

public class PublicationSnapshotTest {
  @Test
  public void exportDoesNotObserveLaterPriceEdits() {
    Item item = new Item();
    item.businessId = "fonda";
    item.price = 90;
    item.unit = "orden";
    List<Item> captured = PublicationSnapshot.copyItems(Arrays.asList(item));
    item.price = 200;
    item.businessId = "taller";
    assertEquals(90, captured.get(0).price, 0);
    assertEquals("fonda", captured.get(0).businessId);
    assertEquals("orden", captured.get(0).unit);
  }

  @Test
  public void exportDoesNotObserveBusinessSwitchOrRenaming() {
    Business business = new Business();
    business.id = "first";
    business.name = "Fondita";
    Business copy = PublicationSnapshot.copyBusiness(business);
    business.name = "Taller";
    business.templateId = "elegante";
    assertEquals("Fondita", copy.name);
    assertEquals("first", copy.id);
    assertEquals("mexicano", copy.templateId);
  }
}
