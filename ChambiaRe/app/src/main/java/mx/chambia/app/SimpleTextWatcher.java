package mx.chambia.app;

public final class SimpleTextWatcher implements android.text.TextWatcher {
  private final java.util.function.Consumer<String> action;

  public SimpleTextWatcher(java.util.function.Consumer<String> action) {
    this.action = action;
  }

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  public void onTextChanged(CharSequence s, int start, int before, int count) {}

  public void afterTextChanged(android.text.Editable s) {
    action.accept(s.toString());
  }
}
