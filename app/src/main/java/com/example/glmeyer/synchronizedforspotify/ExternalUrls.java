
package com.example.glmeyer.synchronizedforspotify;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ExternalUrls implements Serializable {

    @SerializedName("spotify")
    @Expose
    public String spotify;

}
