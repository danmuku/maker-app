package com.danmuku.maker.app.model;

import java.io.Serializable;
import java.util.List;


public class FeedItem implements Serializable {

    private String imgPath;
    private List<TagItem> tagList;
    private int position;

    public FeedItem() {

    }

    public FeedItem(List<TagItem> tagList, String imgPath) {
        this.imgPath = imgPath;
        this.tagList = tagList;
        this.position = 0;
    }

    public FeedItem(List<TagItem> tagList, String imgPath, int position) {
        this.imgPath = imgPath;
        this.tagList = tagList;
        this.position = 0;
    }

    public List<TagItem> getTagList() {
        return tagList;
    }

    public void setTagList(List<TagItem> tagList) {
        this.tagList = tagList;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public int getPosition(){
        return position;
    }

    public void setPosition(int position){this.position = position;}
}
