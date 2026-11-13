package mx.chambia.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import mx.chambia.app.data.AppDatabase;
import mx.chambia.app.data.Business;
import mx.chambia.app.data.Item;
import mx.chambia.app.data.Publication;
import mx.chambia.app.design.TemplateEngine;
import mx.chambia.app.export.ExportManager;
import org.json.JSONObject;

/** CHAMB-IA: prototipo nativo funcional, offline y sin credenciales externas. */
public abstract class WorkspaceFragment extends androidx.fragment.app.Fragment {
  private final WorkspaceDialogs dialogs = new WorkspaceDialogs(this);
  private final WorkspaceExports exports = new WorkspaceExports(this);
  protected int TEAL = Color.rgb(194, 65, 12),
      INK = Color.rgb(31, 41, 55),
      LIGHT = Color.rgb(255, 251, 247),
      WHITE = Color.WHITE;
  protected int VIOLET = Color.rgb(124, 58, 237), BORDER = Color.rgb(232, 226, 220);
  protected static final int USB_REQUEST = 211;
  protected static final String[] SECTORS = {
    "fonda", "pizzeria", "tortilleria", "taller", "tianguis", "barberia", "servicios"
  };
  protected String[] SECTOR_LABELS;
  protected ExecutorService io;
  protected WorkspaceViewModel model;
  protected android.view.LayoutInflater inflater;
  protected ViewGroup host;
  protected AppDatabase db;
  protected final List<Business> businesses = new ArrayList<>();
  protected final List<Item> items = new ArrayList<>();
  protected final List<Publication> publications = new ArrayList<>();
  protected Business business;
  protected mx.chambia.app.publication.DraftEditor draftEditor;
  protected String observedWork = "";

  protected TextView draftStatus;
  protected mx.chambia.app.data.Draft activeDraft;
  protected LinearLayout root, container, toolbar;
  protected mx.chambia.app.databinding.FragmentWorkspaceBinding shell;
  protected String page = "inicio", format = "JPG", destination = "TV";
  protected int previewPage = 0;
  private String renderedKey = "";
  protected boolean accessible = false, narrated = false, updatingSpinner = false;
  protected File lastExport, pendingUsb;
  protected final ArrayList<File> pendingFiles = new ArrayList<>();
  protected String pendingBusiness = "", pendingPublication = "";
  protected TextToSpeech tts;
  protected boolean ttsReady = false;
  protected mx.chambia.app.accessibility.NarrationExporter audioJob;

