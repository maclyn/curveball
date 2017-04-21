package com.inipage.homelylauncher.scroller;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.drawer.HighlightableAdapter;
import com.inipage.homelylauncher.model.Favorite;

import java.util.List;

public class ScrollerAdapter extends
        RecyclerView.Adapter<ScrollerAdapter.FavoriteHolder> implements
        HighlightableAdapter {
    Context ctx;
    List<Favorite> favorites;
    List<ApplicationIcon> apps;

    private int highlightedItem = -1;

    private static final int ITEM_TYPE_SPACE = 1; //@0
    private static final int ITEM_TYPE_HEADER = 2; //@1
    private static final int ITEM_TYPE_FAVORITE = 3; //@2 + (iof(fav))
    private static final int ITEM_TYPE_PIVOT = 4; //@2 + fav.len
    private static final int ITEM_TYPE_APP = 5; //@2 + fav.len + 1 + (iof(app))
    private static final int ITEM_TYPE_INVAL = 99;

    public static class FavoriteHolder extends RecyclerView.ViewHolder {
        View mainView;

        public FavoriteHolder(View mainView, int itemType) {
            super(mainView);
            this.mainView = mainView;
            switch(itemType){

            }
        }
    }

    public ScrollerAdapter(Context context) {
        this.ctx = context;
        //TODO: Add favorites and apps to this
    }

    @Override
    public FavoriteHolder onCreateViewHolder(ViewGroup parent, int position) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        int type = getItemViewType(position);
        View root = null;
        switch(type){
            case ITEM_TYPE_SPACE:
                root = new View(ctx);
                break;
            case ITEM_TYPE_HEADER:
                root = inflater.inflate(R.layout.item_scr_header, parent, false);
                break;
            case ITEM_TYPE_FAVORITE:
                root = inflater.inflate(R.layout.item_scr_favorite, parent, false);
                break;
            case ITEM_TYPE_PIVOT:
                root = inflater.inflate(R.layout.item_scr_pivot, parent, false);
                break;
            default:
                throw new RuntimeException("Invalid type!");
        }
        return new FavoriteHolder(root, type);
    }

    //Set up specific customIcon with data
    @Override
    public void onBindViewHolder(FavoriteHolder viewHolder, int i) {
        switch(getItemViewType(i)){
            case ITEM_TYPE_INVAL:
                throw new RuntimeException("ITEM_TYPE_INVAL!");
            case ITEM_TYPE_SPACE:
                bindSpace(viewHolder, i);
                break;
            case ITEM_TYPE_HEADER:
                bindHeader(viewHolder, i);
                break;
            case ITEM_TYPE_FAVORITE:
                bindFavorite(viewHolder, i);
                break;
            case ITEM_TYPE_PIVOT:
                bindPivot(viewHolder, i);
                break;
            case ITEM_TYPE_APP:
                bindApp(viewHolder, i);
                break;
        }
    }

    private void bindSpace(FavoriteHolder holder, int pos) {

    }

    private void bindHeader(FavoriteHolder holder, int pos) {

    }

    private void bindFavorite(FavoriteHolder holder, int pos) {

    }

    private void bindPivot(FavoriteHolder holder, int pos) {

    }

    private void bindApp(FavoriteHolder holder, int pos) {

    }

    @Override
    public int getItemViewType(int position) {
        int headerPos = 1;
        int pivotPos = 2 + favorites.size();
        int appsEndPos = 2 + favorites.size() + 1 + apps.size() - 1;

        if(position == 0)
            return ITEM_TYPE_SPACE;
        else if (position == headerPos)
            return ITEM_TYPE_HEADER;
        else if (position > headerPos && position < pivotPos)
            return ITEM_TYPE_FAVORITE;
        else if (position == pivotPos)
            return ITEM_TYPE_PIVOT;
        else if (position > pivotPos && position <= appsEndPos)
            return ITEM_TYPE_APP;
        else
            return ITEM_TYPE_INVAL;
    }

    @Override
    public int getItemCount() {
        return 1 /* space */ + 1 /* header */ + favorites.size() + 1 /* pivot */ + apps.size();
    }

    @Override
    public long getItemId(int position) {
        switch(getItemViewType(position)){
            case ITEM_TYPE_INVAL:
                throw new RuntimeException("ITEM_TYPE_INVAL!");
            case ITEM_TYPE_SPACE:
                return 1;
            case ITEM_TYPE_HEADER:
                return 2;
            case ITEM_TYPE_FAVORITE:
                return favorites.get(position - 2).hashCode();
            case ITEM_TYPE_PIVOT:
                return 3;
            case ITEM_TYPE_APP:
                return apps.get(position - 3  - favorites.size()).hashCode();
            default:
                return -1;
        }
    }

    @Override
    public void highlightItem(int position) {
        /*
        if(highlightedItem == position) return; //No-op if already set
        if(highlightedItem >= 0) unhighlightItem();

        highlightedItem = position;
        notifyItemChanged(position);
        */
    }

    @Override
    public void unhighlightItem(){
        /*
        int previouslyHighlighted = highlightedItem;
        highlightedItem = -1;
        if(highlightedItem >= 0) notifyItemChanged(previouslyHighlighted);
        */
    }
}
