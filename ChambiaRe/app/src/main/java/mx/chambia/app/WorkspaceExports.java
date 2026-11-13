package mx.chambia.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.chambia.app.data.Business;
import mx.chambia.app.data.Item;
import mx.chambia.app.data.Publication;
import mx.chambia.app.export.ExportManager;
import org.json.JSONArray;
import org.json.JSONObject;

/** Durable export, narration and sharing operations, separate from screen composition. */
final class WorkspaceExports {
  private final WorkspaceFragment workspace;

  WorkspaceExports(WorkspaceFragment workspace) {
    this.workspace = workspace;
  }

  void regenerate(Publication publication) {
    if (Boolean.TRUE.equals(workspace.model.exporting.getValue())) {
      workspace.toast(workspace.getString(R.string.ui_copy_36));
      return;
    }
    try {
      List<Item> snapshot =
          PublicationSnapshot.decode(publication.snapshotJson, publication.businessId);
      Business captured = PublicationSnapshot.copyBusiness(workspace.business);
      captured.templateId = publication.templateId;
      JSONObject own = new JSONObject(publication.businessJson);
      captured.mealJson = own.optString("mealJson", "{}");
      captured.name = own.optString("name", captured.name);
      captured.phone = own.optString("phone", captured.phone);
      captured.sector = own.optString("sector", captured.sector);
      captured.slogan = own.optString("slogan", captured.slogan);
      captured.profileJson = own.optString("profileJson", "{}");
      captured.publicationHeading = own.optString("publicationHeading", "");
      captured.publicationSession = own.optString("publicationSession", "");
      final android.content.Context context = workspace.requireContext().getApplicationContext();
      new AlertDialog.Builder(workspace.requireContext())
          .setTitle(workspace.getString(R.string.ui_copy_37))
          .setItems(
              new String[] {"JPG", "PNG", "PDF"},
              (dialog, which) -> {
                String format = new String[] {"JPG", "PNG", "PDF"}[which];
                workspace.enqueueExport(
                    captured,
                    snapshot,
                    format,
                    publication.destination,
                    publication.highContrast,
                    "COMPARTIR");
              })
          .show();
    } catch (Exception e) {
      workspace.toast(workspace.getString(R.string.ui_copy_38));
    }
  }

