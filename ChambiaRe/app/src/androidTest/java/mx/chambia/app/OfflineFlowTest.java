package mx.chambia.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.design.TemplateEngine;
import mx.chambia.app.export.ExportManager;
import org.junit.Test;

public class OfflineFlowTest {
  private Context context() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  @Test
  public void businessDraftsSurviveReopenAndRemainIsolated() throws Exception {
    String name = "test-" + UUID.randomUUID() + ".db";
    AppDatabase db = AppDatabase.create(context(), name);
    try {
      Business first = new Business();
      first.id = "fonda";
      first.name = "Fondita";
      db.dao().saveBusiness(first);
      Business second = new Business();
      second.id = "taller";
      second.name = "Taller";
      db.dao().saveBusiness(second);
      List<SeedCatalog.Suggestion> suggestions = SeedCatalog.load(context(), "fonda");
      assertTrue(suggestions.size() >= 6);
      for (int n = 0; n < 6; n++) {
        Item item = SeedCatalog.toItem(suggestions.get(n), first.id);
        item.price = 60 + n;
        item.priceMode = "fijo";
        db.dao().saveItem(item);
      }
      Draft morning = new Draft();
      morning.id = "morning";
      morning.businessId = first.id;
      morning.title = "Desayuno";
      morning.snapshotJson = PublicationSnapshot.encode(db.dao().items(first.id));
      db.dao().saveDraft(morning);
      Draft evening = new Draft();
      evening.id = "evening";
      evening.businessId = first.id;
      evening.title = "Cena";
      db.dao().saveDraft(evening);
      assertEquals(0, db.dao().items(second.id).size());
      assertNull(db.dao().draft(second.id, morning.id));
      db.close();
      db = AppDatabase.create(context(), name);
      assertEquals(6, db.dao().items(first.id).size());
      assertEquals(2, db.dao().drafts(first.id).size());
      List<Item> offer =
          PublicationSnapshot.decode(db.dao().draft(first.id, morning.id).snapshotJson, first.id);
      double original = offer.get(0).price;
      Item changed = db.dao().items(first.id).get(0);
      changed.price = 999;
      db.dao().saveItem(changed);
      assertEquals(
          original,
          PublicationSnapshot.decode(db.dao().draft(first.id, morning.id).snapshotJson, first.id)
              .get(0)
              .price,
          0);
      assertTrue(
          PublicationSnapshot.decode(db.dao().draft(first.id, evening.id).snapshotJson, first.id)
              .isEmpty());
    } finally {
      db.close();
      context().deleteDatabase(name);
    }
  }

