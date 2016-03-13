
package com.example.glmeyer.synchronizedforspotify;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SpotifySearch {

    @SerializedName("tracks")
    @Expose
    public Tracks tracks;

}
