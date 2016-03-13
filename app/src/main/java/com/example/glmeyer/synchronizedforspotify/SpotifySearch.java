
package com.example.glmeyer.synchronizedforspotify;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SpotifySearch implements Serializable{

    @SerializedName("tracks")
    @Expose
    public Tracks tracks;

}
