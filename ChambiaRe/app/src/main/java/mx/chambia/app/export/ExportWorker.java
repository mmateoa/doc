package mx.chambia.app.export;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mx.chambia.app.PublicationSnapshot;
import mx.chambia.app.data.*;
import mx.chambia.app.publication.PublicationStore;
import org.json.JSONObject;

/** A durable request refers to a private immutable file, never a mutable catalogue row. */
public final class ExportWorker extends Worker {
  public ExportWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
    super(context, parameters);
  }

  @NonNull
  @Override
  public Result doWork() {
    File request =
        new File(getApplicationContext().getFilesDir(), "export_requests/" + getId() + ".json");
    List<File> files = null;
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (InputStream stream = new FileInputStream(request)) {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = stream.read(buffer)) != -1) {
          bytes.write(buffer, 0, length);
          if (bytes.size() > 4 * 1024 * 1024) throw new IOException("Publicación demasiado grande");
        }
      }
      JSONObject input = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
      Business business = PublicationStore.business(input.getString("business"));
      List<Item> items = PublicationSnapshot.decode(input.getString("items"), business.id);
      setProgressAsync(new Data.Builder().putString("status", "Generando materiales").build());
      files =
          ExportManager.generate(
              getApplicationContext(),
              business,
              items,
              business.templateId,
              input.getString("format"),
              input.getBoolean("contrast"),
              input.getString("destination"),
              this::isStopped);
      if (isStopped()) throw new InterruptedIOException("Exportación cancelada");
      AppDatabase database = ChambiaRepository.get(getApplicationContext()).database;
      final List<File> generated = files;
      final Publication[] saved = new Publication[1];
      database.runInTransaction(
          () -> {
            boolean found = false;
            for (Business candidate : database.dao().allBusinesses())
              if (candidate.id.equals(business.id)) found = true;
            if (!found)
              throw new IllegalStateException("El negocio fue eliminado antes de terminar");
            try {
              saved[0] =
                  PublicationStore.save(
                      database.dao(),
                      business,
                      items,
                      generated,
                      input.getString("destination"),
                      input.getBoolean("contrast"));
            } catch (Exception ex) {
              throw new IllegalStateException(ex);
            }
          });
      return Result.success(new Data.Builder().putString("publicationId", saved[0].id).build());
    } catch (Exception ex) {
      if (files != null) {
        for (File file : files) file.delete();
        if (!files.isEmpty()) files.get(0).getParentFile().delete();
      }
      return Result.failure(
          new Data.Builder()
              .putString("error", ex.getMessage() == null ? "Error al exportar" : ex.getMessage())
              .build());
    } finally {
      request.delete();
    }
  }
}
