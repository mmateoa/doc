package mx.chambia.app.accessibility;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Produces real engine WAV files and the exact reviewed script, in bounded segments. */
public final class NarrationExporter {
  public interface Callback {
    void complete(List<File> files);

    void error(String message);
  }

  private final TextToSpeech engine;
  private final File folder;
  private final Callback callback;
  private final List<File> files = new ArrayList<>();
  private final List<String> segments;
  private final String prefix = UUID.randomUUID().toString();
  private int index;
  private boolean finished;

  private NarrationExporter(TextToSpeech engine, File folder, String script, Callback callback) {
    this.engine = engine;
    this.folder = folder;
    this.callback = callback;
    segments = SpeechSegments.split(script, TextToSpeech.getMaxSpeechInputLength() - 1);
  }

  public static NarrationExporter start(
      TextToSpeech engine, File folder, String script, Callback callback) throws IOException {
    if (script.trim().isEmpty()) throw new IOException("El guion está vacío");
    if (!folder.mkdirs()) throw new IOException("No se pudo crear la carpeta de audio");
    NarrationExporter exporter = new NarrationExporter(engine, folder, script, callback);
    File text = new File(folder, "menu.txt");
    try (OutputStream out = new FileOutputStream(text)) {
      out.write(script.getBytes(StandardCharsets.UTF_8));
    }
    exporter.files.add(text);
    engine.setOnUtteranceProgressListener(
        new UtteranceProgressListener() {
          @Override
          public void onStart(String id) {}

          @Override
          public void onDone(String id) {
            exporter.done(id);
          }

          @Override
          public void onError(String id) {
            exporter.fail("La voz no pudo generar el audio");
          }

          @Override
          public void onStop(String id, boolean interrupted) {
            exporter.fail("Narración cancelada");
          }
        });
    exporter.next();
    return exporter;
  }

  private synchronized void next() {
    if (finished) return;
    if (index == segments.size()) {
      finished = true;
      callback.complete(new ArrayList<>(files));
      return;
    }
    File audio = new File(folder, String.format(Locale.ROOT, "audio_%03d.wav", index + 1));
    files.add(audio);
    if (engine.synthesizeToFile(segments.get(index), new Bundle(), audio, prefix + ":" + index)
        == TextToSpeech.ERROR) fail("El motor no permite exportar audio");
  }

  private synchronized void done(String id) {
    if (finished || !id.equals(prefix + ":" + index)) return;
    File audio = files.get(files.size() - 1);
    try (InputStream in = new FileInputStream(audio)) {
      byte[] header = new byte[12];
      if (in.read(header) != 12
          || audio.length() < 44
          || header[0] != 'R'
          || header[1] != 'I'
          || header[2] != 'F'
          || header[3] != 'F'
          || header[8] != 'W'
          || header[9] != 'A'
          || header[10] != 'V'
          || header[11] != 'E') throw new IOException("El motor no produjo WAV válido");
    } catch (IOException ex) {
      fail(ex.getMessage());
      return;
    }
    index++;
    next();
  }

  private synchronized void fail(String message) {
    if (finished) return;
    finished = true;
    for (File file : files) file.delete();
    folder.delete();
    callback.error(message);
  }

  public synchronized void cancel() {
    if (finished) return;
    engine.stop();
    fail("Narración cancelada");
  }
}
