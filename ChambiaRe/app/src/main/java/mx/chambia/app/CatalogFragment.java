package mx.chambia.app;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.*;
import mx.chambia.app.data.*;
import mx.chambia.app.databinding.FragmentCatalogBinding;
import mx.chambia.app.ui.CatalogItemAdapter;

public final class CatalogFragment extends WorkspaceFragment {
  private String query = "", category = "Todas";

  @Override
  protected void renderPage() {
    boolean picking = requireArguments().getBoolean("selecting", false);
    FragmentCatalogBinding b = FragmentCatalogBinding.inflate(getLayoutInflater(), container, true);
    if (picking) {
      b.title.setText(R.string.select_title);
      b.help.setText(R.string.preparation_help);
      shell.navigation.setVisibility(View.GONE);
      shell.taskToolbar.setNavigationIcon(R.drawable.ic_back);
      shell.taskToolbar.setNavigationOnClickListener(
          v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }
    b.done.setVisibility(picking ? View.VISIBLE : View.GONE);
    b.done.setOnClickListener(v -> go("preparar"));
    b.suggestions.setOnClickListener(v -> catalogSuggestions());
    b.add.setOnClickListener(v -> editItem(null));
    android.content.SharedPreferences prefs =
        requireContext().getSharedPreferences("catalog_ui_" + business.id, 0);
    query = prefs.getString("query", query);
    category = prefs.getString("category", category);
    CatalogItemAdapter adapter =
        new CatalogItemAdapter(
            picking,
            new CatalogItemAdapter.Events() {
              public boolean selected(Item i) {
                return draftEditor.includes(i.id);
              }

              public void toggle(Item i, boolean c) {
                if (c) draftEditor.include(i);
                else draftEditor.remove(i.id);
                saveDraft();
              }

              public void edit(Item i) {
                editItem(i);
              }
            });
    b.items.setLayoutManager(new LinearLayoutManager(requireContext()));
    b.items.setAdapter(adapter);
    List<String> categories = new ArrayList<>();
    categories.add("Todas");
    for (Item i : items) if (!categories.contains(i.category)) categories.add(i.category);
    b.category.setAdapter(
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
    b.category.setContentDescription(getString(R.string.catalog_title));
    b.category.setSelection(Math.max(0, categories.indexOf(category)));
    Runnable refresh =
        () -> {
          List<Item> found = new ArrayList<>();
          for (Item i : items)
            if ((category.equals("Todas") || category.equals(i.category))
                && (i.name + " " + i.category)
                    .toLowerCase(Locale.ROOT)
                    .contains(query.toLowerCase(Locale.ROOT))) found.add(i);
          adapter.submitList(PublicationSnapshot.copyItems(found));
          b.empty.setVisibility(found.isEmpty() ? View.VISIBLE : View.GONE);
          prefs.edit().putString("query", query).putString("category", category).apply();
        };
    b.search.setText(query);
    b.search.addTextChangedListener(
        new SimpleTextWatcher(
            value -> {
              query = value;
              refresh.run();
            }));
    b.category.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
            category = categories.get(position);
            refresh.run();
          }
        });
    refresh.run();
  }
}
