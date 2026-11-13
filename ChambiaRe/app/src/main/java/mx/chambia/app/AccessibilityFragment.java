package mx.chambia.app;

import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
import java.util.*;
import mx.chambia.app.databinding.FragmentAccessibilityBinding;
import mx.chambia.app.export.ExportManager;

public final class AccessibilityFragment extends WorkspaceFragment {
  @Override
  protected void renderPage() {
    FragmentAccessibilityBinding b =
        FragmentAccessibilityBinding.inflate(getLayoutInflater(), container, true);
    android.content.SharedPreferences preferences =
        requireContext().getSharedPreferences("narration_" + business.id, 0);
    b.contrast.setChecked(accessible);
    b.contrast.setOnCheckedChangeListener(
        (v, c) -> {
          accessible = c;
          requireContext()
              .getSharedPreferences("workspace", 0)
              .edit()
              .putBoolean("contrast_" + business.id, c)
              .apply();
        });
    b.voice.setChecked(narrated);
    b.voice.setOnCheckedChangeListener(
        (v, c) -> {
          narrated = c;
          requireContext()
              .getSharedPreferences("workspace", 0)
              .edit()
              .putBoolean("narrated_" + business.id, c)
              .apply();
        });
    b.script.setText(narrationScript());
    b.save.setOnClickListener(
        v -> {
          preferences
              .edit()
              .putString("source", ExportManager.narration(business, selected()))
              .putString("script", b.script.getText().toString())
              .apply();
          toast(getString(R.string.script_saved));
        });
    b.speed.setAdapter(
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              getString(R.string.speed_slow),
              getString(R.string.speed_normal),
              getString(R.string.speed_fast)
            }));
    float rate = preferences.getFloat("rate", 1f);
    b.speed.setSelection(rate < 1 ? 0 : rate > 1 ? 2 : 1);
    b.speed.setContentDescription(getString(R.string.speed_normal));
    b.speed.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int n, long id) {
            preferences.edit().putFloat("rate", new float[] {.8f, 1f, 1.2f}[n]).apply();
          }
        });
    b.chooseVoice.setOnClickListener(
        clicked -> {
          if (!ttsReady || tts == null || tts.getVoices() == null) {
            toast(getString(R.string.ui_copy_117));
            return;
          }
          List<android.speech.tts.Voice> voices = new ArrayList<>();
          for (android.speech.tts.Voice installed : tts.getVoices())
            if (!installed.isNetworkConnectionRequired()
                && "es".equals(installed.getLocale().getLanguage())) voices.add(installed);
          if (voices.isEmpty()) {
            toast(getString(R.string.ui_copy_118));
            return;
          }
          String[] names = new String[voices.size()];
          for (int n = 0; n < names.length; n++)
            names[n] = voices.get(n).getName() + " · " + voices.get(n).getLocale();
          new AlertDialog.Builder(requireContext())
              .setTitle(getString(R.string.ui_copy_119))
              .setItems(
                  names,
                  (dialog, index) -> {
                    preferences.edit().putString("voice", voices.get(index).getName()).apply();
                    tts.setVoice(voices.get(index));
                  })
              .show();
        });

    b.listen.setOnClickListener(v -> speak());
    b.stop.setOnClickListener(v -> stopNarration());
    b.audio.setOnClickListener(v -> synthesizeAudio());
    b.text.setOnClickListener(v -> export("TXT", "COMPARTIR"));
    b.back.setOnClickListener(v -> go("vista"));
  }
}
