package org.femtobit.orbisverto.util;

public class Vector2i {
    public int x;
    public int y;

    public Vector2i() {
        x = 0; y = 0;
    }

    public Vector2i(int xIn, int yIn) {
        x = xIn;
        y = yIn;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
