package mx.chambia.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.EditText;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import mx.chambia.app.data.*;
import mx.chambia.app.export.ExportManager;
import mx.chambia.app.publication.*;
import org.junit.Test;

/** Real framework/SQLite/PDF checks; never replace a USB or TTS test with a fake success. */
public class RedesignFlowTest {
  private Context context() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  private List<Item> meal(Business business) {
    MealPlan meal = MealPlan.defaults();
    meal.enabled = true;
    meal.priceCents = 11000;
    meal.notes = "Tortillas incluidas · Hasta agotar existencias";
    String[][] names = {
      {"Sopa de fideo", "Consomé de pollo"},
      {"Arroz rojo", "Espagueti"},
      {"Pollo en mole", "Enchiladas verdes", "Chicharrón en salsa roja"},
      {"Milanesa con guarnición"},
      {"Agua de jamaica"},
      {"Gelatina"}
    };
    List<Item> all = new ArrayList<>();
    for (int n = 0; n < names.length; n++) {
      meal.sections.get(n).enabled = true;
      for (String name : names[n]) {
        Item i = new Item();
        i.id = "dish-" + all.size();
        i.name = name;
        i.businessId = business.id;
        i.category = meal.sections.get(n).label;
        if (n == 3) {
          i.price = 145;
          i.priceMode = "fijo";
        }
        meal.assign(meal.sections.get(n), i.id, true);
        all.add(i);
      }
    }
    business.mealJson = meal.encode();
    return all;
  }

