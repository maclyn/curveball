package com.inipage.homelylauncher.scroller;

import android.view.View;

/**
 * While building the ScrollerAdapter, it became clear a conventional list would not work: to
 * deal with the issues of building a mixed grid/list without chaining adapters and ListViews,
 * I created this class to... do weird things. Yeah. This is very much a work-in-progress and
 * highly experimental.
 */
public class PsuedoGridBinder {
    public class PositionBlock {
        int xOffset;
        int yOffset;
        int height;
        int width;

        public PositionBlock(int xOffset, int yOffset, int height, int width) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.height = height;
            this.width = width;
        }

        public int getXOffset() {
            return xOffset;
        }

        public int getYOffset() {
            return yOffset;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }

    public abstract class GroupChild {
    }

    public interface Placeable {
        /**
         * Returns a view representing this object.
         * TODO: Add a ViewHolder-esque thing to this.
         * @return A view representing the object.
         */
        View getView();

        /**
         * Returns an integer representing the "type" of placeable this is.
         * @return The type of placeable.
         */
        int getPlaceableType();

        PositionBlock getDimensions();
    }
}
