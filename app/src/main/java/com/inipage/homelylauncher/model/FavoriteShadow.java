package com.inipage.homelylauncher.model;

import java.util.Comparator;

/**
 * A "favorites-lite" class simply used to keep track of shadow locations in the... grid!
 */
public class FavoriteShadow {
    private Favorite data;
    private int x;
    private int y;
    private int width;
    private int height;

    public FavoriteShadow(Favorite f){
        this.data = f;
        this.x = f.getX();
        this.y = f.getY();
        this.width = f.getWidth();
        this.height = f.getHeight();
    }

    public Favorite getData() {
        return data;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public FavoriteShadow setX(int x) {
        this.x = x;
        return this;
    }

    public FavoriteShadow setY(int y) {
        this.y = y;
        return this;
    }

    public FavoriteShadow setWidth(int width) {
        this.width = width;
        return this;
    }

    public FavoriteShadow setHeight(int height) {
        this.height = height;
        return this;
    }

    public static Comparator<FavoriteShadow> getComparator() {
        return new Comparator<FavoriteShadow>() {
            @Override
            public int compare(FavoriteShadow o1, FavoriteShadow o2) {
                if(o1.getY() < o2.getY()) return -1;
                if(o1.getY() > o2.getY()) return 1;
                if(o1.getX() < o2.getX()) return -1;
                if(o1.getX() > o2.getX()) return 1;
                return 0;
            }
        };
    }

    @Override
    public String toString() {
        String id = String.valueOf(data.getId());
        if(id.length() > 4){
            id = id.substring(0, 3) + "+";
        } else if (id.length() < 4){
            while(id.length() < 4)
                id += "-";
        }
        return id + " w/ " + width + "x" + height + " @(" + x + "," + y + ")";
    }
}
