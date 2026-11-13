package mx.chambia.app.ui;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import mx.chambia.app.*;
import mx.chambia.app.data.*;
import mx.chambia.app.databinding.*;
import mx.chambia.app.publication.MealPlan;

/** Six configurable sections; saves the entire aggregate through the draft's serialized queue. */
public final class MealEditor {
  private final androidx.fragment.app.Fragment fragment;
  private final EditorMealBinding b;
  private final MealPlan plan;
  private final List<Item> items;
  private final Runnable save;
  private final java.util.function.Consumer<Item> editOffer;
  private final SectionAdapter adapter = new SectionAdapter();

  public MealEditor(
      androidx.fragment.app.Fragment f,
      ViewGroup parent,
      MealPlan p,
      List<Item> items,
      Runnable save,
      java.util.function.Consumer<Item> editOffer) {
    fragment = f;
    plan = p;
    this.items = items;
    this.save = save;
    this.editOffer = editOffer;
    b = EditorMealBinding.inflate(f.getLayoutInflater(), parent, true);
    b.enabled.setChecked(p.enabled);
    b.fields.setVisibility(p.enabled ? View.VISIBLE : View.GONE);
    b.enabled.setOnCheckedChangeListener(
        (v, c) -> {
          p.enabled = c;
          b.fields.setVisibility(c ? View.VISIBLE : View.GONE);
          save.run();
        });
    b.price.setText(
        p.priceCents < 0 ? "" : java.math.BigDecimal.valueOf(p.priceCents, 2).toPlainString());
    b.withoutPrices.setChecked(p.withoutPrices);
    b.price.setEnabled(!p.withoutPrices);
    b.withoutPrices.setOnCheckedChangeListener(
        (v, c) -> {
          p.withoutPrices = c;
          b.price.setEnabled(!c);
          b.priceLayout.setError(null);
          save.run();
        });
    b.price.addTextChangedListener(
        new SimpleTextWatcher(
            value -> {
              try {
                p.priceCents = MealPlan.parsePrice(value);
                b.priceLayout.setError(null);
                save.run();
              } catch (IllegalArgumentException ex) {
                b.priceLayout.setError(ex.getMessage());
              }
            }));
    b.notes.setText(p.notes);
    b.notes.addTextChangedListener(
        new SimpleTextWatcher(
            value -> {
              p.notes = value;
              save.run();
            }));
    b.sections.setLayoutManager(new LinearLayoutManager(f.requireContext()));
    b.sections.setAdapter(adapter);
    b.addSection.setOnClickListener(
        v -> {
          EditText name = new EditText(fragment.requireContext());
          name.setHint(R.string.meal_section_name);
          name.setMinHeight((int) (48 * fragment.getResources().getDisplayMetrics().density));
          androidx.appcompat.app.AlertDialog dialog =
              new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                      fragment.requireContext())
                  .setTitle(R.string.add_meal_section)
                  .setView(name)
                  .setNegativeButton(R.string.cancel, null)
                  .setPositiveButton(R.string.save, null)
                  .create();
          dialog.setOnShowListener(
              ignored ->
                  dialog
                      .getButton(-1)
                      .setOnClickListener(
                          clicked -> {
                            if (name.getText().toString().trim().isEmpty()) {
                              name.setError(text(R.string.meal_section_name));
                              return;
                            }
                            plan.addSection(name.getText().toString());
                            save.run();
                            adapter.notifyItemInserted(plan.sections.size() - 1);
                            dialog.dismiss();
                          }));
          dialog.show();
        });
  }

  private String text(int id) {
    return fragment.getString(id);
  }

  private void configure(MealPlan.Section s) {
    LinearLayout form = new LinearLayout(fragment.requireContext());
    form.setOrientation(1);
    int pad = (int) (16 * fragment.getResources().getDisplayMetrics().density);
    form.setPadding(pad, pad, pad, pad);
    EditText label = new EditText(fragment.requireContext());
    label.setHint(R.string.meal_section_name);
    label.setText(s.label);
    form.addView(label);
    EditText choices = new EditText(fragment.requireContext());
    choices.setHint(R.string.meal_choices);
    choices.setInputType(2);
    choices.setText(String.valueOf(s.choices));
    form.addView(choices);
    CheckBox enabled = new CheckBox(fragment.requireContext());
    enabled.setText(R.string.meal_section_enabled);
    enabled.setChecked(s.enabled);
    form.addView(enabled);
    CheckBox required = new CheckBox(fragment.requireContext());
    required.setText(R.string.meal_required);
    required.setChecked(s.required);
    form.addView(required);
    Spinner policy = new Spinner(fragment.requireContext());
    policy.setAdapter(
        new ArrayAdapter<>(
            fragment.requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              text(R.string.meal_included),
              text(R.string.meal_independent),
              text(R.string.meal_additional),
              text(R.string.meal_not_offered)
            }));
    String[] policies = {"incluido", "independiente", "adicional", "no_incluido"};
    policy.setSelection(Arrays.asList(policies).indexOf(s.policy));
    form.addView(policy);
    List<CheckBox> boxes = new ArrayList<>();
    for (Item i : items) {
      CheckBox c = new CheckBox(fragment.requireContext());
      c.setMinHeight((int) (48 * fragment.getResources().getDisplayMetrics().density));
      c.setText(
          i.name + (i.state.equals("agotado") ? fragment.getString(R.string.ui_copy_120) : ""));
      c.setChecked(s.ids.contains(i.id));
      boxes.add(c);
      form.addView(c);
    }
    ScrollView scroll = new ScrollView(fragment.requireContext());
    scroll.addView(form);
    androidx.appcompat.app.AlertDialog dialog =
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.meal_edit)
            .setView(scroll)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(
                R.string.move_up,
                (d, w) -> {
                  int index = plan.sections.indexOf(s);
                  if (index > 0) {
                    Collections.swap(plan.sections, index, index - 1);
                    save.run();
                    adapter.notifyDataSetChanged();
                  }
                })
            .setPositiveButton(R.string.save, null)
            .create();
    dialog.setOnShowListener(
        v ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    clicked -> {
                      int count;
                      try {
                        count = Integer.parseInt(choices.getText().toString());
                        if (count < 1 || count > 20) throw new NumberFormatException();
                      } catch (Exception ex) {
                        choices.setError(text(R.string.meal_choices));
                        return;
                      }
                      if (label.getText().toString().trim().isEmpty()) {
                        label.setError(text(R.string.meal_section_name));
                        return;
                      }
                      s.label = label.getText().toString().trim();
                      s.enabled = enabled.isChecked();
                      s.required = required.isChecked();
                      s.choices = count;
                      s.policy = policies[policy.getSelectedItemPosition()];
                      s.ids.clear();
                      for (int n = 0; n < items.size(); n++)
                        if (boxes.get(n).isChecked()) plan.assign(s, items.get(n).id, true);
                      save.run();
                      adapter.notifyDataSetChanged();
                      dialog.dismiss();
                    }));
    dialog.show();
  }

  private final class SectionAdapter extends RecyclerView.Adapter<SectionHolder> {
    public int getItemCount() {
      return plan.sections.size();
    }

    public SectionHolder onCreateViewHolder(ViewGroup p, int type) {
      return new SectionHolder(
          RowMealSectionBinding.inflate(fragment.getLayoutInflater(), p, false));
    }

    public void onBindViewHolder(SectionHolder h, int n) {
      MealPlan.Section s = plan.sections.get(n);
      h.b.title.setText(s.label + (s.enabled ? "" : fragment.getString(R.string.ui_copy_121)));
      List<String> names = new ArrayList<>();
      for (Item i : items) if (s.ids.contains(i.id)) names.add(i.name);
      h.b.options.setText(
          names.isEmpty() ? text(R.string.meal_empty) : android.text.TextUtils.join(" · ", names));
      h.b.edit.setOnClickListener(v -> configure(s));
      h.b.editOption.setContentDescription(fragment.getString(R.string.edit_item, s.label));
      h.b.editOption.setEnabled(!s.ids.isEmpty());
      h.b.editOption.setOnClickListener(
          v -> {
            List<Item> options = new ArrayList<>();
            for (Item i : items) if (s.ids.contains(i.id)) options.add(i);
            String[] labels = new String[options.size()];
            for (int q = 0; q < labels.length; q++) labels[q] = options.get(q).name;
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                    fragment.requireContext())
                .setTitle(R.string.edit_offer)
                .setItems(labels, (dialog, index) -> editOffer.accept(options.get(index)))
                .setNegativeButton(R.string.cancel, null)
                .show();
          });
    }
  }

  private static final class SectionHolder extends RecyclerView.ViewHolder {
    final RowMealSectionBinding b;

    SectionHolder(RowMealSectionBinding b) {
      super(b.getRoot());
      this.b = b;
    }
  }
}
