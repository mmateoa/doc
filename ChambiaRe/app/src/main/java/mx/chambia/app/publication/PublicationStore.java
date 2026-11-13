package mx.chambia.app.publication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import mx.chambia.app.PublicationSnapshot;
import mx.chambia.app.data.*;
import mx.chambia.app.export.ExportManager;
import org.json.*;

/** Versioned content and file references; never updates an exported record. */
public final class PublicationStore {
  private PublicationStore() {}

  public static String businessJson(Business business) throws Exception {
    JSONObject own = new JSONObject();
    own.put("mealJson", business.mealJson);
    own.put("id", business.id);
    own.put("name", business.name);
    own.put("phone", business.phone);
    own.put("sector", business.sector);
    own.put("slogan", business.slogan);
    own.put("modality", business.modality);
    own.put("templateId", business.templateId);
    own.put("profileJson", business.profileJson);
    own.put("publicationHeading", business.publicationHeading);
    own.put("publicationSession", business.publicationSession);
    return own.toString();
  }

  public static Business business(String json) throws Exception {
    JSONObject own = new JSONObject(json);
    Business result = new Business();
    result.mealJson = own.optString("mealJson", "{}");
    result.id = own.getString("id");
    result.name = own.getString("name");
    result.phone = own.optString("phone", "");
    result.sector = own.optString("sector", "fonda");
    result.slogan = own.optString("slogan", "");
    result.modality = own.optString("modality", "");
    result.templateId = own.optString("templateId", "mexicano");
    result.profileJson = own.optString("profileJson", "{}");
    result.publicationHeading = own.optString("publicationHeading", "");
    result.publicationSession = own.optString("publicationSession", "");
    return result;
  }

  public static Publication save(
      AppDao dao,
      Business business,
      List<Item> items,
      List<File> files,
      String destination,
      boolean contrast)
      throws Exception {
    Publication publication = new Publication();
    publication.id = UUID.randomUUID().toString();
    publication.businessId = business.id;
    publication.createdAt = System.currentTimeMillis();
    publication.title =
        (business.publicationHeading.isEmpty() ? "Publicación" : business.publicationHeading)
            + " "
            + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
    publication.date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    publication.templateId = business.templateId;
    publication.snapshotJson = PublicationSnapshot.encode(items);
    publication.businessJson = businessJson(business);
    publication.destination = destination;
    publication.highContrast = contrast;
    publication.note = ExportManager.readable(business, items);
    JSONArray paths = new JSONArray();
    for (File file : files) paths.put(file.getAbsolutePath());
    publication.exportPathsJson = paths.toString();
    dao.savePublication(publication);
    return publication;
  }
}
