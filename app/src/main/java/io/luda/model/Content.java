package io.luda.model;

import java.io.Serializable;

public class Content implements Serializable {

    public enum Type {
        NEWS,
        AD_IMG,
        AD_VIDEO,
        NONE

    }

    public Content(){

    }


    public Content(String title, String description, String urlImage, String urlVideo,  String adId, String pubDate, Type type) {
        this.urlImage = urlImage;
        this.urlVideo = urlVideo;
        this.title = title;
        this.description = description;
        this.adId = adId;
        this.pubDate = pubDate;
        this.type = type;
    }

    public  String pubDate;
    public Type type = Type.NONE;

    public String urlImage;

    public String urlVideo;

    public String title;

    public String description;

    public String adId;

    public long duration = 10000;
}