  void savePublication(
      Business business, List<Item> picked, List<File> files, String destination, boolean high) {
    try {
      mx.chambia.app.publication.PublicationStore.save(
          workspace.db.dao(), business, picked, files, destination, high);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  void export(String fileFormat, String mode) {
    if (workspace.selected().isEmpty()) {
      workspace.toast(workspace.getString(R.string.ui_copy_92));
      return;
    }
    if (!workspace.validatePublication()) return;
    if (Boolean.TRUE.equals(workspace.model.exporting.getValue())) {
      workspace.toast(workspace.getString(R.string.ui_copy_93));
      return;
    }
    workspace.enqueueExport(
        PublicationSnapshot.copyBusiness(workspace.business),
        PublicationSnapshot.copyItems(workspace.selected()),
        fileFormat,
        mode.equals("USB") ? "TV" : workspace.destination,
        workspace.accessible,
        mode);
  }

  void enqueueExport(
      Business captured,
      List<Item> content,
      String fileFormat,
      String target,
      boolean contrast,
      String mode) {
    final android.content.Context context = workspace.requireContext().getApplicationContext();
    androidx.work.OneTimeWorkRequest work =
        new androidx.work.OneTimeWorkRequest.Builder(mx.chambia.app.export.ExportWorker.class)
            .addTag("export_" + captured.id)
            .build();
    final android.content.SharedPreferences preferences =
        context.getSharedPreferences("workspace", 0);
    workspace.model.exporting.setValue(true);
    workspace.io.execute(
        () -> {
          try {
            JSONObject input = new JSONObject();
            input.put(
                "business", mx.chambia.app.publication.PublicationStore.businessJson(captured));
            input.put("items", PublicationSnapshot.encode(content));
            input.put("format", fileFormat);
            input.put("destination", target);
            input.put("contrast", contrast);
            File folder = new File(context.getFilesDir(), "export_requests");
            if (!folder.isDirectory() && !folder.mkdirs())
              throw new java.io.IOException(workspace.getString(R.string.ui_copy_94));
            File request = new File(folder, work.getId() + ".json");
            try (java.io.OutputStream stream = new java.io.FileOutputStream(request)) {
              stream.write(input.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            preferences
                .edit()
                .putString("work_" + captured.id, work.getId().toString())
                .putString("mode_" + work.getId(), mode)
                .putString("destination_" + work.getId(), target)
                .apply();
            androidx.work.WorkManager.getInstance(context).enqueue(work);
            workspace.runOnUiThread(
                () -> {
                  workspace.observeExport();
                  workspace.toast(workspace.getString(R.string.ui_copy_95));
                });
          } catch (Exception ex) {
            workspace.model.exporting.postValue(false);
            workspace.runOnUiThread(
                () -> workspace.toast(workspace.getString(R.string.ui_copy_96) + ex.getMessage()));
          }
        });
  }

  void observeExport() {
    android.content.SharedPreferences preferences =
        workspace.requireContext().getSharedPreferences("workspace", 0);
    String workId = preferences.getString("work_" + workspace.business.id, "");
    if (workId.isEmpty() || workspace.observedWork.equals(workId)) return;
    workspace.observedWork = workId;
    androidx.work.WorkManager manager =
        androidx.work.WorkManager.getInstance(workspace.requireContext());
    manager
        .getWorkInfoByIdLiveData(UUID.fromString(workId))
        .observe(
            workspace.getViewLifecycleOwner(),
            info -> {
              if (info == null) return;
              boolean busy = !info.getState().isFinished();
              workspace.model.exporting.setValue(busy);
              if (busy) {
                if (workspace.draftStatus != null)
                  workspace.draftStatus.setText(workspace.getString(R.string.ui_copy_97));
                return;
              }
              if (preferences.getBoolean("handled_" + workId, false)) return;
              if (info.getState() != androidx.work.WorkInfo.State.SUCCEEDED) {
                preferences.edit().putBoolean("handled_" + workId, true).apply();
                workspace.toast(
                    info.getState() == androidx.work.WorkInfo.State.CANCELLED
                        ? workspace.getString(R.string.ui_copy_98)
                        : (info.getOutputData().getString("error") == null
                            ? workspace.getString(R.string.ui_copy_99)
                            : info.getOutputData().getString("error")));
                return;
              }
              String publicationId = info.getOutputData().getString("publicationId");
              workspace.io.execute(
                  () -> {
                    try {
                      Publication publication = workspace.db.dao().publication(publicationId);
                      if (publication == null) return;
                      List<File> files = new ArrayList<>();
                      JSONArray paths = new JSONArray(publication.exportPathsJson);
                      for (int n = 0; n < paths.length(); n++)
                        files.add(new File(paths.getString(n)));
                      workspace.runOnUiThread(
                          () -> {
                            if (preferences.getBoolean("handled_" + workId, false)) return;
                            preferences.edit().putBoolean("handled_" + workId, true).apply();
                            workspace.pendingFiles.clear();
                            workspace.pendingFiles.addAll(files);
                            workspace.pendingPublication = publication.id;
                            workspace.pendingBusiness =
                                publication.businessId + "_" + workspace.business.name;
                            workspace.lastExport = files.get(0);
                            String mode = preferences.getString("mode_" + workId, "COMPARTIR");
                            workspace.destination =
                                preferences.getString("destination_" + workId, "TV");
                            if (mode.equals("COMPARTIR"))
                              workspace.launchShare(
                                  ExportManager.share(workspace.requireContext(), files));
                            else if (mode.equals("USB")) {
                              workspace.pendingUsb = files.get(0);
                              workspace.selectUsb();
                            } else workspace.selectLocal(files.get(0));
                            workspace.model.loadBusiness(workspace.business.id);
                          });
                    } catch (Exception ex) {
                      workspace.runOnUiThread(
                          () ->
                              workspace.toast(
                                  workspace.getString(R.string.ui_copy_100) + ex.getMessage()));
                    }
                  });
            });
  }

  void cancelExport() {
    String id =
        workspace
            .requireContext()
            .getSharedPreferences("workspace", 0)
            .getString("work_" + workspace.business.id, "");
    if (!id.isEmpty())
      androidx.work.WorkManager.getInstance(workspace.requireContext())
          .cancelWorkById(UUID.fromString(id));
  }

  void launchShare(Intent intent) {
    try {
      workspace.startActivity(intent);
      if (Intent.ACTION_CHOOSER.equals(intent.getAction())
          && !workspace.pendingPublication.isEmpty())
        workspace.recordEvent(workspace.pendingPublication, "SHARE_SHEET_OPENED", 0);
    } catch (Exception e) {
      workspace.toast(workspace.getString(R.string.ui_copy_101) + e.getMessage());
    }
  }

  void selectUsb() {
    Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    in.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    workspace.startActivityForResult(in, workspace.USB_REQUEST);
  }

  void selectLocal(File file) {
    workspace.pendingUsb = file;
    Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    in.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    workspace.startActivityForResult(in, workspace.USB_REQUEST + 1);
    workspace.toast(workspace.getString(R.string.ui_copy_102));
  }

  void recordEvent(String id, String type, int count) {
    if (id.isEmpty()) return;
    workspace.io.execute(
        () -> {
          try {
            workspace.db.runInTransaction(
                () -> {
                  Publication p = workspace.db.dao().publication(id);
                  if (p == null) return;
                  try {
                    JSONArray events = new JSONArray(p.eventsJson);
                    JSONObject event = new JSONObject();
                    event
                        .put("type", type)
                        .put("at", System.currentTimeMillis())
                        .put("files", count);
                    events.put(event);
                    workspace.db.dao().updatePublicationEvents(id, events.toString());
                  } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                  }
                });
          } catch (Exception ex) {
            workspace.runOnUiThread(
                () -> workspace.toast(workspace.getString(R.string.event_save_failed)));
          }
        });
  }

  String narrationScript() {
    String source = ExportManager.narration(workspace.business, workspace.selected());
    android.content.SharedPreferences preferences =
        workspace.requireContext().getSharedPreferences("narration_" + workspace.business.id, 0);
    return source.equals(preferences.getString("source", ""))
        ? preferences.getString("script", source)
        : source;
  }

  boolean configureVoice() {
    if (!workspace.ttsReady || workspace.tts == null) {
      workspace.toast(workspace.getString(R.string.ui_copy_104));
      return false;
    }
    android.content.SharedPreferences preferences =
        workspace.requireContext().getSharedPreferences("narration_" + workspace.business.id, 0);
    String selected = preferences.getString("voice", "");
    if (!selected.isEmpty() && workspace.tts.getVoices() != null)
      for (android.speech.tts.Voice voice : workspace.tts.getVoices())
        if (voice.getName().equals(selected)) workspace.tts.setVoice(voice);
    workspace.tts.setSpeechRate(preferences.getFloat("rate", 1f));
    if (workspace.tts.getVoice() == null
        || workspace.tts.getVoice().isNetworkConnectionRequired()) {
      workspace.toast(workspace.getString(R.string.ui_copy_105));
      return false;
    }
    return true;
  }

  void stopNarration() {
    if (workspace.audioJob != null) {
      workspace.audioJob.cancel();
      workspace.audioJob = null;
    }
    if (workspace.tts != null) workspace.tts.stop();
  }

  void speak() {
    if (Boolean.TRUE.equals(workspace.model.exporting.getValue())) {
      workspace.toast(workspace.getString(R.string.ui_copy_106));
      return;
    }
    if (!workspace.configureVoice()) return;
    List<String> segments =
        mx.chambia.app.accessibility.SpeechSegments.split(
            workspace.narrationScript(), TextToSpeech.getMaxSpeechInputLength() - 1);
    workspace.tts.stop();
    for (int index = 0; index < segments.size(); index++)
      if (workspace.tts.speak(segments.get(index), TextToSpeech.QUEUE_ADD, null, "preview_" + index)
          == TextToSpeech.ERROR) {
        workspace.toast(workspace.getString(R.string.ui_copy_107));
        break;
      }
  }

  void synthesizeAudio() {
    if (workspace.selected().isEmpty()) {
      workspace.toast(workspace.getString(R.string.ui_copy_108));
      return;
    }
    if (Boolean.TRUE.equals(workspace.model.exporting.getValue())) {
      workspace.toast(workspace.getString(R.string.ui_copy_109));
      return;
    }
    if (!workspace.configureVoice()) return;
    final Business captured = PublicationSnapshot.copyBusiness(workspace.business);
    final List<Item> content = PublicationSnapshot.copyItems(workspace.selected());
    File folder =
        new File(
            workspace.requireContext().getFilesDir(),
            "exports/" + captured.id + "/audio_" + UUID.randomUUID());
    final String script = workspace.narrationScript();
    workspace.model.exporting.setValue(true);
    try {
      workspace.audioJob =
          mx.chambia.app.accessibility.NarrationExporter.start(
              workspace.tts,
              folder,
              script,
              new mx.chambia.app.accessibility.NarrationExporter.Callback() {
                @Override
                public void complete(List<File> files) {
                  workspace.io.execute(
                      () -> {
                        try {
                          workspace.savePublication(captured, content, files, "ACCESIBLE", true);
                          workspace.model.exporting.postValue(false);
                          workspace.runOnUiThread(
                              () -> {
                                workspace.audioJob = null;
                                workspace.pendingFiles.clear();
                                workspace.pendingFiles.addAll(files);
                                workspace.pendingBusiness = captured.id + "_" + captured.name;
                                new AlertDialog.Builder(workspace.requireContext())
                                    .setTitle(workspace.getString(R.string.ui_copy_110))
                                    .setItems(
                                        new String[] {
                                          workspace.getString(R.string.ui_copy_111),
                                          workspace.getString(R.string.ui_copy_112),
                                          workspace.getString(R.string.ui_copy_113)
                                        },
                                        (dialog, index) -> {
                                          if (index == 0)
                                            workspace.launchShare(
                                                ExportManager.share(
                                                    workspace.requireContext(), files));
                                          else if (index == 1) workspace.selectLocal(files.get(0));
                                          else {
                                            File audio = files.get(1);
                                            Uri uri =
                                                androidx.core.content.FileProvider.getUriForFile(
                                                    workspace.requireContext(),
                                                    workspace.requireContext().getPackageName()
                                                        + ".files",
                                                    audio);
                                            Intent play =
                                                new Intent(Intent.ACTION_VIEW)
                                                    .setDataAndType(uri, "audio/wav")
                                                    .addFlags(
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            workspace.launchShare(play);
                                          }
                                        })
                                    .setNegativeButton("Cerrar", null)
                                    .show();
                              });
                        } catch (Exception ex) {
                          workspace.model.exporting.postValue(false);
                          workspace.runOnUiThread(
                              () ->
                                  workspace.toast(
                                      workspace.getString(R.string.ui_copy_114) + ex.getMessage()));
                        }
                      });
                }

                @Override
                public void error(String message) {
                  workspace.model.exporting.postValue(false);
                  workspace.runOnUiThread(
                      () -> {
                        workspace.audioJob = null;
                        workspace.toast(message);
                      });
                }
              });
      workspace.toast(workspace.getString(R.string.ui_copy_115));
    } catch (Exception ex) {
      workspace.model.exporting.setValue(false);
      workspace.toast(workspace.getString(R.string.ui_copy_116) + ex.getMessage());
    }
  }
}
