package com.example.onserve;

import android.content.Context;
import android.graphics.Typeface;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.ArrayList;
import java.util.List;

public class PlaceAutocompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private List<AutocompletePrediction> resultList = new ArrayList<>();
    private final CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    public PlaceAutocompleteAdapter(Context context) {
        super(context, R.layout.item_place_suggestion);
    }

    public void updateData(List<AutocompletePrediction> list) {
        this.resultList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Nullable
    @Override
    public AutocompletePrediction getItem(int position) {
        return resultList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place_suggestion, parent, false);
        }

        AutocompletePrediction item = getItem(position);
        if (item != null) {
            TextView tvPrimary = convertView.findViewById(R.id.tv_primary_text);
            TextView tvSecondary = convertView.findViewById(R.id.tv_secondary_text);
            ImageView ivIcon = convertView.findViewById(R.id.iv_suggestion_icon);

            tvPrimary.setText(item.getPrimaryText(STYLE_BOLD));
            tvSecondary.setText(item.getSecondaryText(STYLE_BOLD));
            
            // Customize icons: if secondary text is empty, it's likely a generic search query
            if (item.getSecondaryText(null).toString().isEmpty()) {
                ivIcon.setImageResource(R.drawable.ic_search_suggestion);
            } else {
                ivIcon.setImageResource(R.drawable.ic_location);
            }
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                // No actual filtering here as we fetch from API
                if (constraint != null) {
                    results.values = resultList;
                    results.count = resultList.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction) {
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                }
                return super.convertResultToString(resultValue);
            }
        };
    }
}
