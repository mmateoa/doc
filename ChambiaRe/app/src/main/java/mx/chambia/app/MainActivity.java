package mx.chambia.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

/** Navigation container; editing, persistence and rendering live outside the Activity. */
public final class MainActivity extends AppCompatActivity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_main);
    if (state == null) {
      NavHostFragment host = NavHostFragment.create(R.navigation.main);
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.nav_host, host)
          .setPrimaryNavigationFragment(host)
          .commit();
    }
  }
}
