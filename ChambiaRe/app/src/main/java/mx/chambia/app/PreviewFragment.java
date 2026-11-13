package mx.chambia.app;

import android.view.*;
import android.widget.*;
import mx.chambia.app.databinding.FragmentPreviewBinding;
import mx.chambia.app.design.TemplateEngine;
import mx.chambia.app.export.ExportManager;

public final class PreviewFragment extends WorkspaceFragment {
  private static final String[] DESTINATIONS = {
    "TV", "REDES", "CUADRADO", "IMPRESIÓN", "A4", "OFICIO"
  };

  @Override
  protected void renderPage() {
    FragmentPreviewBinding b = FragmentPreviewBinding.inflate(getLayoutInflater(), container, true);
    int[] labels = {
      R.string.format_tv,
      R.string.format_social,
      R.string.format_square,
      R.string.format_letter,
      R.string.format_a4,
      R.string.format_legal
    };
    String[] names = new String[labels.length];
    for (int n = 0; n < names.length; n++) names[n] = getString(labels[n]);
    b.format.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
    b.format.setSelection(Math.max(0, java.util.Arrays.asList(DESTINATIONS).indexOf(destination)));
    Runnable refresh =
        () -> {
          try {
            int[] size = ExportManager.dimensions(destination);
            TemplateEngine engine = new TemplateEngine(requireContext(), business.templateId);
            int pages = engine.pages(business, selected(), size[0], size[1], accessible).size();
            previewPage = Math.max(0, Math.min(previewPage, pages - 1));
            b.preview.bind(
                business,
                selected(),
                business.templateId,
                size[0],
                size[1],
                accessible,
                previewPage);
            b.page.setText(getString(R.string.page_count, previewPage + 1, pages));
            model.uiState.set(
                "page:" + business.id + ":" + activeDraft.id + ":" + page, previewPage);
            model.uiState.set(
                "destination:" + business.id + ":" + activeDraft.id + ":" + page, destination);
            b.previous.setEnabled(previewPage > 0);
            b.next.setEnabled(previewPage + 1 < pages);
            b.generate.setEnabled(!selected().isEmpty());
          } catch (Exception ex) {
            b.generate.setEnabled(false);
            b.page.setText(ex.getMessage());
          }
        };
    b.format.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int n, long id) {
            if (!destination.equals(DESTINATIONS[n])) previewPage = 0;
            destination = DESTINATIONS[n];
            refresh.run();
          }
        });
    b.previous.setOnClickListener(
        v -> {
          previewPage--;
          refresh.run();
        });
    b.next.setOnClickListener(
        v -> {
          previewPage++;
          refresh.run();
        });
    b.edit.setOnClickListener(v -> go("preparar"));
    b.design.setOnClickListener(v -> go("disenos"));
    b.accessibility.setOnClickListener(v -> go("accesible"));
    b.generate.setOnClickListener(
        v -> {
          if (validatePublication()) go("exportar");
        });
    b.preview.setOnClickListener(
        v -> {
          mx.chambia.app.ui.PreviewSurface image =
              new mx.chambia.app.ui.PreviewSurface(requireContext(), null);
          int[] size = ExportManager.dimensions(destination);
          try {
            image.bind(
                business,
                selected(),
                business.templateId,
                size[0],
                size[1],
                accessible,
                previewPage);
            image.setMinimumHeight(dp(400));
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.zoom_title)
                .setView(image)
                .setPositiveButton(R.string.close, null)
                .show();
          } catch (Exception ex) {
            toast(ex.getMessage());
          }
        });
    refresh.run();
  }
}
