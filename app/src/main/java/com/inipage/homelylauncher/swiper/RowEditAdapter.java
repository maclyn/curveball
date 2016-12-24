package com.inipage.homelylauncher.swiper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.views.ShortcutGestureView;
import com.mobeta.android.dslv.DragSortListView;

import java.util.List;

public class RowEditAdapter extends ArrayAdapter<ShortcutGestureView.ShortcutCard> implements
        DragSortListView.DragListener, DragSortListView.DropListener {
    List<ShortcutGestureView.ShortcutCard> objects;

    public RowEditAdapter(Context context, int resource, List<ShortcutGestureView.ShortcutCard> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View editRow;
        if(convertView != null){
            editRow = convertView;
        } else {
            editRow = LayoutInflater.from(getContext()).inflate(R.layout.row_edit_row, null);
        }

        TextView name = (TextView) editRow.findViewById(R.id.appEditName);
        name.setText(getItem(position).getTitle());

        return editRow;
    }

    @Override
    public void drag(int from, int to) {
        //Not used. We don't worry about the drags until the "drop"
    }

    @Override
    public void drop(int from, int to) {
        //Edit underlying data set
        ShortcutGestureView.ShortcutCard element = getItem(from);
        objects.remove(element);
        objects.add(to, element);
        notifyDataSetChanged();
    }
}
