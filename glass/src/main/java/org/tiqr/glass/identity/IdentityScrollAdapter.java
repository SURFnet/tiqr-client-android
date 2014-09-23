package org.tiqr.glass.identity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;

import org.tiqr.authenticator.datamodel.Identity;

import java.util.Arrays;

/**
 * Generates the cards (using custom layouts, rather than the {@code Card} class) that show the
 * results of the game after it has ended.
 */
public class IdentityScrollAdapter extends CardScrollAdapter {
    private final Context _context;
    private Identity[] _identities = new Identity[0];
    private CardBuilder[] _cards = new CardBuilder[0];
    private CharSequence _footnote;

    /**
     * Constructor.
     *
     * @param context
     */
    public IdentityScrollAdapter(Context context) {
        _context = context;
    }

    /**
     * Sets the identities.
     *
     * @param identities
     */
    public void setIdentities(Identity[] identities) {
        _identities = identities;
        _cards = new CardBuilder[identities.length];
        notifyDataSetChanged();
    }

    /**
     * Sets the footnote.
     *
     * @param footnote
     */
    public void setFootnote(CharSequence footnote) {
        _footnote = footnote;
        _cards = new CardBuilder[_identities.length];
        notifyDataSetChanged();
    }

    /**
     * Card builder factory.
     *
     * @param position
     *
     * @return Card builder.
     */
    private CardBuilder _getCard(int position) {
        if (_cards[position] != null) {
            return _cards[position];
        }

        Identity identity = getItem(position);
        _cards[position] =
             new CardBuilder(_context, CardBuilder.Layout.COLUMNS)
                 .setText(identity.getDisplayName());

        if (_footnote != null) {
            _cards[position].setFootnote(_footnote);
        }

        return _cards[position];
    }

    /**
     * Returns the number of view types for the CardBuilder class. The
     * CardBuilder class has a convenience method that returns this value for
     * you.
     */
    @Override
    public int getViewTypeCount() {
        return CardBuilder.getViewTypeCount();
    }

    /**
     * Returns the view type of this card, so the system can figure out
     * if it can be recycled. The CardBuilder.getItemViewType() method
     * returns it's own type.
     */
    @Override
    public int getItemViewType(int position){
        return _getCard(position).getItemViewType();
    }

    /**
     * When requesting a card from the adapter, recycle the view if possible.
     * The CardBuilder.getView() method automatically recycles the convertView
     * it receives, if possible, or creates a new view if convertView is null or
     * of the wrong type.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return _getCard(position).getView(convertView, parent);
    }

    /**
     * Stable ids.
     *
     * @return true
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Returns the item identifier.
     *
     * @param position
     *
     * @return Item identifier.
     */
    public long getItemId(int position) {
        return _identities[position].getId();
    }

    /**
     * Returns the position of the given identity.
     *
     * @param identity Identity.
     *
     * @return Position.
     */
    @Override
    public int getPosition(Object identity) {
        return Arrays.asList(_identities).indexOf(identity);
    }

    /**
     * Number of items.
     *
     * @return Number of items.
     */
    @Override
    public int getCount() {
        return _identities.length;
    }

    /**
     * Item at position.
     *
     * @param position
     *
     * @return Item.
     */
    @Override
    public Identity getItem(int position) {
        return _identities[position];
    }
}
