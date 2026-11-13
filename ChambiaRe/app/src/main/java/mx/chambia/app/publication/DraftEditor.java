package mx.chambia.app.publication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mx.chambia.app.PublicationSnapshot;
import mx.chambia.app.data.Item;

/** Pure Java editing rules. No caller can mutate an offer through a catalogue reference. */
public final class DraftEditor {
  private final String businessId;
  private final List<Item> entries = new ArrayList<>();

  public DraftEditor(String businessId, List<Item> initial) {
    this.businessId = businessId;
    replace(initial);
  }

  public boolean includes(String id) {
    return index(id) >= 0;
  }

  private int index(String id) {
    for (int n = 0; n < entries.size(); n++) if (entries.get(n).id.equals(id)) return n;
    return -1;
  }

  private void check(Item item) {
    if (!businessId.equals(item.businessId))
      throw new IllegalArgumentException("El artículo pertenece a otro negocio");
  }

  public void include(Item item) {
    check(item);
    if (!includes(item.id))
      entries.add(PublicationSnapshot.copyItems(Collections.singletonList(item)).get(0));
  }

  public void update(Item item) {
    check(item);
    int index = index(item.id);
    if (index < 0) throw new IllegalArgumentException("Artículo fuera del borrador");
    entries.set(index, PublicationSnapshot.copyItems(Collections.singletonList(item)).get(0));
  }

  public void remove(String id) {
    int index = index(id);
    if (index >= 0) entries.remove(index);
  }

  public void move(String id, int offset) {
    int index = index(id), target = index + offset;
    if (index >= 0 && target >= 0 && target < entries.size())
      Collections.swap(entries, index, target);
  }

  public void replace(List<Item> items) {
    for (Item item : items) check(item);
    entries.clear();
    for (Item item : items) include(item);
  }

  public List<Item> entries() {
    return PublicationSnapshot.copyItems(entries);
  }

  public List<Item> visible() {
    List<Item> visible = new ArrayList<>();
    for (Item item : entries) if (!"fuera".equals(item.state)) visible.add(item);
    return PublicationSnapshot.copyItems(visible);
  }
}
