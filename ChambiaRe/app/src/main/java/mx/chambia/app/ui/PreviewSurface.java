package mx.chambia.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.design.TemplateEngine;

/** Renders directly with the export compositor; no screenshots or placeholder art. */
public final class PreviewSurface extends View {
  private Business business;
  private List<Item> page = Collections.emptyList();
  private TemplateEngine engine;
  private int width = 1920, height = 1080;
  private boolean contrast;

  public PreviewSurface(Context c, AttributeSet a) {
    super(c, a);
  }

  public void bind(
      Business b, List<Item> items, String template, int w, int h, boolean high, int number) {
    business = mx.chambia.app.PublicationSnapshot.copyBusiness(b);
    width = w;
    height = h;
    contrast = high;
    engine = new TemplateEngine(getContext(), template);
    List<List<Item>> pages = engine.pages(b, items, w, h, high);
    page = pages.get(Math.min(number, pages.size() - 1));
    requestLayout();
    setContentDescription(mx.chambia.app.export.ExportManager.readable(b, page));
    invalidate();
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int w = MeasureSpec.getSize(widthSpec);
    int h =
        getLayoutParams() != null
                && getLayoutParams().height == android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ? Math.max(getMinimumHeight(), w * height / width)
            : MeasureSpec.getSize(heightSpec);
    setMeasuredDimension(w, h);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (engine == null) return;
    float scale = Math.min(getWidth() / (float) width, getHeight() / (float) height);
    canvas.save();
    canvas.translate((getWidth() - width * scale) / 2, (getHeight() - height * scale) / 2);
    canvas.scale(scale, scale);
    engine.draw(canvas, business, page, width, height, contrast);
    canvas.restore();
  }
}
