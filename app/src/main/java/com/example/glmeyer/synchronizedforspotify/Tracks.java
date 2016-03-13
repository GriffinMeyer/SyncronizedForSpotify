
package com.example.glmeyer.synchronizedforspotify;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Tracks {

    @SerializedName("href")
    @Expose
    public String href;
    @SerializedName("items")
    @Expose
    public List<Item> items = new ArrayList<Item>();
    @SerializedName("limit")
    @Expose
    public Integer limit;
    @SerializedName("next")
    @Expose
    public String next;
    @SerializedName("offset")
    @Expose
    public Integer offset;
    @SerializedName("previous")
    @Expose
    public Object previous;
    @SerializedName("total")
    @Expose
    public Integer total;

}
