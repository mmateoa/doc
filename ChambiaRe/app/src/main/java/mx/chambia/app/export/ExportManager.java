package mx.chambia.app.export;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import mx.chambia.app.data.Business;
import mx.chambia.app.data.Item;
import mx.chambia.app.design.TemplateEngine;
import org.json.JSONObject;

public final class ExportManager {
  private ExportManager() {}

  public static String readable(Business b, List<Item> items) {
    items = mx.chambia.app.publication.MealPlan.content(b, items);
    StringBuilder text =
        new StringBuilder(b.name)
            .append("\n")
            .append(b.publicationHeading.isEmpty() ? "Productos y servicios" : b.publicationHeading)
            .append("\n")
            .append(b.publicationSession)
            .append("\n");
    mx.chambia.app.publication.MealPlan meal =
        mx.chambia.app.publication.MealPlan.decode(b.mealJson);
    if (meal.enabled) text.append(meal.heading()).append("\n");
    String category = "";
    for (Item item : items) {
      if (!category.equals(item.category)) {
        category = item.category;
        text.append("\n").append(category).append("\n");
      }
      text.append(item.name);
      String amount = mx.chambia.app.data.PriceLabel.of(item);
      if (!item.priceMode.equals("confirmar") && !amount.isEmpty())
        text.append(" — ").append(amount);
      text.append("\n");
      String details = mx.chambia.app.data.ItemOptions.details(item, false);
      if (!details.isEmpty()) text.append(details).append("\n");
    }
    if (meal.enabled && !meal.notes.isEmpty()) text.append(meal.notes).append("\n");
    if (b.phone != null && !b.phone.isEmpty())
      text.append("Contacto: ").append(b.phone).append("\n");
    try {
      JSONObject profile = new JSONObject(b.profileJson);
      for (String key : new String[] {"address", "hours", "networks"})
        if (!profile.optString(key, "").isEmpty()) text.append(profile.getString(key)).append("\n");
    } catch (Exception ex) {
      throw new IllegalArgumentException("Datos del negocio no válidos", ex);
    }
    return text.toString();
  }

  public static String narration(Business b, List<Item> items) {
    items = mx.chambia.app.publication.MealPlan.content(b, items);
    StringBuilder text = new StringBuilder("Bienvenidos a ").append(b.name).append(". ");
    mx.chambia.app.publication.MealPlan meal =
        mx.chambia.app.publication.MealPlan.decode(b.mealJson);
    if (meal.enabled) {
      text.append("Comida corrida. ");
      if (!meal.withoutPrices && meal.priceCents >= 0) {
        Item amount = new Item();
        amount.price = meal.priceCents / 100d;
        amount.priceMode = "fijo";
        text.append(mx.chambia.app.data.PriceLabel.spoken(amount)).append(". ");
      }
    }
    String category = "";
    for (Item item : items) {
      if (!category.equals(item.category)) {
        category = item.category;
        text.append(category).append(". ");
      }
      text.append(item.name);
      String amount = mx.chambia.app.data.PriceLabel.spoken(item);
      if (!item.priceMode.equals("confirmar") && !amount.isEmpty())
        text.append(", ").append(amount);
      text.append(". ");
      String details = mx.chambia.app.data.ItemOptions.details(item, true);
      if (!details.isEmpty()) text.append(details).append(". ");
    }
    if (meal.enabled && !meal.notes.isEmpty()) text.append(meal.notes).append(". ");
    return text.append("Gracias por su preferencia.").toString();
  }

  public static int[] dimensions(String destination) {
    if ("REDES".equals(destination)) return new int[] {1080, 1920};
    if ("CUADRADO".equals(destination)) return new int[] {1080, 1080};
    if ("A4".equals(destination)) return new int[] {1190, 1684};
    if ("OFICIO".equals(destination)) return new int[] {1275, 1950};
    if ("IMPRESIÓN".equals(destination)) return new int[] {1275, 1650};
    return new int[] {1920, 1080};
  }

  public static List<File> generate(
      Context ctx,
      Business b,
      List<Item> items,
      String template,
      String format,
      boolean accessible,
      String destination)
      throws Exception {
    return generate(ctx, b, items, template, format, accessible, destination, () -> false);
  }

