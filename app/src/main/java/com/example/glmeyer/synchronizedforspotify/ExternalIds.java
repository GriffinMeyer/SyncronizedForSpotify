
package com.example.glmeyer.synchronizedforspotify;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class ExternalIds implements Serializable {

    @SerializedName("isrc")
    @Expose
    public String isrc;

}
