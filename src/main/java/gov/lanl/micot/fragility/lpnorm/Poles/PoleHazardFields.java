package gov.lanl.micot.fragility.lpnorm;

import java.util.List;

/**
 * Created by 301338 on 6/27/2017.
 */
public class PoleHazardFields {

    private String id;
    private String hazardQuantityType;
    private PoleRasterFieldData rasterFieldData;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHazardQuantityType() {
        return hazardQuantityType;
    }

    public void setHazardQuantityType(String hazardQuantityType) {
        this.hazardQuantityType = hazardQuantityType;
    }

    public PoleRasterFieldData getRasterFieldData() {
        return rasterFieldData;
    }

    public void setRasterFieldData(PoleRasterFieldData rasterFieldData) {
        this.rasterFieldData = rasterFieldData;
    }
}
