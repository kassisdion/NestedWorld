package com.nestedworld.nestedworld.network.http.models.response.places.regions;

import com.google.gson.annotations.Expose;
import com.nestedworld.nestedworld.database.models.Region;

public class RegionResponse {
    @Expose
    public Region region;
}
