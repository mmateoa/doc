package mx.chambia.app;

import android.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import mx.chambia.app.data.Business;
import mx.chambia.app.data.Item;
import mx.chambia.app.data.SeedCatalog;
import org.json.JSONObject;

/** Business, catalogue and draft dialogs delegate persistence to the workspace repository. */
final class WorkspaceDialogs {
  private final WorkspaceFragment workspace;

  WorkspaceDialogs(WorkspaceFragment owner) {
    this.workspace = owner;
  }

  void quickAnnouncement() {
    EditText content = workspace.input(workspace.getString(R.string.ui_copy_16), "");
    content.setSingleLine(false);
    content.setMinLines(3);
    AlertDialog dialog =
        new AlertDialog.Builder(workspace.requireContext())
            .setTitle(workspace.getString(R.string.ui_copy_17))
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Preparar", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    view -> {
                      String value = content.getText().toString().trim();
                      if (value.isEmpty()) {
                        content.setError(workspace.getString(R.string.ui_copy_18));
                        return;
                      }
                      Item announcement = new Item();
                      announcement.id = UUID.randomUUID().toString();
                      announcement.businessId = workspace.business.id;
                      announcement.name = value;
                      announcement.category = "Anuncios";
                      announcement.priceMode = "oculto";
                      workspace.saveDraft();
                      workspace.activeDraft = new mx.chambia.app.data.Draft();
                      workspace.activeDraft.id = UUID.randomUUID().toString();
                      workspace.activeDraft.businessId = workspace.business.id;
                      workspace.activeDraft.title = "Anuncio rápido";
                      workspace.activeDraft.mode = "Promoción o anuncio";
                      workspace
                          .requireContext()
                          .getSharedPreferences("workspace", 0)
                          .edit()
                          .putString("draft_" + workspace.business.id, workspace.activeDraft.id)
                          .apply();
                      workspace.draftEditor.replace(
                          java.util.Collections.singletonList(announcement));
                      workspace.saveDraft();
                      dialog.dismiss();
                      workspace.go("vista");
                    }));
    dialog.show();
  }

  void draftDetails(boolean create) {
    LinearLayout form = workspace.vertical();
    form.setPadding(workspace.dp(16), workspace.dp(8), workspace.dp(16), 0);
    EditText title =
        workspace.input(
            workspace.getString(R.string.ui_copy_24), create ? "" : workspace.activeDraft.title);
    form.addView(title);
    EditText session =
        workspace.input(
            workspace.getString(R.string.ui_copy_25), create ? "" : workspace.activeDraft.session);
    form.addView(session);
    String[] modes = {
      "Oferta del día",
      "Carta permanente",
      "Lista de precios",
      "Promoción o anuncio",
      "Cartel informativo"
    };
    Spinner mode = workspace.spinner(modes);
    mode.setSelection(
        create ? 0 : Math.max(0, Arrays.asList(modes).indexOf(workspace.activeDraft.mode)));
    form.addView(mode);
    AlertDialog dialog =
        new AlertDialog.Builder(workspace.requireContext())
            .setTitle(
                create
                    ? workspace.getString(R.string.ui_copy_26)
                    : workspace.getString(R.string.ui_copy_27))
            .setView(workspace.scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    view -> {
                      String name = title.getText().toString().trim();
                      if (name.isEmpty()) {
                        title.setError(workspace.getString(R.string.ui_copy_28));
                        return;
                      }
                      if (create) {
                        workspace.saveDraft();
                        workspace.activeDraft = new mx.chambia.app.data.Draft();
                        workspace.activeDraft.id = UUID.randomUUID().toString();
                        workspace.activeDraft.businessId = workspace.business.id;
                        workspace.draftEditor =
                            new mx.chambia.app.publication.DraftEditor(
                                workspace.business.id, new ArrayList<>());
                        workspace
                            .requireContext()
                            .getSharedPreferences("workspace", 0)
                            .edit()
                            .putString("draft_" + workspace.business.id, workspace.activeDraft.id)
                            .apply();
                      }
                      workspace.activeDraft.title = name;
                      workspace.activeDraft.session = session.getText().toString().trim();
                      workspace.activeDraft.mode = modes[mode.getSelectedItemPosition()];
                      workspace.saveDraft();
                      dialog.dismiss();
                      workspace.showPage();
                    }));
    dialog.show();
  }

  void chooseDraft() {
    String owner = workspace.business.id;
    workspace.io.execute(
        () -> {
          try {
            List<mx.chambia.app.data.Draft> drafts = workspace.db.dao().drafts(owner);
            workspace.runOnUiThread(
                () -> {
                  if (drafts.isEmpty()) {
                    workspace.toast(workspace.getString(R.string.ui_copy_29));
                    return;
                  }
                  String[] labels = new String[drafts.size()];
                  for (int n = 0; n < labels.length; n++) {
                    mx.chambia.app.data.Draft draft = drafts.get(n);
                    labels[n] =
                        draft.title
                            + " · "
                            + draft.mode
                            + (draft.session.isEmpty() ? "" : " · " + draft.session);
                  }
                  new AlertDialog.Builder(workspace.requireContext())
                      .setTitle(workspace.getString(R.string.ui_copy_30))
                      .setItems(
                          labels,
                          (dialog, index) -> {
                            workspace.saveDraft();
                            workspace
                                .requireContext()
                                .getSharedPreferences("workspace", 0)
                                .edit()
                                .putString("draft_" + owner, drafts.get(index).id)
                                .apply();
                            workspace.loadBusiness();
                          })
                      .setNegativeButton("Cerrar", null)
                      .show();
                });
          } catch (Exception ex) {
            workspace.runOnUiThread(
                () -> workspace.toast(workspace.getString(R.string.ui_copy_31)));
          }
        });
  }

  void catalogSuggestions() {
    new AlertDialog.Builder(workspace.requireContext())
        .setTitle(workspace.getString(R.string.ui_copy_39))
        .setItems(
            workspace.SECTOR_LABELS,
            (dialog, index) -> workspace.catalogSuggestions(workspace.SECTORS[index]))
        .show();
  }

  void catalogSuggestions(String sector) {
    List<SeedCatalog.Suggestion> suggestions = SeedCatalog.load(workspace.requireContext(), sector);
    if (suggestions.isEmpty()) {
      workspace.toast(workspace.getString(R.string.ui_copy_40));
      return;
    }
    List<SeedCatalog.Suggestion> available = new ArrayList<>();
    for (SeedCatalog.Suggestion s : suggestions) {
      boolean exists = false;
      for (Item i : workspace.items)
        if (i.masterId.equals(s.masterId)) {
          exists = true;
          break;
        }
      if (!exists) available.add(s);
    }
    if (available.isEmpty()) {
      workspace.toast(workspace.getString(R.string.ui_copy_41));
      return;
    }
    mx.chambia.app.databinding.DialogCatalogSuggestionsBinding binding =
        mx.chambia.app.databinding.DialogCatalogSuggestionsBinding.inflate(
            workspace.getLayoutInflater());
    List<Item> candidates = new ArrayList<>();
    for (SeedCatalog.Suggestion suggestion : available)
      candidates.add(SeedCatalog.toItem(suggestion, workspace.business.id));
    java.util.Set<String> selectedIds = new java.util.HashSet<>();
    List<Item> visible = new ArrayList<>();
    List<String> categories = new ArrayList<>();
    categories.add(workspace.getString(R.string.all_categories));
    for (Item i : candidates) if (!categories.contains(i.category)) categories.add(i.category);
    binding.category.setAdapter(
        new ArrayAdapter<>(
            workspace.requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
    binding.category.setContentDescription(workspace.getString(R.string.catalog_title));
    androidx.appcompat.app.AlertDialog dialog =
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                workspace.requireContext())
            .setTitle(R.string.suggestions_title)
            .setView(binding.getRoot())
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(workspace.getString(R.string.add_suggestions_count, 0), null)
            .create();
    mx.chambia.app.ui.CatalogItemAdapter adapter =
        new mx.chambia.app.ui.CatalogItemAdapter(
            true,
            false,
            new mx.chambia.app.ui.CatalogItemAdapter.Events() {
              public boolean selected(Item i) {
                return selectedIds.contains(i.id);
              }

              public void edit(Item i) {}

              public void toggle(Item i, boolean checked) {
                if (checked) selectedIds.add(i.id);
                else selectedIds.remove(i.id);
                if (dialog.getButton(-1) != null) {
                  dialog
                      .getButton(-1)
                      .setText(
                          workspace.getString(R.string.add_suggestions_count, selectedIds.size()));
                  dialog.getButton(-1).setEnabled(!selectedIds.isEmpty());
                }
              }
            });
    binding.items.setLayoutManager(
        new androidx.recyclerview.widget.LinearLayoutManager(workspace.requireContext()));
    binding.items.setAdapter(adapter);
    Runnable refresh =
        () -> {
          String query = binding.search.getText().toString().toLowerCase(Locale.ROOT);
          int category = binding.category.getSelectedItemPosition();
          visible.clear();
          for (Item i : candidates)
            if ((category <= 0 || i.category.equals(categories.get(category)))
                && (i.name + " " + i.category).toLowerCase(Locale.ROOT).contains(query))
              visible.add(i);
          adapter.submitList(new ArrayList<>(visible));
        };
    binding.search.addTextChangedListener(new SimpleTextWatcher(value -> refresh.run()));
    binding.category.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> parent) {}

          public void onItemSelected(
              android.widget.AdapterView<?> parent, View view, int position, long id) {
            refresh.run();
          }
        });
    binding.selectVisible.setOnClickListener(
        v -> {
          boolean all = !visible.isEmpty();
          for (Item i : visible) if (!selectedIds.contains(i.id)) all = false;
          for (Item i : visible)
            if (all) selectedIds.remove(i.id);
            else selectedIds.add(i.id);
          adapter.notifyDataSetChanged();
          dialog
              .getButton(-1)
              .setText(workspace.getString(R.string.add_suggestions_count, selectedIds.size()));
          dialog.getButton(-1).setEnabled(!selectedIds.isEmpty());
        });
    dialog.setOnShowListener(
        v -> {
          dialog.getButton(-1).setEnabled(false);
          dialog
              .getButton(-1)
              .setOnClickListener(
                  clicked -> {
                    List<Item> added = new ArrayList<>();
                    for (Item i : candidates)
                      if (selectedIds.contains(i.id)) {
                        workspace.items.add(i);
                        added.add(i);
                      }
                    workspace.persist(
                        () ->
                            workspace.db.runInTransaction(
                                () -> {
                                  for (Item i : added) workspace.db.dao().saveItem(i);
                                }));
                    dialog.dismiss();
                    workspace.toast(workspace.getString(R.string.suggestions_added, added.size()));
                    workspace.showPage();
                  });
          refresh.run();
        });
    dialog.show();
  }

  void editItem(@Nullable Item original) {
    workspace.editItem(original, false);
  }

  void editItem(@Nullable Item original, boolean offerOnly) {
    boolean newItem = original == null;
    Item target =
        newItem
            ? new Item()
            : PublicationSnapshot.copyItems(java.util.Collections.singletonList(original)).get(0);
    LinearLayout form = workspace.vertical();
    form.setPadding(workspace.dp(16), workspace.dp(8), workspace.dp(16), 0);
    EditText name = workspace.input("Nombre", target.name);
    form.addView(name);
    EditText category = workspace.input(workspace.getString(R.string.ui_copy_45), target.category);
    form.addView(category);
    EditText description =
        workspace.input(workspace.getString(R.string.ui_copy_46), target.description);
    description.setSingleLine(false);
    description.setMinLines(2);
    form.addView(description);
    EditText variants = workspace.input(workspace.getString(R.string.ui_copy_47), target.variants);
    variants.setSingleLine(false);
    variants.setMinLines(3);
    form.addView(variants);
    EditText bundle =
        workspace.input(workspace.getString(R.string.ui_copy_48), target.bundleContents);
    bundle.setSingleLine(false);
    bundle.setMinLines(3);
    form.addView(bundle);
    EditText unit = workspace.input(workspace.getString(R.string.ui_copy_49), target.unit);
    form.addView(unit);
    EditText price =
        workspace.input(
            workspace.getString(R.string.ui_copy_50),
            target.price == 0 ? "" : String.valueOf(target.price));
    price.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    form.addView(price);
    Spinner pricing =
        workspace.spinner(
            new String[] {
              workspace.getString(R.string.ui_copy_51),
              workspace.getString(R.string.ui_copy_52),
              "Cotizar",
              workspace.getString(R.string.ui_copy_53),
              workspace.getString(R.string.ui_copy_54)
            });
    pricing.setSelection(
        target.priceMode.equals("desde")
            ? 1
            : target.priceMode.equals("cotizar")
                ? 2
                : target.priceMode.equals("confirmar")
                    ? 3
                    : target.priceMode.equals("oculto") ? 4 : 0);
    form.addView(pricing);
    Spinner status =
        workspace.spinner(
            new String[] {
              "Disponible",
              workspace.getString(R.string.ui_copy_55),
              workspace.getString(R.string.ui_copy_56)
            });
    status.setSelection(target.state.equals("agotado") ? 1 : target.state.equals("fuera") ? 2 : 0);
    form.addView(status);
    AlertDialog dialog =
        new AlertDialog.Builder(workspace.requireContext())
            .setTitle(
                offerOnly
                    ? workspace.getString(R.string.ui_copy_57)
                    : newItem
                        ? workspace.getString(R.string.ui_copy_58)
                        : workspace.getString(R.string.ui_copy_59))
            .setView(workspace.scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create();
    dialog.setOnShowListener(
        x -> {
          dialog
              .getButton(-1)
              .setOnClickListener(
                  v -> {
                    String n = name.getText().toString().trim();
                    double amount = 0;
                    try {
                      String raw = price.getText().toString().trim();
                      if (!raw.isEmpty()) amount = Double.parseDouble(raw);
                    } catch (Exception ex) {
                      price.setError(workspace.getString(R.string.ui_copy_60));
                      return;
                    }
                    if (n.isEmpty()) {
                      name.setError(workspace.getString(R.string.ui_copy_61));
                      return;
                    }
                    if (!Double.isFinite(amount) || amount < 0 || amount > 999999) {
                      price.setError(workspace.getString(R.string.ui_copy_62));
                      return;
                    }
                    try {
                      mx.chambia.app.data.ItemOptions.variants(variants.getText().toString());
                    } catch (IllegalArgumentException ex) {
                      variants.setError(ex.getMessage());
                      return;
                    }
                    target.variants = variants.getText().toString().trim();
                    target.bundleContents = bundle.getText().toString().trim();
                    if (newItem) {
                      target.id = UUID.randomUUID().toString();
                      target.businessId = workspace.business.id;
                      workspace.items.add(target);
                    }
                    target.description = description.getText().toString().trim();
                    target.name = n;
                    target.unit = unit.getText().toString().trim();
                    target.category = category.getText().toString().trim();
                    target.price = amount;
                    target.priceMode =
                        new String[] {"fijo", "desde", "cotizar", "confirmar", "oculto"}
                            [pricing.getSelectedItemPosition()];
                    if (price.getText().toString().trim().isEmpty()
                        && pricing.getSelectedItemPosition() < 2) target.priceMode = "confirmar";
                    target.state =
                        new String[] {"disponible", "agotado", "fuera"}
                            [status.getSelectedItemPosition()];
                    if (offerOnly) {
                      workspace.draftEditor.update(target);
                      workspace.saveDraft();
                    } else {
                      if (!newItem) {
                        for (int index = 0; index < workspace.items.size(); index++)
                          if (workspace.items.get(index).id.equals(target.id)) {
                            workspace.items.set(index, target);
                            break;
                          }
                      }
                      workspace.persist(() -> workspace.db.dao().saveItem(target));
                    }
                    dialog.dismiss();
                    workspace.showPage();
                  });
        });
    dialog.show();
  }

  void businessDialog() {
    LinearLayout form = workspace.vertical();
    form.setPadding(workspace.dp(16), workspace.dp(8), workspace.dp(16), 0);
    EditText name = workspace.input(workspace.getString(R.string.ui_copy_63), "");
    form.addView(name);
    Spinner sector = workspace.spinner(workspace.SECTOR_LABELS);
    form.addView(sector);
    Spinner mode =
        workspace.spinner(
            new String[] {
              "Local fijo", "Tianguis", "A domicilio", "Móvil", "En línea", "Temporal"
            });
    form.addView(mode);
    AlertDialog dialog =
        new AlertDialog.Builder(workspace.requireContext())
            .setTitle(workspace.getString(R.string.ui_copy_64))
            .setView(form)
            .setCancelable(!workspace.businesses.isEmpty())
            .setNegativeButton(
                workspace.businesses.isEmpty() ? "" : "Cancelar",
                (d, w) -> {
                  if (workspace.businesses.isEmpty()) workspace.createFirstBusiness();
                })
            .setPositiveButton("Crear", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String n = name.getText().toString().trim();
                      if (n.isEmpty()) {
                        name.setError(workspace.getString(R.string.ui_copy_65));
                        return;
                      }
                      Business b = new Business();
                      b.id = UUID.randomUUID().toString();
                      b.name = n;
                      b.sector = workspace.SECTORS[sector.getSelectedItemPosition()];
                      b.modality = String.valueOf(mode.getSelectedItem());
                      workspace.businesses.add(b);
                      workspace.business = b;
                      workspace.persist(() -> workspace.db.dao().saveBusiness(b));
                      dialog.dismiss();
                      workspace
                          .requireContext()
                          .getSharedPreferences("workspace", 0)
                          .edit()
                          .putString("lastBusiness", b.id)
                          .apply();
                      workspace.model.businesses.setValue(new ArrayList<>(workspace.businesses));
                      workspace.go("catalogo");
                      workspace.toast(workspace.getString(R.string.ui_copy_66));
                    }));
    dialog.show();
  }

  void manageBusinesses() {
    workspace.io.execute(
        () -> {
          try {
            List<Business> all = workspace.db.dao().allBusinesses();
            workspace.runOnUiThread(
                () -> {
                  String[] labels = new String[all.size()];
                  for (int n = 0; n < all.size(); n++)
                    labels[n] =
                        all.get(n).name
                            + (all.get(n).archived ? workspace.getString(R.string.ui_copy_67) : "");
                  new AlertDialog.Builder(workspace.requireContext())
                      .setTitle(workspace.getString(R.string.ui_copy_68))
                      .setItems(
                          labels,
                          (dialog, index) -> {
                            Business chosen = all.get(index);
                            new AlertDialog.Builder(workspace.requireContext())
                                .setTitle(chosen.name)
                                .setItems(
                                    new String[] {
                                      chosen.archived ? "Reactivar" : "Archivar",
                                      workspace.getString(R.string.ui_copy_69)
                                    },
                                    (options, action) -> {
                                      if (action == 0) {
                                        workspace.io.execute(
                                            () -> {
                                              workspace
                                                  .db
                                                  .dao()
                                                  .archiveBusiness(chosen.id, !chosen.archived);
                                              workspace.model.loadBusinesses();
                                            });
                                      } else
                                        new AlertDialog.Builder(workspace.requireContext())
                                            .setTitle(
                                                workspace.getString(R.string.ui_copy_70)
                                                    + chosen.name
                                                    + "?")
                                            .setMessage(workspace.getString(R.string.ui_copy_71))
                                            .setNegativeButton("Cancelar", null)
                                            .setNeutralButton(
                                                workspace.getString(R.string.ui_copy_72),
                                                (confirm, which) ->
                                                    workspace.removeBusiness(chosen, false))
                                            .setPositiveButton(
                                                workspace.getString(R.string.ui_copy_73),
                                                (confirm, which) ->
                                                    workspace.removeBusiness(chosen, true))
                                            .show();
                                    })
                                .show();
                          })
                      .setNegativeButton("Cerrar", null)
                      .show();
                });
          } catch (Exception e) {
            workspace.runOnUiThread(
                () -> workspace.toast(workspace.getString(R.string.ui_copy_74)));
          }
        });
  }

  void editBusiness() {
    if (workspace.business == null) return;
    LinearLayout form = workspace.vertical();
    form.setPadding(workspace.dp(16), workspace.dp(8), workspace.dp(16), 0);
    JSONObject profile;
    try {
      profile = new JSONObject(workspace.business.profileJson);
    } catch (Exception ex) {
      workspace.toast(workspace.getString(R.string.ui_copy_77));
      return;
    }
    EditText
        name = workspace.input(workspace.getString(R.string.ui_copy_78), workspace.business.name),
        phone = workspace.input(workspace.getString(R.string.ui_copy_79), workspace.business.phone),
        slogan =
            workspace.input(workspace.getString(R.string.ui_copy_80), workspace.business.slogan);
    EditText
        address =
            workspace.input(
                workspace.getString(R.string.ui_copy_81), profile.optString("address", "")),
        hours = workspace.input("Horario", profile.optString("hours", "")),
        networks =
            workspace.input(
                workspace.getString(R.string.ui_copy_82), profile.optString("networks", ""));
    EditText
        sectors =
            workspace.input(
                workspace.getString(R.string.ui_copy_83),
                profile.optString("sectors", workspace.business.sector)),
        modes =
            workspace.input(workspace.getString(R.string.ui_copy_84), workspace.business.modality);
    for (EditText field :
        new EditText[] {name, phone, slogan, address, hours, networks, sectors, modes})
      form.addView(field);
    AlertDialog dialog =
        new AlertDialog.Builder(workspace.requireContext())
            .setTitle(workspace.getString(R.string.ui_copy_85))
            .setView(workspace.scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    view -> {
                      if (name.getText().toString().trim().isEmpty()) {
                        name.setError(workspace.getString(R.string.ui_copy_86));
                        return;
                      }
                      try {
                        profile.put("address", address.getText().toString().trim());
                        profile.put("hours", hours.getText().toString().trim());
                        profile.put("networks", networks.getText().toString().trim());
                        profile.put("sectors", sectors.getText().toString().trim());
                      } catch (Exception ex) {
                        workspace.toast(workspace.getString(R.string.ui_copy_87));
                        return;
                      }
                      workspace.business.name = name.getText().toString().trim();
                      workspace.business.phone = phone.getText().toString().trim();
                      workspace.business.slogan = slogan.getText().toString().trim();
                      workspace.business.modality = modes.getText().toString().trim();
                      workspace.business.profileJson = profile.toString();
                      Business captured = PublicationSnapshot.copyBusiness(workspace.business);
                      workspace.persist(() -> workspace.db.dao().saveBusiness(captured));
                      dialog.dismiss();
                      workspace.showPage();
                    }));
    dialog.show();
  }
}