  @Override
  public View onCreateView(android.view.LayoutInflater inflater, ViewGroup parent, Bundle state) {
    observedWork = "";
    SECTOR_LABELS =
        new String[] {
          getString(R.string.sector_label_0),
          getString(R.string.sector_label_1),
          getString(R.string.sector_label_2),
          getString(R.string.sector_label_3),
          getString(R.string.sector_label_4),
          getString(R.string.sector_label_5),
          getString(R.string.sector_label_6)
        };
    TEAL = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
    INK = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary);
    LIGHT = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.background);
    WHITE = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface);
    BORDER = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.outline);
    host = new android.widget.FrameLayout(requireContext());
    model =
        new androidx.lifecycle.ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
    db = model.repository.database;
    io = model.repository.io;
    page = requireArguments().getString("page", "inicio");
    format = requireArguments().getString("format", "JPG");
    destination = requireArguments().getString("destination", "TV");
    android.content.SharedPreferences settings =
        requireContext().getSharedPreferences("workspace", 0);
    accessible = settings.getBoolean("contrast", false);
    narrated = settings.getBoolean("narrated", false);
    if (state != null) {
      ArrayList<String> paths = state.getStringArrayList("pendingFiles");
      if (paths != null) for (String path : paths) pendingFiles.add(new File(path));
      format = state.getString("format", "JPG");
      destination = state.getString("destination", "TV");
      String pending = state.getString("pending");
      if (pending != null) pendingUsb = new File(pending);
      pendingBusiness = state.getString("pendingBusiness", "");
      pendingPublication = state.getString("pendingPublication", "");
    }
    drawShell(getString(R.string.ui_copy_7));
    tts =
        new TextToSpeech(
            requireContext(),
            status -> {
              ttsReady = status == TextToSpeech.SUCCESS;
              if (ttsReady && tts != null) {
                int language = tts.setLanguage(new Locale("es", "MX"));
                ttsReady = language >= TextToSpeech.LANG_AVAILABLE;
              }
            });
    return host;
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    super.onViewCreated(view, state);
    model.exporting.observe(
        getViewLifecycleOwner(),
        busy -> {
          if (Boolean.TRUE.equals(busy))
            view.announceForAccessibility(getString(R.string.ui_copy_8));
        });
    model.error.observe(getViewLifecycleOwner(), this::toast);
    model.draftSaveState.observe(
        getViewLifecycleOwner(),
        stateValue -> {
          if (draftStatus != null
              && business != null
              && business.id.equals(stateValue.businessId)) {
            draftStatus.setText(stateValue.message);
            draftStatus.announceForAccessibility(stateValue.message);
          }
        });
    model.businesses.observe(
        getViewLifecycleOwner(),
        result -> {
          businesses.clear();
          businesses.addAll(result);
          if (businesses.isEmpty()) {
            business = null;
            items.clear();
            publications.clear();
            drawShell(getString(R.string.ui_copy_9));
            createFirstBusiness();
            return;
          }
          String previous =
              requireContext()
                  .getSharedPreferences("workspace", 0)
                  .getString(
                      "lastBusiness",
                      requireActivity().getPreferences(0).getString("lastBusiness", ""));
          business = null;
          for (Business candidate : businesses)
            if (candidate.id.equals(previous)) {
              business = candidate;
              break;
            }
          if (business == null) business = businesses.get(0);
          loadBusiness();
        });
    model.content.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result == null
              || business == null
              || !business.id.equals(result.businessId)
              || !model.activeDraftId(business.id).equals(result.draftId)) return;
          items.clear();
          items.addAll(result.items);
          publications.clear();
          publications.addAll(result.publications);
          activeDraft = result.draft;
          if (activeDraft == null) {
            activeDraft = new mx.chambia.app.data.Draft();
            activeDraft.businessId = business.id;
            activeDraft.id = model.activeDraftId(business.id);
          }
          try {
            draftEditor =
                new mx.chambia.app.publication.DraftEditor(
                    business.id,
                    result.draft == null
                        ? new ArrayList<>()
                        : PublicationSnapshot.decode(result.draft.snapshotJson, business.id));
          } catch (Exception ex) {
            drawShell(getString(R.string.ui_copy_10));
            return;
          }
          business.mealJson = activeDraft.mealJson;
          business.publicationHeading = activeDraft.mode;
          business.publicationSession = activeDraft.session;
          observeExport();
          showPage();
        });
    model.loadBusinesses();
  }

  protected void createFirstBusiness() {
    new AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.ui_copy_11))
        .setMessage(getString(R.string.ui_copy_12))
        .setPositiveButton("Comenzar", (d, w) -> businessDialog())
        .setNeutralButton(getString(R.string.ui_copy_13), (d, w) -> manageBusinesses())
        .setCancelable(false)
        .show();
  }

  protected void persist(Runnable op) {
    io.execute(
        () -> {
          try {
            op.run();
          } catch (Exception e) {
            runOnUiThread(() -> toast(getString(R.string.ui_copy_14) + e.getMessage()));
          }
        });
  }

  protected void loadBusiness() {
    if (business == null) return;
    android.content.SharedPreferences settings =
        requireContext().getSharedPreferences("workspace", 0);
    settings.edit().putString("lastBusiness", business.id).apply();
    accessible = settings.getBoolean("contrast_" + business.id, false);
    narrated = settings.getBoolean("narrated_" + business.id, false);
    draftEditor = null;
    items.clear();
    publications.clear();
    drawShell(getString(R.string.ui_copy_15));
    model.loadBusiness(business.id);
  }

  protected void drawShell(String label) {
    saveUiState();
    renderedKey = "";
    shell =
        mx.chambia.app.databinding.FragmentWorkspaceBinding.inflate(
            getLayoutInflater(), host, false);
    host.removeAllViews();
    host.addView(shell.getRoot());
    root = shell.getRoot();
    container = shell.content;
    if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
      android.widget.FrameLayout.LayoutParams params =
          new android.widget.FrameLayout.LayoutParams(
              Math.min(getResources().getDisplayMetrics().widthPixels, dp(840)), -2);
      params.gravity = Gravity.CENTER_HORIZONTAL;
      container.setLayoutParams(params);
      container.setPadding(dp(24), dp(16), dp(24), dp(16));
    }
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  androidx.core.view.WindowInsetsCompat.Type.systemBars()
                      | androidx.core.view.WindowInsetsCompat.Type.ime());
          view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
    androidx.core.view.ViewCompat.requestApplyInsets(root);
    container.addView(text(label, 16, false));
  }

  protected void showPage() {
    drawShell("");
    container.removeAllViews();
    boolean primary =
        Arrays.asList("inicio", "catalogo", "publicaciones", "disenos").contains(page);
    shell.taskToolbar.setTitle(primary ? getString(R.string.app_name) : pageTitle());
    if (!primary) {
      shell.taskToolbar.setNavigationIcon(R.drawable.ic_back);
      shell.taskToolbar.setNavigationContentDescription(R.string.back);
      shell.taskToolbar.setNavigationOnClickListener(
          v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }
    shell.businessSwitcher.setText(business.name + "  ▾");
    shell.businessSwitcher.setOnClickListener(v -> chooseBusiness());
    draftStatus = text("", 12, false);
    draftStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    shell.navigation.setVisibility(primary ? View.VISIBLE : View.GONE);
    if (primary) {
      shell.navigation.getMenu().findItem(destinationId(page)).setChecked(true);
      shell.navigation.setOnItemSelectedListener(
          item -> {
            String next =
                item.getItemId() == R.id.catalog
                    ? "catalogo"
                    : item.getItemId() == R.id.history
                        ? "publicaciones"
                        : item.getItemId() == R.id.designs ? "disenos" : "inicio";
            if (!page.equals(next)) go(next);
            return true;
          });
    }
    String screenKey = business.id + ":" + activeDraft.id + ":" + page;
    Integer rememberedPage = model.uiState.get("page:" + screenKey);
    if (rememberedPage != null) previewPage = rememberedPage;
    String rememberedDestination = model.uiState.get("destination:" + screenKey);
    if (rememberedDestination != null) destination = rememberedDestination;
    renderPage();
    renderedKey = business.id + ":" + activeDraft.id + ":" + page;
    String key = renderedKey;
    Integer y = model.uiState.get("scroll:" + key);
    shell.scroll.post(
        () -> {
          if (shell != null && key.equals(renderedKey)) shell.scroll.scrollTo(0, y == null ? 0 : y);
        });
  }

  protected String pageTitle() {
    switch (page) {
      case "preparar":
        return getString(R.string.prepare_title);
      case "vista":
        return getString(R.string.preview_title);
      case "exportar":
        return getString(R.string.export_title);
      case "accesible":
        return getString(R.string.accessibility_title);
      default:
        return getString(R.string.app_name);
    }
  }

  protected void chooseBusiness() {
    List<String> labels = new ArrayList<>();
    for (Business b : businesses) labels.add(b.name);
    labels.add(getString(R.string.add_business));
    labels.add(getString(R.string.manage_businesses));
    new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext()) {
      {
        android.widget.ListView list = new android.widget.ListView(getContext());
        list.setAdapter(
            new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, labels));
        setContentView(list);
        list.setOnItemClickListener(
            (p, v, index, id) -> {
              dismiss();
              if (index == businesses.size()) businessDialog();
              else if (index > businesses.size()) manageBusinesses();
              else if (!business.id.equals(businesses.get(index).id)) {
                saveDraft();
                business = businesses.get(index);
                requireContext()
                    .getSharedPreferences("workspace", 0)
                    .edit()
                    .putString("lastBusiness", business.id)
                    .apply();
                go("inicio");
              }
            });
      }
    }.show();
  }

  protected abstract void renderPage();

  protected void go(String next) {
    Bundle args = new Bundle();
    if (next.equals("seleccionar")) {
      next = "catalogo";
      args.putBoolean("selecting", true);
    }
    args.putString("page", next);
    args.putString("format", format);
    args.putString("destination", destination);
    androidx.navigation.NavController nav =
        androidx.navigation.fragment.NavHostFragment.findNavController(this);
    androidx.navigation.NavOptions.Builder options =
        new androidx.navigation.NavOptions.Builder().setLaunchSingleTop(true);
    if (!args.getBoolean("selecting", false)
        && Arrays.asList("inicio", "catalogo", "publicaciones", "disenos").contains(next))
      options.setPopUpTo(R.id.home, false);
    nav.navigate(destinationId(next), args, options.build());
  }

  protected int destinationId(String next) {
    switch (next) {
      case "catalogo":
        return R.id.catalog;
      case "publicaciones":
        return R.id.history;
      case "disenos":
        return R.id.designs;
      case "preparar":
        return R.id.prepare;
      case "vista":
        return R.id.preview;
      case "accesible":
        return R.id.accessibility;
      case "exportar":
        return R.id.export;
      default:
        return R.id.home;
    }
  }

  protected void runOnUiThread(Runnable action) {
    new android.os.Handler(android.os.Looper.getMainLooper())
        .post(
            () -> {
              if (isAdded() && getView() != null) action.run();
            });
  }

  protected void quickAnnouncement() {
    dialogs.quickAnnouncement();
  }

  protected String prompt() {
    switch (business.sector) {
      case "fonda":
        return getString(R.string.ui_copy_19);
      case "taller":
        return getString(R.string.ui_copy_20);
      case "tianguis":
        return getString(R.string.ui_copy_21);
      case "tortilleria":
        return getString(R.string.ui_copy_22);
      default:
        return getString(R.string.ui_copy_23);
    }
  }

  protected void draftDetails(boolean create) {
    dialogs.draftDetails(create);
  }

  protected void chooseDraft() {
    dialogs.chooseDraft();
  }

  protected void saveDraft() {
    if (business != null && draftEditor != null) {
      business.mealJson = activeDraft.mealJson;
      business.publicationHeading = activeDraft.mode;
      business.publicationSession = activeDraft.session;
      model.saveDraft(activeDraft, draftEditor.entries());
    }
  }

  protected void addDesignPreview(String id) {
    try {
      TemplateEngine engine = new TemplateEngine(requireContext(), id);
      List<Item> first = engine.pages(business, selected(), 1920, 1080).get(0);
      View preview =
          new View(requireContext()) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
              int w = View.MeasureSpec.getSize(widthSpec);
              setMeasuredDimension(w, w * 9 / 16);
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
              try {
                engine.draw(canvas, business, first, getWidth(), getHeight(), accessible);
              } catch (RuntimeException e) {
                android.graphics.Paint paint = new android.graphics.Paint();
                paint.setTextSize(20);
                canvas.drawText(getString(R.string.ui_copy_32), 20, 40, paint);
              }
            }
          };
      preview.setContentDescription(
          getString(R.string.ui_copy_33) + id + getString(R.string.ui_copy_34));
      container.addView(preview, match());
    } catch (Exception e) {
      hint(getString(R.string.ui_copy_35) + e.getMessage());
    }
  }

  protected void regenerate(Publication publication) {
    exports.regenerate(publication);
  }

  protected void catalogSuggestions() {
    dialogs.catalogSuggestions();
  }

  protected void catalogSuggestions(String sector) {
    dialogs.catalogSuggestions(sector);
  }

  protected void editItem(@Nullable Item original) {
    dialogs.editItem(original);
  }

  protected void editItem(@Nullable Item original, boolean offerOnly) {
    dialogs.editItem(original, offerOnly);
  }

  protected void businessDialog() {
    dialogs.businessDialog();
  }

  protected void manageBusinesses() {
    dialogs.manageBusinesses();
  }

  protected void removeBusiness(Business chosen, boolean deleteFiles) {
    final File exports = new File(requireContext().getFilesDir(), "exports");
    final android.content.Context context = requireContext().getApplicationContext();
    io.execute(
        () -> {
          try {
            if (deleteFiles) {
              File folder = new File(exports, chosen.id);
              if (!folder
                  .getCanonicalPath()
                  .startsWith(exports.getCanonicalPath() + File.separator))
                throw new java.io.IOException(getString(R.string.ui_copy_75));
              deletePrivateFiles(folder);
            }
            androidx.work.WorkManager.getInstance(context)
                .cancelAllWorkByTag("export_" + chosen.id);
            model.repository.deleteBusiness(chosen.id);
            context.getSharedPreferences("narration_" + chosen.id, 0).edit().clear().apply();
            context.getSharedPreferences("designs_" + chosen.id, 0).edit().clear().apply();
            context
                .getSharedPreferences("workspace", 0)
                .edit()
                .remove("draft_" + chosen.id)
                .remove("contrast_" + chosen.id)
                .remove("narrated_" + chosen.id)
                .remove("work_" + chosen.id)
                .remove("tree_" + chosen.id)
                .apply();
            model.loadBusinesses();
          } catch (Exception e) {
            runOnUiThread(() -> toast(getString(R.string.ui_copy_76) + e.getMessage()));
          }
        });
  }

  protected static void deletePrivateFiles(File folder) throws java.io.IOException {
    if (!folder.exists()) return;
    File[] children = folder.listFiles();
    if (children != null) for (File child : children) deletePrivateFiles(child);
    if (!folder.delete()) throw new java.io.IOException("No se pudo eliminar " + folder.getName());
  }

  protected void editBusiness() {
    dialogs.editBusiness();
  }

  protected void reusePrevious() {
    if (publications.isEmpty()) {
      toast(getString(R.string.ui_copy_88));
      return;
    }
    reuse(publications.get(0));
  }

  protected void reuse(Publication publication) {
    try {
      if (!business.id.equals(publication.businessId))
        throw new IllegalArgumentException(getString(R.string.ui_copy_89));
      saveDraft();
      activeDraft = new mx.chambia.app.data.Draft();
      activeDraft.id = UUID.randomUUID().toString();
      activeDraft.businessId = business.id;
      activeDraft.title = publication.title;
      activeDraft.mealJson = new JSONObject(publication.businessJson).optString("mealJson", "{}");
      requireContext()
          .getSharedPreferences("workspace", 0)
          .edit()
          .putString("draft_" + business.id, activeDraft.id)
          .apply();
      draftEditor.replace(PublicationSnapshot.decode(publication.snapshotJson, business.id));
      saveDraft();
      go("preparar");
      toast(getString(R.string.ui_copy_90));
    } catch (Exception e) {
      toast(getString(R.string.ui_copy_91));
    }
  }

  protected void savePublication(
      Business business, List<Item> picked, List<File> files, String destination, boolean high) {
    exports.savePublication(business, picked, files, destination, high);
  }

  protected void export(String fileFormat, String mode) {
    exports.export(fileFormat, mode);
  }

  protected boolean validatePublication() {
    List<String> errors =
        mx.chambia.app.publication.MealPlan.decode(activeDraft.mealJson)
            .validate(draftEditor.entries());
    if (!errors.isEmpty()) {
      new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.review_menu)
          .setMessage(android.text.TextUtils.join("\n", errors))
          .setPositiveButton(R.string.correct_menu, (d, w) -> go("preparar"))
          .show();
      return false;
    }
    for (Item item : selected())
      if (!mx.chambia.app.publication.MealPlan.decode(activeDraft.mealJson).enabled
          && item.priceMode.equals("confirmar")) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pending_prices)
            .setMessage(R.string.pending_prices_help)
            .setPositiveButton(R.string.correct_menu, (d, w) -> go("preparar"))
            .show();
        return false;
      }
    return true;
  }

  protected void enqueueExport(
      Business captured,
      List<Item> content,
      String fileFormat,
      String target,
      boolean contrast,
      String mode) {
    exports.enqueueExport(captured, content, fileFormat, target, contrast, mode);
  }

  protected void observeExport() {
    exports.observeExport();
  }

  protected void cancelExport() {
    exports.cancelExport();
  }

  protected void launchShare(Intent intent) {
    exports.launchShare(intent);
  }

  protected void selectUsb() {
    exports.selectUsb();
  }

  protected void selectLocal(File file) {
    exports.selectLocal(file);
  }

  @Override
  public void onActivityResult(int request, int result, @Nullable Intent data) {
    super.onActivityResult(request, result, data);
    if (request != USB_REQUEST && request != USB_REQUEST + 1) return;
    if (result != Activity.RESULT_OK
        || data == null
        || data.getData() == null
        || pendingUsb == null) {
      pendingUsb = null;
      pendingFiles.clear();
      toast(getString(R.string.copy_cancelled));
      return;
    }
    Uri uri = data.getData();
    try {
      int granted =
          data.getFlags()
              & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      requireContext().getContentResolver().takePersistableUriPermission(uri, granted);
      requireContext()
          .getSharedPreferences("workspace", 0)
          .edit()
          .putString("tree_" + business.id, uri.toString())
          .apply();
    } catch (SecurityException ignored) {
      toast(getString(R.string.ui_copy_103));
    }
    File f = pendingUsb;
    List<File> output = new ArrayList<>(pendingFiles);
    if (output.isEmpty()) output.add(f);
    pendingFiles.clear();
    String owner = pendingBusiness;
    pendingUsb = null;
    String section =
        f.getName().endsWith(".txt")
            ? "ACCESIBLE"
            : request == USB_REQUEST
                ? "TV"
                : java.util.Arrays.asList("IMPRESIÓN", "A4", "OFICIO").contains(destination)
                    ? "IMPRESION"
                    : "CELULAR";
    final android.content.Context context = requireContext().getApplicationContext();
    final String publicationId = pendingPublication;
    io.execute(
        () -> {
          int copied = 0;
          try {
            for (File entry : output) {
              ExportManager.copyToTree(context, uri, entry, owner, section);
              copied++;
            }
            final int count = copied;
            recordEvent(
                publicationId, request == USB_REQUEST ? "COPIED_USB" : "COPIED_LOCAL", count);
            runOnUiThread(
                () ->
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                            requireContext())
                        .setMessage(getString(R.string.copy_success, count))
                        .setPositiveButton(R.string.close, null)
                        .show());
          } catch (Exception e) {
            final int count = copied;
            final List<File> remaining = new ArrayList<>(output.subList(count, output.size()));
            recordEvent(publicationId, "COPY_PARTIAL", count);
            runOnUiThread(
                () ->
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                            requireContext())
                        .setMessage(
                            getString(R.string.copy_partial, count, output.size(), e.getMessage()))
                        .setNegativeButton(R.string.close, null)
                        .setPositiveButton(
                            R.string.retry,
                            (d, w) -> {
                              pendingFiles.clear();
                              pendingFiles.addAll(remaining);
                              pendingBusiness = owner;
                              pendingPublication = publicationId;
                              pendingUsb = remaining.get(0);
                              if (request == USB_REQUEST) selectUsb();
                              else selectLocal(remaining.get(0));
                            })
                        .show());
          }
        });
  }

  protected void recordEvent(String id, String type, int count) {
    exports.recordEvent(id, type, count);
  }

  protected String narrationScript() {
    return exports.narrationScript();
  }

  protected boolean configureVoice() {
    return exports.configureVoice();
  }

  protected void stopNarration() {
    exports.stopNarration();
  }

  protected void speak() {
    exports.speak();
  }

  protected void synthesizeAudio() {
    exports.synthesizeAudio();
  }

  protected List<Item> selected() {
    return draftEditor == null ? new ArrayList<>() : draftEditor.visible();
  }

  protected String price(Item i) {
    return mx.chambia.app.data.PriceLabel.of(i);
  }

  protected void section(String s) {
    TextView t = text(s, 23, true);
    t.setTextColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
    container.addView(t, match());
  }

  protected void hint(String s) {
    TextView t = text(s, 14, false);
    t.setTextColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
    LinearLayout.LayoutParams p = match();
    p.setMargins(0, dp(8), 0, dp(14));
    container.addView(t, p);
  }

  protected void cardTitle(String s) {
    TextView t = text(s, 17, true);
    t.setTextColor(TEAL);
    LinearLayout.LayoutParams p = match();
    p.setMargins(0, dp(15), 0, dp(8));
    container.addView(t, p);
  }

  protected void line(String label, String value) {
    LinearLayout r = row();
    TextView left = text(label, 14, false);
    r.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
    r.addView(text(value, 14, true));
    container.addView(r, match());
  }

  protected void action(String label, Runnable event, boolean primary) {
    Button btn = button(label, primary);
    LinearLayout.LayoutParams p = match();
    p.setMargins(0, dp(4), 0, dp(7));
    container.addView(btn, p);
    btn.setOnClickListener(v -> event.run());
  }

  protected LinearLayout block() {
    LinearLayout box = vertical();
    box.setPadding(dp(12), dp(12), dp(12), dp(12));
    box.setBackground(shape(WHITE, BORDER));
    LinearLayout.LayoutParams p = match();
    p.setMargins(0, dp(5), 0, dp(7));
    container.addView(box, p);
    return box;
  }

  protected GradientDrawable shape(int bg, int border) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(bg);
    d.setCornerRadius(dp(13));
    d.setStroke(dp(1), border);
    return d;
  }

  protected Button button(String label, boolean strong) {
    com.google.android.material.button.MaterialButton b =
        new com.google.android.material.button.MaterialButton(
            requireContext(),
            null,
            strong
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle);
    b.setAllCaps(false);
    b.setText(label);
    b.setTextSize(16);
    b.setMinHeight(dp(48));
    b.setCornerRadius(dp(12));
    b.setTextColor(
        androidx.core.content.ContextCompat.getColorStateList(
            requireContext(), strong ? R.color.button_text : R.color.nav_tint));
    b.setBackgroundTintList(
        strong
            ? androidx.core.content.ContextCompat.getColorStateList(
                requireContext(), R.color.button_background)
            : android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface)));
    return b;
  }

  protected TextView text(String v, int size, boolean bold) {
    TextView t = new TextView(requireContext());
    t.setText(v);
    t.setTextSize(size);
    t.setTextColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
    if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    return t;
  }

  protected EditText input(String hint, String value) {
    EditText e = new EditText(requireContext());
    e.setTextSize(16);
    e.setSingleLine(true);
    e.setHint(hint);
    e.setContentDescription(hint);
    e.setMinHeight(dp(48));
    e.setText(value);
    return e;
  }

  protected Spinner spinner(String[] opts) {
    Spinner s = new Spinner(requireContext());
    s.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, opts));
    return s;
  }

  protected View scrollForm(View form) {
    ScrollView scroll = new ScrollView(requireContext());
    scroll.addView(form);
    return scroll;
  }

  protected LinearLayout row() {
    LinearLayout l = new LinearLayout(requireContext());
    l.setGravity(Gravity.CENTER_VERTICAL);
    l.setOrientation(LinearLayout.HORIZONTAL);
    return l;
  }

  protected LinearLayout vertical() {
    LinearLayout l = new LinearLayout(requireContext());
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  protected LinearLayout.LayoutParams match() {
    return new LinearLayout.LayoutParams(-1, -2);
  }

  protected int dp(int x) {
    return (int) (getResources().getDisplayMetrics().density * x + 0.5f);
  }

  protected void toast(String s) {
    Toast.makeText(requireContext(), s, Toast.LENGTH_LONG).show();
  }

  private void saveUiState() {
    if (shell != null && model != null && !renderedKey.isEmpty()) {
      model.uiState.set("scroll:" + renderedKey, shell.scroll.getScrollY());
      model.uiState.set("page:" + renderedKey, previewPage);
      model.uiState.set("destination:" + renderedKey, destination);
    }
  }

  @Override
  public void onPause() {
    saveUiState();
    super.onPause();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    ArrayList<String> paths = new ArrayList<>();
    for (File file : pendingFiles) paths.add(file.getAbsolutePath());
    out.putStringArrayList("pendingFiles", paths);
    out.putString("format", format);
    out.putString("destination", destination);
    if (pendingUsb != null) out.putString("pending", pendingUsb.getAbsolutePath());
    out.putString("pendingBusiness", pendingBusiness);
    out.putString("pendingPublication", pendingPublication);
    out.putInt("previewPage", previewPage);
  }

  @Override
  public void onDestroyView() {
    stopNarration();
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
    shell = null;
    super.onDestroyView();
  }
}
