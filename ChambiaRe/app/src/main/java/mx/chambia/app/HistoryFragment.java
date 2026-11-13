package mx.chambia.app;

import android.view.*;
import androidx.recyclerview.widget.*;
import java.io.File;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.databinding.*;
import org.json.JSONArray;

public final class HistoryFragment extends WorkspaceFragment {
  private final List<Draft> drafts = new ArrayList<>();
  private boolean showDrafts = false;

  @Override
  protected void renderPage() {
    FragmentHistoryBinding b = FragmentHistoryBinding.inflate(getLayoutInflater(), container, true);
    b.publications.setLayoutManager(new LinearLayoutManager(requireContext()));
    HistoryAdapter adapter = new HistoryAdapter();
    b.publications.setAdapter(adapter);
    Runnable refresh =
        () -> {
          List<String> ids = new ArrayList<>();
          if (showDrafts) {
            for (Draft d : drafts) ids.add(d.id);
          } else for (Publication pub : publications) ids.add(pub.id);
          adapter.submitList(ids);
          b.empty.setVisibility(ids.isEmpty() ? View.VISIBLE : View.GONE);
        };
    b.tabs.check(showDrafts ? R.id.drafts : R.id.generated);
    b.tabs.addOnButtonCheckedListener(
        (group, id, checked) -> {
          if (checked) {
            showDrafts = id == R.id.drafts;
            adapter.notifyDataSetChanged();
            refresh.run();
          }
        });
    b.create.setOnClickListener(v -> go("preparar"));
    String owner = business.id;
    io.execute(
        () -> {
          List<Draft> loaded = db.dao().drafts(owner);
          runOnUiThread(
              () -> {
                if (business.id.equals(owner)) {
                  drafts.clear();
                  drafts.addAll(loaded);
                  refresh.run();
                }
              });
        });
    refresh.run();
  }

  private List<File> files(Publication p) {
    List<File> result = new ArrayList<>();
    try {
      JSONArray paths = new JSONArray(p.exportPathsJson);
      for (int n = 0; n < paths.length(); n++) result.add(new File(paths.getString(n)));
    } catch (Exception ignored) {
    }
    return result;
  }

  private boolean available(List<File> files) {
    if (files.isEmpty()) return false;
    for (File f : files) if (!f.isFile()) return false;
    return true;
  }

  private void actions(Publication pub) {
    List<File> files = files(pub);
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        .setTitle(pub.title)
        .setItems(
            new String[] {
              getString(R.string.duplicate),
              getString(R.string.share),
              getString(R.string.regenerate),
              getString(R.string.delete)
            },
            (d, index) -> {
              if (index == 0) reuse(pub);
              else if (index == 1) {
                if (available(files)) {
                  pendingPublication = pub.id;
                  launchShare(mx.chambia.app.export.ExportManager.share(requireContext(), files));
                } else toast(getString(R.string.missing_file));
              } else if (index == 2) regenerate(pub);
              else
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_publication)
                    .setMessage(R.string.delete_publication_help)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(
                        R.string.delete,
                        (dialog, w) -> {
                          persist(() -> db.dao().deletePublication(pub.id, business.id));
                          publications.remove(pub);
                          showPage();
                        })
                    .show();
            })
        .show();
  }

  private final class HistoryAdapter extends ListAdapter<String, HistoryHolder> {
    HistoryAdapter() {
      super(
          new DiffUtil.ItemCallback<String>() {
            public boolean areItemsTheSame(String a, String b) {
              return a.equals(b);
            }

            public boolean areContentsTheSame(String a, String b) {
              return false;
            }
          });
    }

    public HistoryHolder onCreateViewHolder(ViewGroup parent, int type) {
      return new HistoryHolder(RowPublicationBinding.inflate(getLayoutInflater(), parent, false));
    }

    public void onBindViewHolder(HistoryHolder h, int position) {
      String id = getItem(position);
      if (showDrafts) {
        Draft draft = null;
        for (Draft d : drafts) if (d.id.equals(id)) draft = d;
        if (draft == null) return;
        final Draft chosen = draft;
        h.b.thumbnail.setVisibility(View.GONE);
        h.b.title.setText(draft.title);
        h.b.status.setText(draft.mode + " · " + draft.session);
        h.b.actions.setVisibility(View.GONE);
        h.b.open.setOnClickListener(
            v -> {
              saveDraft();
              requireContext()
                  .getSharedPreferences("workspace", 0)
                  .edit()
                  .putString("draft_" + business.id, chosen.id)
                  .apply();
              go("preparar");
            });
      } else {
        Publication pub = null;
        for (Publication p : publications) if (p.id.equals(id)) pub = p;
        if (pub == null) return;
        final Publication chosen = pub;
        h.b.thumbnail.setVisibility(View.VISIBLE);
        try {
          mx.chambia.app.data.Business captured =
              mx.chambia.app.publication.PublicationStore.business(pub.businessJson);
          h.b.thumbnail.bind(
              captured,
              PublicationSnapshot.decode(pub.snapshotJson, pub.businessId),
              pub.templateId,
              640,
              360,
              pub.highContrast,
              0);
        } catch (Exception ex) {
          h.b.thumbnail.setVisibility(View.GONE);
        }
        h.b.title.setText(pub.title);
        h.b.status.setText(
            pub.date
                + " · "
                + pub.destination
                + (available(files(pub)) ? "" : " · " + getString(R.string.missing_file)));
        h.b.actions.setVisibility(View.VISIBLE);
        h.b.actions.setOnClickListener(v -> actions(chosen));
        h.b.open.setOnClickListener(
            v ->
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(chosen.title)
                    .setMessage(chosen.note)
                    .setPositiveButton(R.string.close, null)
                    .show());
      }
    }
  }

  private static final class HistoryHolder extends RecyclerView.ViewHolder {
    final RowPublicationBinding b;

    HistoryHolder(RowPublicationBinding b) {
      super(b.getRoot());
      this.b = b;
    }
  }
}
