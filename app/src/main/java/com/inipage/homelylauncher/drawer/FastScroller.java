package com.inipage.homelylauncher.drawer;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class for setting up a scrollbar.
 */
public class FastScroller {
    final String TAG = "FastScroller";

    class SectionElement {
        String name;
        int count;
        int start;
        float startZone;
        float endZone;

        SectionElement(int count, int start, String name) {
            this.count = count;
            this.start = start;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public int getStart() {
            return start;
        }
    }

    List<SectionElement> mappings;
    float maxTranslation;
    int totalCount = 0;
    RecyclerView scrollview;
    View bar;
    TextView popup;
    TextView startLetter;
    TextView endLetter;
    int totalDy = 0;

    /**
     * Create a FastScroller. You must call setupScrollbar() before expecting this to work, and
     * after adding items with pushMapping().
     * @param scrollview The RecyclerView.
     */
    public FastScroller(RecyclerView scrollview, View bar, TextView startLetter, TextView endLetter, TextView popup){
        this.scrollview = scrollview;
        this.bar = bar;
        this.startLetter = startLetter;
        this.endLetter = endLetter;
        this.popup = popup;
        mappings = new ArrayList<>();
    }

    /**
     * Used to associate an element of the RecyclerView we're scrolling with a position in the list.
     * @param name The name of the element, or null if it has none (it'll be assigned "?").
     * @param position Its position.
     */
    public void pushMapping(String name, int position) {
        if(name == null || name.length() == 0){
            name = "?";
        }

        name = name.substring(0, 1);
        SectionElement se = containsName(name);
        if(se != null){
            ++se.count;
            if(position < se.start) se.start = position;
        } else {
            mappings.add(new SectionElement(1, position, name));
        }
    }

    public void setupScrollbar(){
        //Here, we sort all those lovely SectionElements (IMPORTANT: WE ASSUME THE LIST IS ALSO
        //SORTED THE SAME WAY) by name, and then get their % in the overall list
        Collections.sort(mappings, new Comparator<SectionElement>() {
            @Override
            public int compare(SectionElement lhs, SectionElement rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        //Get total number of elements
        totalCount = 0;
        for(SectionElement se : mappings){
            totalCount += se.count;
        }

        //Set start zone and end zone in all the elements
        float currentEnd = 0;
        maxTranslation = bar.getWidth();
        if(mappings.size() > 0){
            startLetter.setText(mappings.get(0).getName());
            endLetter.setText(mappings.get(mappings.size()-1).getName());
        }

        for(SectionElement se : mappings){
            float sizeOfZone = ((float)se.count / (float)totalCount) * maxTranslation;
            se.startZone = currentEnd;
            currentEnd += sizeOfZone;
            se.endZone = currentEnd;
        }

        int[] barCoordinates = new int[2];
        bar.getLocationOnScreen(barCoordinates);
        final int startBar = barCoordinates[0];
        final int endBar = startBar + (int) maxTranslation;
        bar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Log.d(TAG, "A touch event has occurred");
                switch(event.getAction()){
                    case MotionEvent.ACTION_MOVE:
                        //Log.d(TAG, "Move event with Y " + event.getRawY());
                        float rawX = event.getRawX();
                        if(rawX > (float)startBar && rawX < (float)endBar){
                            float place = rawX - startBar;

                            int position = 0;
                            for(SectionElement se : mappings){
                                if(se.startZone <= place && se.endZone >= place){
                                    position = se.start;
                                    popup.setText(se.getName());
                                    break;
                                }
                            }
                            scrollview.scrollToPosition(position);
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        popup.setVisibility(View.GONE);
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        popup.setVisibility(View.VISIBLE);
                        return true;
                    case MotionEvent.ACTION_UP:
                        popup.setVisibility(View.GONE);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private SectionElement containsName(String name){
        for(SectionElement se : mappings){
            if(se.name.equals(name)) return se;
        }
        return null;
    }
}
