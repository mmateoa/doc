package mx.chambia.app;

import android.view.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.databinding.*;

public final class DesignsFragment extends WorkspaceFragment {
  private static final String[] IDS = {"mexicano", "minimalista", "elegante"};
  private static final int[] NAMES = {
    R.string.template_mexicano, R.string.template_minimalista, R.string.template_elegante
  };

  @Override
  protected void renderPage() {
    FragmentDesignsBinding b = FragmentDesignsBinding.inflate(getLayoutInflater(), container, true);
    android.content.SharedPreferences prefs =
        requireContext().getSharedPreferences("designs_" + business.id, 0);
    b.templates.setLayoutManager(
        new GridLayoutManager(
            requireContext(),
            getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2));
    TemplateAdapter adapter = new TemplateAdapter();
    b.templates.setAdapter(adapter);
    Runnable refresh =
        () -> {
          List<String> ids = new ArrayList<>();
          for (String id : IDS)
            if (!b.favorites.isChecked() || prefs.getBoolean("favorite_" + id, false)) ids.add(id);
          adapter.submitList(ids);
          b.empty.setVisibility(ids.isEmpty() ? View.VISIBLE : View.GONE);
        };
    b.filters.setOnCheckedStateChangeListener((group, checked) -> refresh.run());
    refresh.run();
  }

  private void detail(String id) {
    DialogTemplateBinding b = DialogTemplateBinding.inflate(getLayoutInflater());
    android.content.SharedPreferences prefs =
        requireContext().getSharedPreferences("designs_" + business.id, 0);
    try {
      b.preview.bind(business, selected(), id, 1920, 1080, accessible, 0);
    } catch (Exception ex) {
      toast(ex.getMessage());
      return;
    }
    b.favorite.setChecked(prefs.getBoolean("favorite_" + id, false));
    b.favorite.setOnCheckedChangeListener(
        (v, c) -> prefs.edit().putBoolean("favorite_" + id, c).apply());
    b.contrast.setChecked(accessible);
    b.contrast.setOnCheckedChangeListener(
        (v, c) -> {
          try {
            b.preview.bind(business, selected(), id, 1920, 1080, c, 0);
          } catch (Exception ex) {
            toast(ex.getMessage());
          }
        });
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        .setTitle(NAMES[Arrays.asList(IDS).indexOf(id)])
        .setView(b.getRoot())
        .setNegativeButton(R.string.close, (d, w) -> showPage())
        .setNeutralButton(R.string.personalize, (d, w) -> draftDetails(false))
        .setPositiveButton(
            R.string.apply,
            (d, w) -> {
              business.templateId = id;
              accessible = b.contrast.isChecked();
              requireContext()
                  .getSharedPreferences("workspace", 0)
                  .edit()
                  .putBoolean("contrast_" + business.id, accessible)
                  .apply();
              Business copy = PublicationSnapshot.copyBusiness(business);
              persist(() -> db.dao().saveBusiness(copy));
              showPage();
            })
        .show();
  }

  private final class TemplateAdapter extends ListAdapter<String, TemplateHolder> {
    TemplateAdapter() {
      super(
          new DiffUtil.ItemCallback<String>() {
            public boolean areItemsTheSame(String a, String b) {
              return a.equals(b);
            }

            public boolean areContentsTheSame(String a, String b) {
              return a.equals(b);
            }
          });
    }

    public TemplateHolder onCreateViewHolder(ViewGroup p, int type) {
      return new TemplateHolder(RowTemplateBinding.inflate(getLayoutInflater(), p, false));
    }

    public void onBindViewHolder(TemplateHolder h, int n) {
      String id = getItem(n);
      h.b.name.setText(NAMES[Arrays.asList(IDS).indexOf(id)]);
      h.b.status.setText(
          id.equals(business.templateId) ? R.string.template_in_use : R.string.template_free);
      try {
        h.b.thumbnail.bind(business, selected(), id, 640, 360, false, 0);
      } catch (Exception ex) {
        h.b.thumbnail.setVisibility(View.GONE);
      }
      h.b.detail.setOnClickListener(v -> detail(id));
    }
  }

  private static final class TemplateHolder extends RecyclerView.ViewHolder {
    final RowTemplateBinding b;

    TemplateHolder(RowTemplateBinding b) {
      super(b.getRoot());
      this.b = b;
    }
  }
}
