package gov.lanl.micot.fragility;

import gov.lanl.nisac.fragility.assets.IAsset;
import gov.lanl.nisac.fragility.core.Properties;
import gov.lanl.nisac.fragility.core.Property;
import gov.lanl.nisac.fragility.engine.DefaultFragilityEngine;
import gov.lanl.nisac.fragility.engine.FragilityEngine;
import gov.lanl.nisac.fragility.gis.RasterField;
import gov.lanl.nisac.fragility.hazards.HazardField;
import gov.lanl.nisac.fragility.hazards.IHazardField;
import gov.lanl.nisac.fragility.io.AssetData;
import gov.lanl.nisac.fragility.io.AssetDataStore;
import gov.lanl.nisac.fragility.io.HazardFieldData;
import gov.lanl.nisac.fragility.io.HazardFieldDataStore;
import gov.lanl.nisac.fragility.io.RasterDataFieldFactory;
import gov.lanl.nisac.fragility.io.RasterFieldData;
import gov.lanl.nisac.fragility.io.ResponseData;
import gov.lanl.nisac.fragility.io.ResponseEstimatorData;
import gov.lanl.nisac.fragility.io.ResponseEstimatorDataStore;
import gov.lanl.nisac.fragility.io.validators.JSONSchemaValidator;
import gov.lanl.nisac.fragility.io.validators.JSONSchemaValidatorReport;
import gov.lanl.nisac.fragility.io.validators.JacksonJSONSchemaValidator;
import gov.lanl.nisac.fragility.responseEstimators.AbstractResponseEstimator;
import gov.lanl.nisac.fragility.responseEstimators.ResponseEstimatorFactory;
import gov.lanl.nisac.fragility.responseModels.IResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A main class for the Fragility framework. The class reads the input and builds the data objects. 
 * It then delegates execution to an instance of a class that implements FragilityEngine. This allows
 * customization of how models are executed within the framework.
 */
public class Fragility {
	
	private static FragilityEngine engine;
	
	private static String inputfile;
	
	private static String outputfile;
	
	private static AssetDataStore assetDataStore;

	private static HazardFieldDataStore hazardFieldDataStore;

	private static ResponseEstimatorDataStore responseEstimatorDataStore;
	
	private static List<IResponse> responses = new ArrayList<>();

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final JsonFactory jsonFactory = new JsonFactory();

	private static final ResponseEstimatorFactory estimatorFactory = new ResponseEstimatorFactory();

	
	public Fragility(){}
	
	public static void main(String[] args){
		startUp(args);
		run();
		output();
	}

