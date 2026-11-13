package mx.chambia.app;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import mx.chambia.app.data.ChambiaRepository;

/** Retains work and operation status across screen recreation. */
public final class WorkspaceViewModel extends AndroidViewModel {
  public final ChambiaRepository repository;
  public final androidx.lifecycle.SavedStateHandle uiState;
  public final MutableLiveData<java.util.List<mx.chambia.app.data.Business>> businesses =
      new MutableLiveData<>();
  public final MutableLiveData<Content> content = new MutableLiveData<>();
  public final MutableLiveData<String> error = new MutableLiveData<>();

  public static final class Content {
    public final String businessId;
    public final String draftId;
    public final java.util.List<mx.chambia.app.data.Item> items;
    public final mx.chambia.app.data.Draft draft;
    public final java.util.List<mx.chambia.app.data.Publication> publications;

    Content(
        String id,
        String draftId,
        java.util.List<mx.chambia.app.data.Item> items,
        java.util.List<mx.chambia.app.data.Publication> publications,
        mx.chambia.app.data.Draft draft) {
      this.draftId = draftId;
      this.draft = draft;
      this.businessId = id;
      this.items = items;
      this.publications = publications;
    }
  }

  public void loadBusinesses() {
    repository.io.execute(
        () -> {
          try {
            businesses.postValue(repository.database.dao().businesses());
          } catch (Exception ex) {
            error.postValue("No se pudieron cargar los negocios: " + ex.getMessage());
          }
        });
  }

  public void loadBusiness(String id) {
    final String draftId = activeDraftId(id);
    content.setValue(null);
    repository.io.execute(
        () -> {
          try {
            content.postValue(
                new Content(
                    id,
                    draftId,
                    repository.database.dao().items(id),
                    repository.database.dao().publications(id),
                    repository.database.dao().draft(id, draftId)));
          } catch (Exception ex) {
            error.postValue("No se pudo cargar el negocio: " + ex.getMessage());
          }
        });
  }

  public static final class DraftSaveState {
    public final String businessId, message;

    DraftSaveState(String id, String message) {
      this.businessId = id;
      this.message = message;
    }
  }

  public final MutableLiveData<DraftSaveState> draftSaveState = new MutableLiveData<>();

  public String activeDraftId(String businessId) {
    return getApplication()
        .getSharedPreferences("workspace", 0)
        .getString("draft_" + businessId, "active_" + businessId);
  }

  public void saveDraft(
      mx.chambia.app.data.Draft source, java.util.List<mx.chambia.app.data.Item> entries) {
    final String businessId = source.businessId;
    final mx.chambia.app.data.Draft draft = new mx.chambia.app.data.Draft();
    draft.id = source.id;
    draft.businessId = businessId;
    draft.title = source.title;
    draft.mode = source.mode;
    draft.session = source.session;
    draft.mealJson = source.mealJson;
    draft.updatedAt = System.currentTimeMillis();
    try {
      draft.snapshotJson = PublicationSnapshot.encode(entries);
    } catch (Exception ex) {
      error.setValue("No se pudo preparar el guardado del borrador.");
      return;
    }
    draftSaveState.setValue(new DraftSaveState(businessId, "Guardando borrador…"));
    repository.io.execute(
        () -> {
          try {
            repository.database.dao().saveDraft(draft);
            draftSaveState.postValue(
                new DraftSaveState(businessId, "Borrador guardado en este dispositivo"));
          } catch (Exception ex) {
            draftSaveState.postValue(
                new DraftSaveState(businessId, "No se pudo guardar el borrador"));
            error.postValue("No se pudo guardar el borrador: " + ex.getMessage());
          }
        });
  }

  public final MutableLiveData<Boolean> exporting = new MutableLiveData<>(false);

  public WorkspaceViewModel(@NonNull Application app, androidx.lifecycle.SavedStateHandle uiState) {
    super(app);
    this.uiState = uiState;
    repository = ChambiaRepository.get(app);
  }
}