  @Test
  public void structuredMealProducesReadableMultipagePdfWithoutChangingHistory() throws Exception {
    Business business = new Business();
    business.id = "test-meal";
    business.name = "Rosita Sazón";
    List<Item> all = meal(business);
    assertTrue(MealPlan.decode(business.mealJson).validate(all).isEmpty());
    String text = ExportManager.readable(business, all);
    assertTrue(text.contains("110"));
    assertTrue(text.contains("145"));
    assertFalse(text.contains("Precio por confirmar"));
    for (String destination :
        new String[] {"TV", "REDES", "CUADRADO", "IMPRESIÓN", "A4", "OFICIO"}) {
      List<File> files =
          ExportManager.generate(context(), business, all, "mexicano", "PDF", false, destination);
      try (ParcelFileDescriptor fd =
              ParcelFileDescriptor.open(files.get(0), ParcelFileDescriptor.MODE_READ_ONLY);
          PdfRenderer pdf = new PdfRenderer(fd)) {
        assertTrue(pdf.getPageCount() > 0);
        for (int n = 0; n < pdf.getPageCount(); n++)
          try (PdfRenderer.Page page = pdf.openPage(n)) {
            assertTrue(page.getWidth() > 0);
          }
      }
      File evidence = new File(context().getFilesDir(), "ux-evidence");
      evidence.mkdirs();
      java.nio.file.Files.copy(
          files.get(0).toPath(),
          new File(evidence, destination + ".pdf").toPath(),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      java.nio.file.Files.write(
          new File(evidence, "menu.txt").toPath(),
          text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      byte[] before = java.nio.file.Files.readAllBytes(files.get(0).toPath());
      all.get(0).price = 999;
      business.name = "Otro nombre";
      assertArrayEquals(before, java.nio.file.Files.readAllBytes(files.get(0).toPath()));
    }
  }

  @Test
  public void autosaveSurvivesActivityReopenAndBusinessSwitch() throws Exception {
    ChambiaRepository repo = ChambiaRepository.get(context());
    Business business = new Business();
    business.id = "ui-rosita";
    business.name = "Rosita Sazón";
    business.phone = "55 1234 5678";
    business.slogan = "Comida hecha en casa";
    List<Item> all = meal(business);
    Draft draft = new Draft();
    draft.id = "ui-draft";
    draft.businessId = business.id;
    draft.title = "Comida de hoy";
    draft.mealJson = business.mealJson;
    draft.snapshotJson = PublicationSnapshot.encode(all);
    Business workshop = new Business();
    workshop.id = "ui-taller";
    workshop.name = "Taller González";
    workshop.sector = "taller";
    repo.io
        .submit(
            () -> {
              repo.database.dao().saveBusiness(business);
              repo.database.dao().saveBusiness(workshop);
              for (Item i : all) repo.database.dao().saveItem(i);
              repo.database.dao().saveDraft(draft);
            })
        .get();
    context()
        .getSharedPreferences("workspace", 0)
        .edit()
        .putString("lastBusiness", business.id)
        .putString("draft_" + business.id, draft.id)
        .commit();
    android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Activity activity =
        instrumentation.startActivitySync(
            new Intent(context(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      View prepare = await(activity, R.id.prepare);
      instrumentation.runOnMainSync(prepare::performClick);
      EditText price = (EditText) await(activity, R.id.price);
      instrumentation.runOnMainSync(
          () -> {
            price.setText("120");
            assertEquals(View.GONE, activity.findViewById(R.id.navigation).getVisibility());
          });
      repo.io.submit(() -> {}).get();
      assertEquals(
          12000,
          repo.io
              .submit(
                  () ->
                      MealPlan.decode(repo.database.dao().draft(business.id, draft.id).mealJson)
                          .priceCents)
              .get()
              .longValue());
      instrumentation.runOnMainSync(activity::finish);
    } finally {
      if (!activity.isFinishing()) instrumentation.runOnMainSync(activity::finish);
    }
    Activity reopened =
        instrumentation.startActivitySync(
            new Intent(context(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      await(reopened, R.id.prepare);
      instrumentation.runOnMainSync(
          () ->
              ((com.google.android.material.bottomnavigation.BottomNavigationView)
                      reopened.findViewById(R.id.navigation))
                  .setSelectedItemId(R.id.catalog));
      await(reopened, R.id.search);
      assertEquals(
          all.size(),
          repo.io
              .submit(
                  () ->
                      PublicationSnapshot.decode(
                              repo.database.dao().draft(business.id, draft.id).snapshotJson,
                              business.id)
                          .size())
              .get()
              .intValue());
      context()
          .getSharedPreferences("workspace", 0)
          .edit()
          .putString("lastBusiness", workshop.id)
          .commit();
      instrumentation.runOnMainSync(reopened::recreate);
      awaitCurrentHome();
      assertEquals(
          0, repo.io.submit(() -> repo.database.dao().items(workshop.id).size()).get().intValue());
      assertEquals(
          12000,
          repo.io
              .submit(
                  () ->
                      MealPlan.decode(repo.database.dao().draft(business.id, draft.id).mealJson)
                          .priceCents)
              .get()
              .longValue());
    } finally {
      instrumentation.runOnMainSync(
          () -> {
            if (!reopened.isFinishing()) reopened.finish();
          });
    }
    // Leave the fixture available for screenshots, without claiming it is user content.
    context()
        .getSharedPreferences("workspace", 0)
        .edit()
        .putString("lastBusiness", business.id)
        .commit();
  }

  @Test
  public void previewPageSurvivesRotation() throws Exception {
    ChambiaRepository repo = ChambiaRepository.get(context());
    Business b = new Business();
    b.id = "rotation";
    b.name = "Rotación";
    b.sector = "taller";
    List<Item> all = new ArrayList<>();
    for (int n = 0; n < 30; n++) {
      Item i = new Item();
      i.businessId = b.id;
      i.id = "r" + n;
      i.name = "Servicio " + n;
      i.priceMode = "cotizar";
      all.add(i);
    }
    Draft d = new Draft();
    d.id = "rotation-draft";
    d.businessId = b.id;
    d.snapshotJson = PublicationSnapshot.encode(all);
    repo.io
        .submit(
            () -> {
              repo.database.dao().saveBusiness(b);
              repo.database.dao().saveDraft(d);
            })
        .get();
    context()
        .getSharedPreferences("workspace", 0)
        .edit()
        .putString("lastBusiness", b.id)
        .putString("draft_" + b.id, d.id)
        .commit();
    android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    Activity first =
        ins.startActivitySync(
            new Intent(context(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    View preview = await(first, R.id.preview);
    ins.runOnMainSync(preview::performClick);
    View next = await(first, R.id.next);
    repo.io.submit(() -> {}).get();
    ins.waitForIdleSync();
    ins.runOnMainSync(() -> first.findViewById(R.id.next).performClick());
    AtomicReference<String> before = new AtomicReference<>();
    ins.runOnMainSync(
        () ->
            before.set(
                ((android.widget.TextView) first.findViewById(R.id.page)).getText().toString()));
    assertTrue("Before rotation: " + before.get(), before.get().startsWith("2 "));
    ins.runOnMainSync(first::recreate);
    AtomicReference<Activity> current = new AtomicReference<>();
    for (int n = 0; n < 300; n++) {
      ins.runOnMainSync(
          () -> {
            for (Activity a :
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
              if (a instanceof MainActivity && a != first) current.set(a);
          });
      if (current.get() != null) break;
      Thread.sleep(100);
    }
    assertNotNull(current.get());
    Activity restored = current.get();
    await(restored, R.id.next);
    AtomicReference<String> after = new AtomicReference<>();
    ins.runOnMainSync(
        () ->
            after.set(
                ((android.widget.TextView) restored.findViewById(R.id.page)).getText().toString()));
    assertEquals(before.get(), after.get());
    ins.runOnMainSync(restored::finish);
  }

  private void awaitCurrentHome() throws InterruptedException {
    Thread.sleep(1000);
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  private View await(Activity activity, int id) throws Exception {
    AtomicReference<View> found = new AtomicReference<>();
    for (int n = 0; n < 100; n++) {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> found.set(activity.findViewById(id)));
      if (found.get() != null) return found.get();
      Thread.sleep(100);
    }
    throw new AssertionError("View not found: " + id);
  }
}
