package com.danmuku.maker.app.model;


import java.io.Serializable;

public class TagItem implements Serializable {
    private static final long serialVersionUID = 2685507991821634905L;
    private String name;
    private double x = -1;
    private double y = -1;


    public TagItem() {
        this.x = 10;
        this.y = 10;
    }

    public TagItem(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public TagItem(double x, double y, String label) {
        this.x = x;
        this.y = y;
        this.name = label;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

}
