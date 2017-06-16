package gov.lanl.micot.fragility.lpnorm;

/**
 * Created by Trevor Crawford on 6/14/2017.
 */
public class GenericData {

    private String lat;
    private String lon;
    private String id;
    private String cableSpan;
    private String uriPath;

    public void setLat(String num){
        this.lat = num;
    }

    public void setLon(String num){
        this.lon = num;
    }

    public void setId(String num){
        this.id = num;
    }

    public void setCableSpan(String num){
        this.cableSpan = num;
    }

    public String getPoleString(){
        return "{\n" +
                "   \"assetClass\": \"PowerDistributionPole\", \n" +
                "   \"assetGeometry\": {\n" +
                "    \"coordinates\": [\n" +
                "     "+this.lon+", \n" +
                "     "+this.lat+"\n" +
                "    ], \n" +
                "    \"type\": \"Point\"\n" +
                "   }, \n" +
                "    \"id\": \""+this.id+"\", \n" +
                "   \"properties\": {\n" +
                "    \"baseDiameter\": 0.22225, \n" +
                "    \"cableSpan\": "+this.cableSpan+", \n" +
                "    \"commAttachmentHeight\": 4.7244, \n" +
                "    \"commCableDiameter\": 0.04, \n" +
                "    \"commCableNumber\": 2, \n" +
                "    \"commCableWireDensity\": 1500.0, \n" +
                "    \"height\": 9.144, \n" +
                "    \"meanPoleStrength\": 38600000.0, \n" +
                "    \"powerAttachmentHeight\": 5.6388, \n" +
                "    \"powerCableDiameter\": 0.0094742, \n" +
                "    \"powerCableNumber\": 2, \n" +
                "    \"powerCableWireDensity\": 2700.0, \n" +
                "    \"powerCircuitName\": \"NAME\", \n" +
                "    \"stdDevPoleStrength\": 7700000.0, \n" +
                "    \"topDiameter\": 0.15361635107, \n" +
                "    \"woodDensity\": 500.0\n" +
                "   }\n" +
                "  }, ";
    }

    public String getResponseEstimator(){

        return "{\n" +
                "\t\t\t\"id\": \"PowerPoleWindStressEstimator\",\n" +
                "\t\t\t\"responseEstimatorClass\": \"PowerPoleWindStressEstimator\",\n" +
                "\t\t\t\"assetClass\": \"PowerDistributionPole\",\n" +
                "\t\t\t\"hazardQuantityTypes\": [\"Windspeed\"],\n" +
                "\t\t\t\"responseQuantityType\": \"DamageProbability\",\n" +
                "\t\t\t\"properties\": \n" +
                "\t\t\t{\n" +
                "\n" +
                "\t\t\t}\n" +
                "\t\t}";
    }

    public String getHazardFields (){

        return "{\n" +
                "\t\t\t\"id\": \"hawaiiArcTestGrid\",\n" +
                "\t\t\t\"hazardQuantityType\": \"Windspeed\",\n" +
                "\t\t\t\"rasterFieldData\": \n" +
                "\t\t\t{\n" +
//                "\t\t\t\t\"uri\": \"file:///C:/Users/301338/Desktop/PROJECTS/fragility/micot-fragility/target/wf_clip.asc\",\n" +
                "\t\t\t\t\"uri\": \"file:///"+this.uriPath +"\",\n" +
                "\t\t\t\t\"gridFormat\": \"ArcGrid\",\n" +
                "\t\t\t\t\"crsCode\": \"EPSG:4326\",\n" +
                "\t\t\t\t\"nBands\": 1,\n" +
                "\t\t\t\t\"rasterBand\": 1,\n" +
                "\t\t\t\t\"valueType\": \"double\"\n" +
                "\t\t\t}\n" +
                "\t\t}";
    }

    public void setFileUI(String path){
        this.uriPath = path;
    }

}
