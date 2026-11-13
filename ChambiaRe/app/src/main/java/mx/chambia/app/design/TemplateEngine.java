package mx.chambia.app.design;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.publication.MealPlan;
import org.json.*;

/** Shared measured compositor for preview, thumbnails, raster and vector PDF. */
public final class TemplateEngine {
  private final JSONObject template;
  private final String id;

  public TemplateEngine(Context context, String id) {
    this.id = id;
    if (!Arrays.asList("mexicano", "minimalista", "elegante").contains(id))
      throw new IllegalArgumentException("Plantilla desconocida");
    try (InputStream in = context.getAssets().open("plantillas/" + id + ".json");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] bytes = new byte[2048];
      int n;
      while ((n = in.read(bytes)) != -1) {
        out.write(bytes, 0, n);
        if (out.size() > 65536) throw new IOException("Plantilla demasiado grande");
      }
      template = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
      if (template.optInt("version", 1) != 1) throw new IOException("Versión incompatible");
      for (String key : new String[] {"background", "foreground", "accent", "secondary"})
        Color.parseColor(template.getString(key));
    } catch (Exception ex) {
      throw new IllegalArgumentException("No se pudo abrir el diseño", ex);
    }
  }

  private int color(String key) {
    return Color.parseColor(template.optString(key, "#000000"));
  }

  private StaticLayout layout(String value, int width, int size, int color, boolean bold) {
    TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    p.setTextSize(size);
    p.setColor(color);
    p.setTypeface(
        Typeface.create(
            id.equals("elegante") ? "serif" : "sans-serif",
            bold ? Typeface.BOLD : Typeface.NORMAL));
    return StaticLayout.Builder.obtain(value, 0, value.length(), p, width)
        .setIncludePad(false)
        .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
        .build();
  }

  private int height(String s, int w, int size, boolean bold) {
    return s.isEmpty() ? 0 : layout(s, w, size, Color.BLACK, bold).getHeight();
  }

  private void text(
      Canvas c, String s, float x, float y, int w, int size, int color, boolean bold) {
    if (s.isEmpty()) return;
    c.save();
    c.translate(x, y);
    layout(s, w, size, color, bold).draw(c);
    c.restore();
  }

  private String heading(Business b) {
    MealPlan meal = MealPlan.decode(b.mealJson);
    return meal.enabled
        ? meal.heading()
        : b.publicationHeading.isEmpty()
            ? ("fonda".equals(b.sector) ? "Menú del día" : "Productos y servicios")
            : b.publicationHeading;
  }

  private String footer(Business b) {
    StringBuilder s = new StringBuilder();
    if (b.slogan != null && !b.slogan.isEmpty()) s.append(b.slogan).append("\n");
    MealPlan meal = MealPlan.decode(b.mealJson);
    if (meal.enabled && !meal.notes.isEmpty()) s.append(meal.notes).append("\n");
    if (!b.publicationSession.isEmpty()) s.append(b.publicationSession).append("\n");
    if (b.phone != null && !b.phone.isEmpty()) s.append("Contacto: ").append(b.phone).append("\n");
    try {
      JSONObject p = new JSONObject(b.profileJson);
      for (String key : new String[] {"address", "hours", "networks"})
        if (!p.optString(key).isEmpty()) s.append(p.getString(key)).append("\n");
    } catch (JSONException ex) {
      throw new IllegalArgumentException("Datos de negocio no válidos", ex);
    }
    return s.toString().trim();
  }

  private int top(Business b) {
    return 60 + height(b.name, 1000, 44, true) + 24 + height(heading(b), 1000, 32, true) + 32;
  }

  private int bottom(Business b) {
    return 50 + height(footer(b), 1000, 23, false);
  }

  private String price(Item i) {
    return i.priceMode.equals("confirmar") && !i.state.equals("agotado") ? "" : PriceLabel.of(i);
  }

  private int rowHeight(Item i, int width, boolean high) {
    int size = high ? 34 : 28;
    String details = ItemOptions.details(i, false);
    return height(i.name, width, size, true)
        + height(price(i), width, size - 2, true)
        + height(details, width, size - 5, false)
        + 26;
  }

  private int sectionHeight(String s, int width, boolean high) {
    return s.isEmpty() ? 0 : height(s, width, high ? 29 : 24, true) + 14;
  }

  public java.util.List<java.util.List<Item>> pages(
      Business b, java.util.List<Item> items, int width, int height) {
    return pages(b, items, width, height, false);
  }

  public java.util.List<java.util.List<Item>> pages(
      Business b, java.util.List<Item> items, int width, int height, boolean high) {
    float h = height * 1200f / width, available = h - bottom(b) - top(b);
    if (available < 100)
      throw new IllegalArgumentException(
          "Reduce el texto de cabecera o contacto para este formato");
    int columns = h < 1000 && !id.equals("minimalista") ? 2 : 1, tw = columns == 2 ? 480 : 1000;
    java.util.List<java.util.List<Item>> result = new ArrayList<>();
    java.util.List<Item> page = new ArrayList<>();
    float used = 0;
    int column = 0;
    String previous = "";
    for (Item i : MealPlan.content(b, items)) {
      int head = previous.equals(i.category) ? 0 : sectionHeight(i.category, tw, high);
      int row = rowHeight(i, tw, high);
      if (row + sectionHeight(i.category, tw, high) > available)
        throw new IllegalArgumentException("Demasiado texto en una opción: " + i.name);
      if (used + head + row > available) {
        column++;
        used = 0;
        previous = "";
        head = sectionHeight(i.category, tw, high);
        if (column >= columns) {
          result.add(page);
          page = new ArrayList<>();
          column = 0;
        }
      }
      page.add(i);
      used += head + row;
      previous = i.category;
    }
    if (!page.isEmpty() || result.isEmpty()) result.add(page);
    return result;
  }

  public Bitmap bitmap(Business b, java.util.List<Item> items, int w, int h, boolean high) {
    if (w < 100 || h < 100 || w > 4096 || h > 4096)
      throw new IllegalArgumentException("Dimensiones no permitidas");
    Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    try {
      draw(new Canvas(image), b, items, w, h, high);
      return image;
    } catch (RuntimeException ex) {
      image.recycle();
      throw ex;
    }
  }

  public void draw(
      Canvas c, Business b, java.util.List<Item> items, int w, int height, boolean high) {
    if (w <= 0 || height <= 0) return;
    int bg = high ? Color.WHITE : color("background"),
        fg = high ? Color.BLACK : color("foreground"),
        accent = high ? Color.BLACK : color("accent"),
        secondary = high ? Color.BLACK : color("secondary");
    float h = height * 1200f / w;
    c.drawColor(bg);
    c.save();
    c.scale(w / 1200f, w / 1200f);
    Paint p = new Paint(3);
    if (id.equals("mexicano")) {
      p.setColor(accent);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(3);
      c.drawRoundRect(22, 22, 1178, h - 22, 18, 18, p);
      p.setStyle(Paint.Style.FILL);
      flower(c, 42, 45, accent, secondary);
      flower(c, 1157, h - 45, secondary, accent);
      p.setColor(accent);
      c.drawRoundRect(80, 40, 1120, top(b) - 24, 14, 14, p);
      text(c, b.name, 100, 52, 1000, 44, Color.WHITE, true);
      text(
          c,
          heading(b),
          100,
          76 + this.height(b.name, 1000, 44, true),
          1000,
          32,
          Color.WHITE,
          true);
    } else if (id.equals("elegante")) {
      p.setColor(accent);
      c.drawRect(70, 42, 78, h - 42, p);
      text(c, b.name, 100, 52, 1000, 44, fg, true);
      p.setColor(secondary);
      c.drawRect(
          100,
          64 + this.height(b.name, 1000, 44, true),
          1100,
          68 + this.height(b.name, 1000, 44, true),
          p);
      text(c, heading(b), 100, 82 + this.height(b.name, 1000, 44, true), 1000, 32, fg, false);
    } else {
      text(c, b.name, 100, 52, 1000, 44, fg, true);
      text(c, heading(b), 100, 76 + this.height(b.name, 1000, 44, true), 1000, 32, accent, true);
      p.setColor(accent);
      c.drawRect(100, top(b) - 15, 1100, top(b) - 9, p);
    }
    int columns = h < 1000 && !id.equals("minimalista") ? 2 : 1,
        tw = columns == 2 ? 480 : 1000,
        column = 0;
    float y = top(b);
    String previous = "";
    for (Item i : items) {
      int row = rowHeight(i, tw, high),
          head = previous.equals(i.category) ? 0 : sectionHeight(i.category, tw, high);
      if (y + head + row > h - bottom(b)) {
        column++;
        y = top(b);
        previous = "";
        head = sectionHeight(i.category, tw, high);
      }
      if (column >= columns) throw new IllegalArgumentException("Contenido sin paginar");
      float x = 100 + column * 520;
      if (head > 0) {
        text(c, i.category, x, y, tw, high ? 29 : 24, secondary, true);
        y += head;
      }
      int size = high ? 34 : 28;
      text(c, i.name, x, y, tw, size, fg, true);
      float line = y + this.height(i.name, tw, size, true);
      String amount = price(i);
      if (!amount.isEmpty()) {
        text(c, amount, x, line, tw, size - 2, fg, true);
        line += this.height(amount, tw, size - 2, true);
      }
      text(c, ItemOptions.details(i, false), x, line, tw, size - 5, fg, false);
      p.setColor(secondary);
      p.setAlpha(80);
      c.drawLine(x, y + row - 10, x + tw, y + row - 10, p);
      p.setAlpha(255);
      y += row;
      previous = i.category;
    }
    text(c, footer(b), 100, h - bottom(b) + 12, 1000, 23, fg, false);
    c.restore();
  }

  private static void flower(Canvas c, float x, float y, int a, int b) {
    Paint p = new Paint(3);
    for (int n = 0; n < 8; n++) {
      p.setColor(n % 2 == 0 ? a : b);
      c.drawCircle(
          x + (float) Math.cos(n * Math.PI / 4) * 14,
          y + (float) Math.sin(n * Math.PI / 4) * 14,
          7,
          p);
    }
    p.setColor(Color.rgb(243, 174, 69));
    c.drawCircle(x, y, 6, p);
  }
}
