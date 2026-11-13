package mx.chambia.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.ColorUtils;
import mx.chambia.app.databinding.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VisualContractTest {
  private Context themed(int width, float scale, boolean night) {
    Configuration config =
        new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());
    config.screenWidthDp = width;
    config.smallestScreenWidthDp = width;
    config.fontScale = scale;
    config.uiMode =
        (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
            | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
    return new ContextThemeWrapper(
        RuntimeEnvironment.getApplication().createConfigurationContext(config),
        R.style.Theme_ChambIA);
  }

  @Test
  public void semanticPrimaryAndSurfaceTextMeetContrastInBothThemes() {
    for (boolean dark : new boolean[] {false, true}) {
      Context c = themed(393, 1.3f, dark);
      assertTrue(
          ColorUtils.calculateContrast(c.getColor(R.color.on_primary), c.getColor(R.color.primary))
              >= 4.5);
      assertTrue(
          ColorUtils.calculateContrast(
                  c.getColor(R.color.text_primary), c.getColor(R.color.surface))
              >= 4.5);
      assertTrue(
          ColorUtils.calculateContrast(
                  c.getColor(R.color.text_secondary), c.getColor(R.color.background))
              >= 4.5);
    }
  }

  @Test
  public void xmlScreensInflateAtSmallAndTabletWidthsAndLargeFonts() {
    for (int width : new int[] {320, 393, 800})
      for (float scale : new float[] {1f, 1.3f, 1.5f, 2f}) {
        Context c = themed(width, scale, false);
        LayoutInflater inflater = LayoutInflater.from(c);
        FragmentHomeBinding home = FragmentHomeBinding.inflate(inflater);
        assertFalse(home.prepare.getText().toString().isEmpty());
        assertTrue(
            home.prepare.getMinHeight() >= 48 * c.getResources().getDisplayMetrics().density);
        FragmentCatalogBinding.inflate(inflater);
        FragmentPreparationBinding.inflate(inflater);
        EditorMealBinding.inflate(inflater);
        FragmentDesignsBinding.inflate(inflater);
        FragmentPreviewBinding.inflate(inflater);
        FragmentHistoryBinding.inflate(inflater);
        FragmentExportBinding.inflate(inflater);
        FragmentAccessibilityBinding.inflate(inflater);
        FragmentWorkspaceBinding shell = FragmentWorkspaceBinding.inflate(inflater);
        assertEquals(4, shell.navigation.getMenu().size());
        for (int n = 0; n < 4; n++) {
          assertNotNull(shell.navigation.getMenu().getItem(n).getIcon());
          assertFalse(shell.navigation.getMenu().getItem(n).getTitle().toString().isEmpty());
        }
      }
  }

  @Test
  public void materialPrimaryKeepsVisibleTextAcrossStates() {
    Context c = themed(320, 1.5f, false);
    com.google.android.material.button.MaterialButton button =
        FragmentHomeBinding.inflate(LayoutInflater.from(c)).prepare;
    int[][] states = {
      {android.R.attr.state_enabled},
      {android.R.attr.state_enabled, android.R.attr.state_pressed},
      {-android.R.attr.state_enabled}
    };
    for (int[] state : states) {
      int fg = button.getTextColors().getColorForState(state, 0),
          bg = button.getBackgroundTintList().getColorForState(state, 0);
      assertNotEquals(fg, bg);
      assertEquals(255, android.graphics.Color.alpha(fg));
    }
  }
}
