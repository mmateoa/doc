package mx.chambia.app.ui;

import android.view.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import mx.chambia.app.*;
import mx.chambia.app.data.*;
import mx.chambia.app.databinding.RowCatalogItemBinding;

/** Immutable row submissions keep selection updates local and preserve focus. */
public final class CatalogItemAdapter extends ListAdapter<Item, CatalogItemAdapter.Holder> {
  public interface Events {
    boolean selected(Item i);

    void toggle(Item i, boolean checked);

    void edit(Item i);
  }

  private final boolean selection;
  private boolean editable = true;
  private final Events events;

  public CatalogItemAdapter(boolean selection, Events events) {
    super(
        new DiffUtil.ItemCallback<Item>() {
          public boolean areItemsTheSame(Item a, Item b) {
            return a.id.equals(b.id);
          }

          public boolean areContentsTheSame(Item a, Item b) {
            return a.name.equals(b.name)
                && a.category.equals(b.category)
                && a.price == b.price
                && a.priceMode.equals(b.priceMode)
                && a.state.equals(b.state);
          }
        });
    this.selection = selection;
    this.events = events;
  }

  public CatalogItemAdapter(boolean selection, boolean editable, Events events) {
    this(selection, events);
    this.editable = editable;
  }

  public static final class Holder extends RecyclerView.ViewHolder {
    final RowCatalogItemBinding b;

    Holder(RowCatalogItemBinding b) {
      super(b.getRoot());
      this.b = b;
    }
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup parent, int type) {
    return new Holder(
        RowCatalogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int position) {
    Item i = getItem(position);
    h.b.name.setText(i.name);
    h.b.detail.setText(i.category + " · " + PriceLabel.of(i));
    h.b.selected.setVisibility(selection ? View.VISIBLE : View.GONE);
    h.b.selected.setOnCheckedChangeListener(null);
    h.b.selected.setChecked(events.selected(i));
    h.b.selected.setContentDescription(
        h.itemView.getContext().getString(R.string.select_item, i.name));
    h.b.selected.setOnCheckedChangeListener((v, c) -> events.toggle(i, c));
    h.b.edit.setVisibility(editable ? View.VISIBLE : View.GONE);
    h.b.edit.setContentDescription(h.itemView.getContext().getString(R.string.edit_item, i.name));
    h.b.edit.setOnClickListener(v -> events.edit(i));
  }
}