	private static void startUp(String[] args) {
		// Usage is:
		//     SimpleFragilityEngine inputFile outputFile <-schema schemaURL>
		if(args.length<2){
			System.err.println("Usage is SimpleFragilityEngine inputFile outputFile <-schema schemaURL>");
			System.exit(1);
		}

		inputfile = args[0];
		outputfile = args[1];

		String schemaKey = null;
		String schemaURLVal = null;		
		URL schemaURL = null;
		boolean validate = false;

		if(args.length==4){
			schemaKey = args[2];
			schemaURLVal = args[3];
			if(schemaKey.equals("-schema")){
				validate = true;
			} else {
				System.err.println("Validation option -schema schemaURL expected.");
				System.exit(1);
			}
		}
		
		// Configure the fragility engine instance.
		// TODO This is fixed to the default engine for now, but will use a creation pattern based on
		// configuration input later.
		engine = new DefaultFragilityEngine();

		// Read the input data. Validate against schema if required.
		InputStream instream = null;
		JSONSchemaValidator validator = null;
		try {
			instream = new FileInputStream(inputfile);
			if(validate){
				schemaURL = new URL(schemaURLVal);
				validator = new JacksonJSONSchemaValidator(schemaURL,true);
				JSONSchemaValidatorReport report = validator.validate(instream);
				if(!report.isSuccess()){
					System.out.println(report);
				}
			}

			JsonNode root = readJSONInput(instream);
			System.out.println("Input read successfully.");

			// Parse the asset data.
			parseAssetData(root.findValue("assets"));
			System.out.println(assetDataStore.size()+" assets stored.");

			// Parse the hazard data.
			parseHazardFields(root.findValue("hazardFields"));
			System.out.println(hazardFieldDataStore.size()+" hazard fields stored.");

			// Parse the response estimator data.
			parseResponseEstimators(root.findValue("responseEstimators"));
			System.out.println(responseEstimatorDataStore.size()+" response estimators instantiated.");
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private static void run() {
		responses = engine.execute(assetDataStore, hazardFieldDataStore, responseEstimatorDataStore);
		
	}

	private static void output() {
		// Output the response data.
		writeJSONOutput(outputfile, responses);
		System.out.println("Fragility results written. Fragility analysis complete.");		
	}
	
	private static JsonNode readJSONInput(InputStream instream) {
		try {
			JsonNode root = objectMapper.readTree(instream);
			return root;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void parseAssetData(JsonNode node) {
		assetDataStore = new AssetDataStore();
		if(node.isArray()){
			for(JsonNode n: node){
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());					
					AssetData aData = objectMapper.readValue(parser, AssetData.class);
					IAsset asset = aData.createAsset();
					assetDataStore.addAsset(asset);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void parseHazardFields(JsonNode node) {
		hazardFieldDataStore = new HazardFieldDataStore();
		if(node.isArray()){
			for(JsonNode n: node){
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());					
					HazardFieldData hazardFieldData = objectMapper.readValue(parser, HazardFieldData.class);
					String id = hazardFieldData.getId();
					String hazardQuantityType = hazardFieldData.getHazardQuantityType();
					RasterFieldData rasterFieldData = hazardFieldData.getRasterFieldData();
					RasterField rasterField = RasterDataFieldFactory.createRasterField(rasterFieldData);
					IHazardField hazardField = new HazardField(id, hazardQuantityType, rasterField);
					hazardFieldDataStore.addHazardField(hazardField);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}

	private static void parseResponseEstimators(JsonNode node) {
		responseEstimatorDataStore = new ResponseEstimatorDataStore();
		if(node.isArray()){
			for(JsonNode n: node){
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());					
					ResponseEstimatorData responseEstimatorData =
							objectMapper.readValue(parser, ResponseEstimatorData.class);
					String id = responseEstimatorData.getId();
					String responseEstimatorClass = responseEstimatorData.getResponseEstimatorClass();
					String assetClass = responseEstimatorData.getAssetClass();							
					List<String> hazardQuantityList = new ArrayList<>();
					String[] hazardQuantityTypes = responseEstimatorData.getHazardQuantityTypes();
					for(String type: hazardQuantityTypes){hazardQuantityList.add(type);}
					String responseQuantityType = responseEstimatorData.getResponseQuantityType();				
					Map<String, Object> dataProperties = responseEstimatorData.getProperties();

					Properties properties = new Properties();
					for(String key: dataProperties.keySet()){
						Property prop = new Property(key,dataProperties.get(key));
						properties.addProperty(prop);
					}
					AbstractResponseEstimator responseEstimator =
							(AbstractResponseEstimator) estimatorFactory.createResponseEstimator(id,
									responseEstimatorClass, assetClass, hazardQuantityList, responseQuantityType,
									properties);
					responseEstimatorDataStore.addResponseEstimator(responseEstimator);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		

	}
	
	private static void writeJSONOutput(String outputfile, List<IResponse> responses) {
		try {
			int nresponses = responses.size();
			ResponseData[] responseData = new ResponseData[nresponses];
			for(int i=0;i<nresponses;i++){
				IResponse response = responses.get(i);
				ResponseData data = new ResponseData();
				data.setAssetID(response.getAssetID());
				data.setAssetClass(response.getAssetClass());
				data.setHazardQuantityType(response.getHazardQuantityType());
				data.setResponseQuantityType(response.getResponseQuantityType());
				data.setValue(response.getValue());
				//String s = objectMapper.writerWithDefaultPrettyPrinter().withView(ResponseData.class).writeValueAsString(data);
				//System.out.println(s);
				responseData[i] = data;
			}
			FileOutputStream os = new FileOutputStream(outputfile);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, responseData);
			os.close();
		}catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


}