  public static List<File> generate(
      Context ctx,
      Business b,
      List<Item> items,
      String template,
      String format,
      boolean accessible,
      String destination,
      java.util.function.BooleanSupplier cancelled)
      throws Exception {
    if (cancelled.getAsBoolean()) throw new java.io.InterruptedIOException("Exportación cancelada");
    if (!java.util.Arrays.asList("PDF", "PNG", "JPG", "TXT").contains(format))
      throw new IllegalArgumentException("Formato desconocido");
    String extension = format.toLowerCase(Locale.ROOT);
    File dir = new File(ctx.getFilesDir(), "exports/" + b.id + "/" + java.util.UUID.randomUUID());
    if (!dir.mkdirs()) throw new Exception("No se pudo crear carpeta");
    java.util.ArrayList<File> files = new java.util.ArrayList<>();
    if (format.equals("TXT")) {
      File text = new File(dir, "menu.txt");
      try (OutputStream out = new FileOutputStream(text)) {
        out.write(readable(b, items).getBytes(StandardCharsets.UTF_8));
      }
      files.add(text);
      return files;
    }
    TemplateEngine engine = new TemplateEngine(ctx, template);
    int[] dimensions = dimensions(destination);
    int width = dimensions[0], height = dimensions[1];
    if (format.equals("PDF")) {
      if ("IMPRESIÓN".equals(destination)) {
        width = 612;
        height = 792;
      } else if ("A4".equals(destination)) {
        width = 595;
        height = 842;
      } else if ("OFICIO".equals(destination)) {
        width = 612;
        height = 936;
      }
    }
    List<List<Item>> pages = engine.pages(b, items, width, height, accessible);
    try {
      if (format.equals("PDF")) {
        File f = new File(dir, "publicacion.pdf");
        PdfDocument doc = new PdfDocument();
        try {
          for (int page = 0; page < pages.size(); page++) {
            if (cancelled.getAsBoolean())
              throw new java.io.InterruptedIOException("Exportación cancelada");
            PdfDocument.Page p =
                doc.startPage(new PdfDocument.PageInfo.Builder(width, height, page + 1).create());
            try {
              engine.draw(p.getCanvas(), b, pages.get(page), width, height, accessible);
            } finally {
              doc.finishPage(p);
            }
          }
          try (FileOutputStream out = new FileOutputStream(f)) {
            doc.writeTo(out);
          }
        } finally {
          doc.close();
        }
        files.add(f);
      } else if (!format.equals("TXT")) {
        for (int page = 0; page < pages.size(); page++) {
          if (cancelled.getAsBoolean())
            throw new java.io.InterruptedIOException("Exportación cancelada");
          File f = new File(dir, String.format(Locale.ROOT, "%02d_menu.%s", page + 1, extension));
          Bitmap bitmap = engine.bitmap(b, pages.get(page), width, height, accessible);
          try (FileOutputStream out = new FileOutputStream(f)) {
            if (!bitmap.compress(
                format.equals("PNG") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                92,
                out)) throw new IOException("No se pudo codificar imagen");
          } finally {
            bitmap.recycle();
          }
          files.add(f);
        }
      }
      File txt = new File(dir, "menu.txt");
      try (OutputStream out = new FileOutputStream(txt)) {
        out.write(readable(b, items).getBytes(StandardCharsets.UTF_8));
      }
      files.add(txt);
      return files;
    } catch (Exception e) {
      for (File partial : dir.listFiles() == null ? new File[0] : dir.listFiles()) partial.delete();
      dir.delete();
      throw e;
    }
  }

  public static Intent share(Context ctx, List<File> files) {
    java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
    for (File file : files)
      uris.add(FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".files", file));
    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
    intent.setType("*/*");
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    android.content.ClipData clip = android.content.ClipData.newRawUri("Publicación", uris.get(0));
    for (int n = 1; n < uris.size(); n++)
      clip.addItem(new android.content.ClipData.Item(uris.get(n)));
    intent.setClipData(clip);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    return Intent.createChooser(intent, "Compartir publicación");
  }

  public static Intent share(Context ctx, File file) {
    Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".files", file);
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType(mime(file.getName()));
    i.putExtra(Intent.EXTRA_STREAM, uri);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    return Intent.createChooser(i, "Compartir publicación");
  }

  public static String mime(String name) {
    if (name.endsWith(".pdf")) return "application/pdf";
    if (name.endsWith(".png")) return "image/png";
    if (name.endsWith(".txt")) return "text/plain";
    if (name.endsWith(".wav")) return "audio/wav";
    return "image/jpeg";
  }

  public static void copyToTree(Context ctx, Uri uri, File f, String business, String section)
      throws Exception {
    DocumentFile root = DocumentFile.fromTreeUri(ctx, uri);
    if (root == null || !root.canWrite())
      throw new Exception("Ubicación no disponible para escritura");
    DocumentFile base = folder(root, "CHAMB-IA");
    DocumentFile own = folder(base, safeName(business));
    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    DocumentFile day = folder(own, date);
    DocumentFile publication = folder(day, f.getParentFile().getName());
    DocumentFile output =
        folder(
            publication,
            (f.getName().endsWith(".txt") || f.getName().endsWith(".wav")) ? "ACCESIBLE" : section);
    DocumentFile dest = output.createFile(mime(f.getName()), f.getName());
    if (dest == null) throw new Exception("No se pudo crear el archivo destino");
    try (java.io.InputStream in = new java.io.FileInputStream(f);
        OutputStream out = ctx.getContentResolver().openOutputStream(dest.getUri())) {
      if (out == null) throw new Exception("No se pudo escribir el archivo");
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    } catch (Exception e) {
      dest.delete();
      throw e;
    }
    if (dest.length() != f.length()) {
      dest.delete();
      throw new IOException("No se pudo verificar el tamaño del archivo copiado");
    }
  }

  private static byte[] checksum(java.io.InputStream input) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[8192];
    int size;
    while ((size = input.read(buffer)) != -1) digest.update(buffer, 0, size);
    return digest.digest();
  }

  private static DocumentFile folder(DocumentFile parent, String name) throws Exception {
    DocumentFile d = parent.findFile(name);
    if (d == null) d = parent.createDirectory(name);
    if (d == null || !d.isDirectory()) throw new Exception("No se pudo crear " + name);
    return d;
  }

  private static String safeName(String s) {
    String v = s.replaceAll("[^\\p{L}\\p{N} _-]", "_").trim();
    return v.isEmpty() ? "Negocio" : v.substring(0, Math.min(v.length(), 60));
  }
}
