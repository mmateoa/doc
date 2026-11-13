package mx.chambia.app;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import mx.chambia.app.data.Item;
import mx.chambia.app.databinding.FragmentPreparationBinding;
import mx.chambia.app.ui.CatalogItemAdapter;

public final class PreparationFragment extends WorkspaceFragment {
  @Override
  protected void renderPage() {
    FragmentPreparationBinding b =
        FragmentPreparationBinding.inflate(getLayoutInflater(), container, true);
    b.title.setText(activeDraft.title);
    ((android.view.ViewGroup) b.review.getParent()).removeView(b.review);
    shell.taskActions.addView(b.review);
    shell.taskActions.setVisibility(View.VISIBLE);
    b.details.setOnClickListener(v -> draftDetails(false));
    b.newDraft.setOnClickListener(v -> draftDetails(true));
    b.add.setOnClickListener(v -> go("seleccionar"));
    if ("fonda".equals(business.sector)) {
      final mx.chambia.app.publication.MealPlan plan =
          mx.chambia.app.publication.MealPlan.decode(activeDraft.mealJson);
      new mx.chambia.app.ui.MealEditor(
          this,
          b.mealEditor,
          plan,
          draftEditor.entries(),
          () -> {
            activeDraft.mealJson = plan.encode();
            saveDraft();
            b.items.setVisibility(plan.enabled ? View.GONE : View.VISIBLE);
          },
          item -> editItem(item, true));
      b.items.setVisibility(plan.enabled ? View.GONE : View.VISIBLE);
    }
    b.empty.setVisibility(selected().isEmpty() ? View.VISIBLE : View.GONE);
    b.items.setLayoutManager(new LinearLayoutManager(requireContext()));
    CatalogItemAdapter adapter =
        new CatalogItemAdapter(
            false,
            new CatalogItemAdapter.Events() {
              public boolean selected(Item i) {
                return true;
              }

              public void toggle(Item i, boolean c) {}

              public void edit(Item i) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(i.name)
                    .setItems(
                        new String[] {
                          getString(R.string.edit_offer),
                          getString(R.string.move_up),
                          getString(R.string.move_down),
                          getString(R.string.remove_offer)
                        },
                        (d, index) -> {
                          if (index == 0) editItem(i, true);
                          else {
                            if (index == 3) draftEditor.remove(i.id);
                            else draftEditor.move(i.id, index == 1 ? -1 : 1);
                            saveDraft();
                            showPage();
                          }
                        })
                    .show();
              }
            });
    b.items.setAdapter(adapter);
    adapter.submitList(draftEditor.entries());
    b.review.setOnClickListener(
        v -> {
          com.google.android.material.textfield.TextInputLayout priceField =
              b.mealEditor.findViewById(R.id.price_layout);
          if (priceField != null && priceField.getError() != null) {
            priceField.requestFocus();
            toast(priceField.getError().toString());
            return;
          }
          if (selected().isEmpty()) {
            toast(getString(R.string.offer_empty));
            return;
          }
          if (validatePublication()) go("vista");
        });
  }
}