  @Test
  public void migratesLegacyVersionOneWithoutLosingPreparation() throws Exception {
    String name = "migration-" + UUID.randomUUID() + ".db";
    File path = context().getDatabasePath(name);
    path.getParentFile().mkdirs();
    try (android.database.sqlite.SQLiteDatabase legacy =
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null)) {
      legacy.execSQL(
          "CREATE TABLE businesses (id TEXT NOT NULL PRIMARY KEY,name TEXT,sector TEXT,modality"
              + " TEXT,phone TEXT,slogan TEXT,templateId TEXT,archived INTEGER NOT NULL)");
      legacy.execSQL(
          "CREATE TABLE items (id TEXT NOT NULL PRIMARY KEY,businessId TEXT,name TEXT,category"
              + " TEXT,unit TEXT,description TEXT,price REAL NOT NULL,priceMode TEXT,state"
              + " TEXT,inDraft INTEGER NOT NULL,masterId TEXT)");
      legacy.execSQL("CREATE INDEX index_items_businessId ON items (businessId)");
      legacy.execSQL(
          "CREATE TABLE publications (id TEXT NOT NULL PRIMARY KEY,businessId TEXT,title TEXT,date"
              + " TEXT,snapshotJson TEXT,templateId TEXT,note TEXT,createdAt INTEGER NOT NULL)");
      legacy.execSQL("CREATE INDEX index_publications_businessId ON publications (businessId)");
      legacy.execSQL(
          "INSERT INTO businesses VALUES ('legacy','Fondita','fonda','Local','','','mexicano',0)");
      legacy.execSQL(
          "INSERT INTO items VALUES"
              + " ('guisado','legacy','Mole','Guisados','orden','',0,'fijo','disponible',1,'seed_mole')");
      legacy.setVersion(1);
    }
    AppDatabase upgraded = AppDatabase.create(context(), name);
    try {
      assertEquals(1, upgraded.dao().businesses().size());
      assertEquals("confirmar", upgraded.dao().items("legacy").get(0).priceMode);
      Draft draft = upgraded.dao().draft("legacy", "active_legacy");
      assertNotNull(draft);
      assertEquals("Mole", PublicationSnapshot.decode(draft.snapshotJson, "legacy").get(0).name);
      assertEquals("{}", upgraded.dao().businesses().get(0).profileJson);
    } finally {
      upgraded.close();
      context().deleteDatabase(name);
    }
  }

  @Test
  public void variantsPackagesAndBusinessProfilesSurviveSnapshots() throws Exception {
    Item pizza = new Item();
    pizza.id = "pizza";
    pizza.businessId = "pizzeria";
    pizza.name = "Pizza";
    pizza.variants = "Chica = 80\nGrande = 140";
    pizza.bundleContents = "Refresco y papas";
    Item decoded =
        PublicationSnapshot.decode(
                PublicationSnapshot.encode(Arrays.asList(pizza)), pizza.businessId)
            .get(0);
    assertEquals(pizza.variants, decoded.variants);
    assertEquals(pizza.bundleContents, decoded.bundleContents);
    Business business = new Business();
    business.id = pizza.businessId;
    business.profileJson = "{\"address\":\"Zona centro\"}";
    business.publicationSession = "Cena";
    Business copy =
        mx.chambia.app.publication.PublicationStore.business(
            mx.chambia.app.publication.PublicationStore.businessJson(business));
    assertEquals(business.profileJson, copy.profileJson);
    assertEquals("Cena", copy.publicationSession);
    String text = ExportManager.readable(copy, Arrays.asList(decoded));
    assertTrue(text.contains("Grande"));
    assertTrue(text.contains("140"));
    assertTrue(text.contains("Refresco"));
    assertTrue(text.contains("Zona centro"));
  }

  @Test
  public void everyItemIsRenderedAndAllFormatsAreReal() throws Exception {
    Business business = new Business();
    business.id = "test";
    business.name = "Fondita de prueba";
    List<Item> items = new ArrayList<>();
    for (int n = 0; n < 17; n++) {
      Item item = new Item();
      item.name = "Guisado con nombre largo para comprobar distribución y legibilidad " + n;
      item.price = 70 + n;
      item.priceMode = "fijo";
      items.add(item);
    }
    TemplateEngine engine = new TemplateEngine(context(), "mexicano");
    List<List<Item>> pages = engine.pages(business, items, 1920, 1080);
    int total = 0;
    for (List<Item> page : pages) {
      total += page.size();
      Bitmap image = engine.bitmap(business, page, 1920, 1080, false);
      assertEquals(1920, image.getWidth());
      image.recycle();
    }
    assertEquals(17, total);
    assertTrue(pages.size() > 1);
    for (String format : new String[] {"JPG", "PNG", "PDF"}) {
      List<File> files =
          ExportManager.generate(context(), business, items, "mexicano", format, false, "TV");
      try {
        assertEquals("menu.txt", files.get(files.size() - 1).getName());
        for (File file : files) assertTrue(file.length() > 0);
        if (format.equals("PDF"))
          try (ParcelFileDescriptor descriptor =
                  ParcelFileDescriptor.open(files.get(0), ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(descriptor)) {
            assertEquals(pages.size(), renderer.getPageCount());
          }
        else assertEquals(pages.size() + 1, files.size());
      } finally {
        for (File file : files) file.delete();
        files.get(0).getParentFile().delete();
      }
    }
  }
}
