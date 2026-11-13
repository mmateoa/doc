package mx.chambia.app;

import mx.chambia.app.databinding.FragmentHomeBinding;

/** A compact work summary; detailed editing belongs to Preparation. */
public final class HomeFragment extends WorkspaceFragment {
  @Override
  protected void renderPage() {
    FragmentHomeBinding b = FragmentHomeBinding.inflate(getLayoutInflater(), container, true);
    if (getResources().getConfiguration().screenWidthDp <= 360) {
      android.view.ViewGroup.LayoutParams compact = b.thumbnail.getLayoutParams();
      compact.height = dp(64);
      b.thumbnail.setLayoutParams(compact);
    }
    b.question.setText(
        "fonda".equals(business.sector) ? R.string.home_fonda : R.string.home_question);
    b.draftTitle.setText(activeDraft.title);
    mx.chambia.app.publication.MealPlan meal =
        mx.chambia.app.publication.MealPlan.decode(activeDraft.mealJson);
    b.summary.setText(
        selected().isEmpty()
            ? getString(R.string.empty_draft)
            : getString(R.string.selected_count, selected().size()));
    if (meal.enabled) b.summary.setText(meal.heading() + " · " + selected().size() + " opciones");
    try {
      b.thumbnail.bind(business, selected(), business.templateId, 1920, 1080, accessible, 0);
    } catch (Exception ex) {
      b.thumbnail.setVisibility(android.view.View.GONE);
    }
    b.prepare.setOnClickListener(v -> go("preparar"));
    b.preview.setOnClickListener(v -> go("vista"));
    b.announcement.setOnClickListener(v -> quickAnnouncement());
    b.reuse.setOnClickListener(v -> reusePrevious());
    b.catalog.setOnClickListener(v -> go("catalogo"));
    b.drafts.setOnClickListener(v -> chooseDraft());
    b.profile.setOnClickListener(v -> editBusiness());
  }
}
