package com.inipage.homelylauncher.search;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.Utilities;

import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {
    public static final String TAG = "SearchResultAdapter";

    public interface SearchAdapterListener {
        void onResultChosen(SearchResult result);
    }

    public class SearchResultViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView title;

        public SearchResultViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.search_result_icon);
            title = (TextView) itemView.findViewById(R.id.search_result_desc);
        }
    }

    private List<SearchResult> results;
    private float iconSize = -1;
    private SearchAdapterListener listener;

    public SearchResultAdapter(String query, Context ctx, SearchAdapterListener listener, List<ApplicationIcon> cachedApps){
        this.results = new ArrayList<>();
        this.listener = listener;
        if(query.length() > 0) {
            for (ApplicationIcon ai : cachedApps) {
                if (ai.getName().toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                    results.add(new SearchResult(ai));
                }
            }

            AutoCompleteApiFactory.getInstance().getSearchSuggestions("toolbar", query).enqueue(new Callback<AutoCompleteResult>() {
                @Override
                public void onResponse(Call<AutoCompleteResult> call, Response<AutoCompleteResult> response) {
                    List<SearchResult> additions = new ArrayList<>();
                    if(response.body() != null) {
                        for (String entry : response.body().getAutocompleteResults()) {
                            additions.add(new SearchResult(entry));
                        }
                    }
                    results.addAll(additions);
                    notifyItemRangeInserted(results.size() - additions.size(), additions.size());
                }

                @Override
                public void onFailure(Call<AutoCompleteResult> call, Throwable t) {
                    Log.d(TAG, "Oops", t);
                }
            });
        }
        this.iconSize = Utilities.convertDpToPixel(48, ctx);
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SearchResultViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(SearchResultViewHolder holder, int position) {
        final SearchResult searchResult = results.get(position);

        holder.title.setText(searchResult.getTitle());
        holder.icon.setTag(searchResult);

        if(searchResult.getType() == SearchResult.SearchResultType.APP_RESULT) {
            final ImageView iconView = holder.icon;
            iconView.setImageBitmap(IconCache.getInstance().getAppIcon(
                    searchResult.getAppData().getPackageName(),
                    searchResult.getAppData().getActivityName(),
                    IconCache.IconFetchPriority.APP_DRAWER_ICONS,
                    (int) iconSize,
                    new IconCache.ItemRetrievalInterface() {
                        @Override
                        public void onRetrievalComplete(Bitmap result) {
                            if(iconView.getTag().equals(searchResult))
                                iconView.setImageBitmap(result);
                        }
                    }));
        } else {
            holder.icon.setImageResource(R.drawable.ic_public_black_36dp);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onResultChosen(searchResult);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void launchTop() {
        listener.onResultChosen(results.get(0));
    }
}
