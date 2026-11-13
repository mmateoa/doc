package mx.chambia.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.design.TemplateEngine;
import mx.chambia.app.export.ExportManager;
import mx.chambia.app.publication.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AndroidRenderingTest {
  private Context context() {
    return RuntimeEnvironment.getApplication();
  }

  private Business business() {
    Business b = new Business();
    b.id = "rosita";
    b.name = "Rosita Sazón";
    b.phone = "55 1234 5678";
    b.slogan = "Comida hecha en casa";
    return b;
  }

  private List<Item> items(Business b) {
    MealPlan p = MealPlan.defaults();
    p.enabled = true;
    p.priceCents = 11000;
    p.notes = "Tortillas incluidas · Hasta agotar existencias";
    String[][] groups = {
      {"Sopa de fideo", "Consomé de pollo"},
      {"Arroz rojo", "Espagueti"},
      {"Pollo en mole", "Enchiladas verdes", "Chicharrón en salsa roja"},
      {"Milanesa con guarnición"},
      {"Agua de jamaica"},
      {"Gelatina"}
    };
    List<Item> all = new ArrayList<>();
    for (int n = 0; n < groups.length; n++) {
      p.sections.get(n).enabled = true;
      for (String name : groups[n]) {
        Item i = new Item();
        i.id = "item-" + all.size();
        i.businessId = b.id;
        i.name = name;
        if (n == 3) {
          i.price = 145;
          i.priceMode = "fijo";
        }
        p.assign(p.sections.get(n), i.id, true);
        all.add(i);
      }
    }
    b.mealJson = p.encode();
    return all;
  }

  @Test
  public void mexicanMealRendersRealJpgPngAndText() throws Exception {
    Business b = business();
    List<Item> items = items(b);
    assertTrue(MealPlan.decode(b.mealJson).validate(items).isEmpty());
    String text = ExportManager.readable(b, items);
    assertTrue(text.contains("110"));
    assertTrue(text.contains("145"));
    assertFalse(text.contains("Precio por confirmar"));
    assertTrue(ExportManager.narration(b, items).contains("110 pesos"));
    File evidence = new File(System.getProperty("evidenceDir"));
    evidence.mkdirs();
    for (String destination : new String[] {"TV", "REDES", "IMPRESIÓN"})
      for (String format : new String[] {"JPG", "PNG"}) {
        List<File> files =
            ExportManager.generate(context(), b, items, "mexicano", format, false, destination);
        assertTrue(files.size() >= 2);
        for (File file : files) {
          assertTrue(file.length() > 0);
          if (!file.getName().endsWith(".txt")) {
            java.nio.file.Files.copy(
                file.toPath(),
                new File(evidence, destination + "-" + file.getName()).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (!format.equals("PDF")) {
              Bitmap decoded = BitmapFactory.decodeFile(file.getPath());
              assertNotNull(decoded);
              int[] expected = ExportManager.dimensions(destination);
              assertEquals(expected[0], decoded.getWidth());
              assertEquals(expected[1], decoded.getHeight());
              decoded.recycle();
            } else {
              byte[] header = java.nio.file.Files.readAllBytes(file.toPath());
              assertEquals(
                  "%PDF", new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
            }
          }
        }
      }
  }

  @Test
  public void allItemsSurvivePagingInEveryFormatAndTemplate() throws Exception {
    Business b = business();
    for (int count : new int[] {0, 1, 5, 20, 40}) {
      List<Item> items = new ArrayList<>();
      for (int n = 0; n < count; n++) {
        Item i = new Item();
        i.id = "row" + n;
        i.name = "Producto de nombre extenso para comprobar ajuste de texto número " + n;
        i.category = n < 20 ? "Productos" : "Servicios";
        i.priceMode = "desde";
        i.price = 650;
        items.add(i);
      }
      for (String id : new String[] {"mexicano", "minimalista", "elegante"})
        for (String dest : new String[] {"TV", "REDES", "CUADRADO", "IMPRESIÓN"})
          for (boolean high : new boolean[] {false, true}) {
            TemplateEngine engine = new TemplateEngine(context(), id);
            int[] size = ExportManager.dimensions(dest);
            List<List<Item>> pages = engine.pages(b, items, size[0], size[1], high);
            int total = 0;
            for (List<Item> page : pages) {
              total += page.size();
              Bitmap image = engine.bitmap(b, page, size[0], size[1], high);
              image.recycle();
            }
            assertEquals(count, total);
          }
    }
  }

  @Test
  public void migrationsSevenAndEightToNinePreserveRowsAndFiles() throws Exception {
    java.util.concurrent.CompletableFuture.runAsync(
            () -> {
              for (int legacyVersion : new int[] {7, 8}) {
                String name = "migration-" + UUID.randomUUID() + ".db";
                try {
                  File path = context().getDatabasePath(name);
                  path.getParentFile().mkdirs();
                  org.json.JSONObject schema =
                      new org.json.JSONObject(
                              new String(
                                  getClass()
                                      .getClassLoader()
                                      .getResourceAsStream("schema" + legacyVersion + ".json")
                                      .readAllBytes(),
                                  java.nio.charset.StandardCharsets.UTF_8))
                          .getJSONObject("database");
                  try (android.database.sqlite.SQLiteDatabase old =
                      android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null)) {
                    org.json.JSONArray entities = schema.getJSONArray("entities");
                    for (int n = 0; n < entities.length(); n++) {
                      org.json.JSONObject entity = entities.getJSONObject(n);
                      old.execSQL(
                          entity
                              .getString("createSql")
                              .replace("${TABLE_NAME}", entity.getString("tableName")));
                      org.json.JSONArray indexes = entity.getJSONArray("indices");
                      for (int q = 0; q < indexes.length(); q++)
                        old.execSQL(
                            indexes
                                .getJSONObject(q)
                                .getString("createSql")
                                .replace("${TABLE_NAME}", entity.getString("tableName")));
                    }
                    old.execSQL(
                        "INSERT INTO businesses"
                            + " (id,name,sector,modality,phone,slogan,templateId,archived,profileJson)"
                            + " VALUES"
                            + " ('legacy','Rosita','fonda','Local','','','mexicano',0,'{}')");
                    old.execSQL(
                        "INSERT INTO drafts"
                            + " (id,businessId,title,mode,session,snapshotJson,updatedAt) VALUES"
                            + " ('draft','legacy','Comida','Oferta del día','', '[]',123)");
                    old.execSQL(
                        "INSERT INTO publications"
                            + " (id,businessId,title,date,snapshotJson,templateId,note,createdAt,exportPathsJson,businessJson,destination,highContrast)"
                            + " VALUES"
                            + " ('pub','legacy','Anterior','2026-11-11','[]','mexicano','contenido',123,'[]','{}','TV',0)");
                    old.setVersion(legacyVersion);
                  }
                  File preserved = new File(context().getFilesDir(), "previous-export.jpg");
                  java.nio.file.Files.write(preserved.toPath(), new byte[] {1, 2, 3});
                  AppDatabase db = AppDatabase.create(context(), name);
                  try {
                    assertEquals(1, db.dao().businesses().size());
                    assertEquals("{}", db.dao().draft("legacy", "draft").mealJson);
                    assertEquals("contenido", db.dao().publication("pub").note);
                    Business b = db.dao().businesses().get(0);
                    Draft draft = db.dao().draft(b.id, "draft");
                    MealPlan meal = MealPlan.defaults();
                    meal.enabled = true;
                    meal.priceCents = 11000;
                    draft.mealJson = meal.encode();
                    db.dao().saveDraft(draft);
                  } finally {
                    db.close();
                  }
                  db = AppDatabase.create(context(), name);
                  try {
                    assertEquals(
                        11000,
                        MealPlan.decode(db.dao().draft("legacy", "draft").mealJson).priceCents);
                    assertArrayEquals(
                        new byte[] {1, 2, 3}, java.nio.file.Files.readAllBytes(preserved.toPath()));
                    assertNull(db.dao().draft("other", "draft"));
                  } finally {
                    db.close();
                    context().deleteDatabase(name);
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }
            })
        .get();
  }
}
