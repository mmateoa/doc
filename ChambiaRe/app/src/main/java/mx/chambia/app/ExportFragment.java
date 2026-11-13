package mx.chambia.app;

import android.view.*;
import android.widget.*;
import mx.chambia.app.databinding.FragmentExportBinding;

public final class ExportFragment extends WorkspaceFragment {
  @Override
  protected void renderPage() {
    FragmentExportBinding b = FragmentExportBinding.inflate(getLayoutInflater(), container, true);
    String[] names = {
      getString(R.string.destination_phone),
      getString(R.string.destination_tv),
      getString(R.string.destination_print),
      getString(R.string.destination_accessible)
    };
    b.destination.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
    b.destination.setContentDescription(getString(R.string.export_title));
    int initial =
        destination.equals("TV")
            ? 1
            : java.util.Arrays.asList("IMPRESIÓN", "A4", "OFICIO").contains(destination) ? 2 : 0;
    b.destination.setSelection(initial);
    String[] formats = {"JPG", "PNG", "PDF", "TXT"};
    b.format.setAdapter(
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, formats));
    b.format.setContentDescription(getString(R.string.advanced_options));
    Runnable summary =
        () ->
            b.summary.setText(
                getString(R.string.export_summary, selected().size(), destination, format));
    b.destination.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int n, long id) {
            destination = n == 0 ? "REDES" : n == 1 ? "TV" : n == 2 ? "IMPRESIÓN" : "ACCESIBLE";
            format = n == 2 ? "PDF" : n == 3 ? "TXT" : "JPG";
            b.format.setSelection(java.util.Arrays.asList(formats).indexOf(format));
            b.usb.setVisibility(n == 1 ? View.VISIBLE : View.GONE);
            summary.run();
          }
        });
    b.format.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int n, long id) {
            format = formats[n];
            summary.run();
          }
        });
    b.advanced.setOnClickListener(
        v ->
            b.format.setVisibility(
                b.format.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    b.local.setOnClickListener(v -> export(format, "CELULAR"));
    b.share.setOnClickListener(v -> export(format, "COMPARTIR"));
    b.usb.setOnClickListener(v -> export("JPG", "USB"));
    b.audio.setOnClickListener(v -> go("accesible"));
    b.cancel.setOnClickListener(v -> cancelExport());
    model.exporting.observe(
        getViewLifecycleOwner(),
        busy -> {
          boolean running = Boolean.TRUE.equals(busy);
          b.progress.setVisibility(running ? View.VISIBLE : View.GONE);
          b.cancel.setVisibility(running ? View.VISIBLE : View.GONE);
          b.local.setEnabled(!running);
          b.share.setEnabled(!running);
          b.usb.setEnabled(!running);
        });
    summary.run();
  }
}
