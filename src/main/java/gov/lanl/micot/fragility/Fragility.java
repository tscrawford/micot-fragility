package gov.lanl.micot.fragility;

import gov.lanl.micot.fragility.lpnorm.*;
import gov.lanl.nisac.fragility.assets.IAsset;
import gov.lanl.nisac.fragility.core.IProperties;
import gov.lanl.nisac.fragility.core.IProperty;
import gov.lanl.nisac.fragility.core.Properties;
import gov.lanl.nisac.fragility.core.Property;
import gov.lanl.nisac.fragility.engine.DefaultExposureEngine;
import gov.lanl.nisac.fragility.engine.DefaultFragilityEngine;
import gov.lanl.nisac.fragility.engine.FragilityEngine;
import gov.lanl.nisac.fragility.engine.IExposureEngine;
import gov.lanl.nisac.fragility.exposure.IExposure;
import gov.lanl.nisac.fragility.exposure.IExposureEvaluator;
import gov.lanl.nisac.fragility.exposure.PointExposureEvaluator;
import gov.lanl.nisac.fragility.gis.IFeature;
import gov.lanl.nisac.fragility.gis.RasterField;
import gov.lanl.nisac.fragility.hazards.HazardField;
import gov.lanl.nisac.fragility.hazards.IHazardField;
import gov.lanl.nisac.fragility.io.AssetData;
import gov.lanl.nisac.fragility.io.AssetDataStore;
import gov.lanl.nisac.fragility.io.ExposureData;
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
import gov.lanl.nisac.fragility.responseEstimators.IResponseEstimator;
import gov.lanl.nisac.fragility.responseEstimators.ResponseEstimatorFactory;
import gov.lanl.nisac.fragility.responseEstimators.wind.ep.PowerPoleWindStressEstimator;
import gov.lanl.nisac.fragility.responseModels.IResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A main class for the Fragility framework. The class reads the input and
 * builds the data objects. It then delegates execution to an instance of a
 * class that implements FragilityEngine. This allows customization of how
 * models are executed within the framework.
 */
public class Fragility{

	private static FragilityEngine responseEngine;
	private static IExposureEngine exposureEngine;

	//private static String inputfile;
	private static String outputfile;

	private static AssetDataStore assetDataStore;
	private static HazardFieldDataStore hazardFieldDataStore;
	private static ResponseEstimatorDataStore responseEstimatorDataStore;

	private static List<IResponse> responses = new ArrayList<>();
	private static List<IExposure> exposures = new ArrayList<>();

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final JsonFactory jsonFactory = new JsonFactory();
	private static final ResponseEstimatorFactory estimatorFactory = new ResponseEstimatorFactory();
	private static boolean exposureOnly = false;

	// lpnorm --
	private static boolean rdtData = false;
	private static boolean windFieldData = false;
	private static boolean poleData = false;
	private static String rdtPath;
	private static String hazardFieldPath;
	private static String polesPath;

	private static RDTData rdt;
	private static GenericData gp;


	public Fragility() {
	}

	public static void main(String[] args) {
		startUp(args);
		run();
		outputResults();
	}

	private static void startUp(String[] args) {

		gp = new GenericData();

		// Parse the command line.
		FragilityCommandLineParser clp = new FragilityCommandLineParser(args);

		exposureOnly = clp.isExposureOnly();
		boolean validateInput = clp.isValidateInput();

		// lpnorm --

		if (clp.hasOutputFile()) {
			outputfile = clp.getOutputPath();
		} else {
			outputfile = "OUTPUT.json";
		}
		rdtData = clp.hasRdt();
		if (rdtData) {
			polesPath = clp.getRdtInputPath();
		}

		poleData = clp.hasPoles();
		if (poleData) {
			polesPath = clp.getPolesInputPath();
		}

		windFieldData = clp.hasWindField();
		if (windFieldData) {
			polesPath = clp.getWindFieldInputPath();
		}

		String schemaURI = clp.getSchemaURI();

		// Configure the fragility and exposure engine instances.
		exposureEngine = new DefaultExposureEngine();
		responseEngine = new DefaultFragilityEngine();

		// checking lpnorm option dependencies
		checkingAndLoadingOptions(clp);

		// Read the input data. Validate against schema if required.
		InputStream instream = null;
		JSONSchemaValidator validator = null;
		if (poleData) {
			try {

				instream = new FileInputStream(clp.getPolesInputPath());

				if (!rdtData) { // lpnorm --

					if (validateInput) {
						URL schemaURL = new URL(schemaURI);
						validator = new JacksonJSONSchemaValidator(schemaURL, true);
						JSONSchemaValidatorReport report = validator.validate(instream);
						if (!report.isSuccess()) {
							System.out.println(report);
						}
						// Reset input stream for reading.
						//instream = new FileInputStream(inputfile);
					}
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static void checkingAndLoadingOptions(FragilityCommandLineParser parser){

		if( rdtData && !poleData && !windFieldData){
			System.out.println("Missing the hazard field option...");
			System.exit(0);
		}

		if( poleData && windFieldData){
			System.out.println("Hazard field is included within option -p");
			System.exit(0);
		}

		if (windFieldData && rdtData && !poleData){
			System.out.println("Hazard field and RDT only options....");
			rdt = readRDT(parser.getRdtInputPath());
			inferPoleData(rdt);
		}
		else if(rdtData && poleData){
			System.out.println("RDT and Pole data options...");
			rdt = readRDT(parser.getRdtInputPath());
			InputStream instream = null;

			try {
				instream = new FileInputStream(parser.getPolesInputPath());
				JsonNode root = readJSONInput(instream);

				//Parse the asset data.
				parseAssetData(root.findValue("assets"));
				System.out.println(assetDataStore.size() + " assets stored.");

				// Parse the hazard data.
				parseHazardFields(root.findValue("hazardFields"));
				System.out.println(hazardFieldDataStore.size() + " hazard fields stored.");

				// Parse the response estimator data if present.
				JsonNode responseEstimatorRoot = root.findValue("responseEstimators");
				if (responseEstimatorRoot != null) {
					parseResponseEstimators(responseEstimatorRoot);
					System.out.println(responseEstimatorDataStore.size() + " response estimators instantiated.");
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else if(windFieldData && rdtData && poleData){
			InputStream instream = null;

			try {
				instream = new FileInputStream(parser.getPolesInputPath());
				JsonNode root = readJSONInput(instream);

				//Parse the asset data.
				parseAssetData(root.findValue("assets"));
				System.out.println(assetDataStore.size() + " assets stored.");

				// Parse the hazard data.
				parseHazardFields(root.findValue("hazardFields"));
				System.out.println(hazardFieldDataStore.size() + " hazard fields stored.");

				// Parse the response estimator data if present.
				JsonNode responseEstimatorRoot = root.findValue("responseEstimators");
				if (responseEstimatorRoot != null) {
					parseResponseEstimators(responseEstimatorRoot);
					System.out.println(responseEstimatorDataStore.size() + " response estimators instantiated.");
				}

				outputfile = "OUT.json";

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			rdt = readRDT(parser.getRdtInputPath());

		} else if( poleData && (!rdtData && !windFieldData)){
			System.out.println("Only pole data...");

			InputStream instream = null;

			try {
				instream = new FileInputStream(parser.getPolesInputPath());
				JsonNode root = readJSONInput(instream);

				//Parse the asset data.
				parseAssetData(root.findValue("assets"));
				System.out.println(assetDataStore.size() + " assets stored.");

				// Parse the hazard data.
				parseHazardFields(root.findValue("hazardFields"));
				System.out.println(hazardFieldDataStore.size() + " hazard fields stored.");

				// Parse the response estimator data if present.
				JsonNode responseEstimatorRoot = root.findValue("responseEstimators");
				if (responseEstimatorRoot != null) {
					parseResponseEstimators(responseEstimatorRoot);
					System.out.println(responseEstimatorDataStore.size() + " response estimators instantiated.");
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

	private static void inferPoleData(RDTData rdtFile) {
		System.out.println("Producing generic pole data . . .");
		assetDataStore = new AssetDataStore();

		List<RDTLines> rdtline = rdtFile.getLines();
		List<RDTBuses> rdtbuses = rdtFile.getBuses();

		//GenericData gp = new GenericData();

		HashMap<String, List<Double>> ht = new HashMap<>();

		String bid=null;

		for (RDTBuses bus : rdtbuses) {
			bid = bus.getId();
			ht.put(bid, new ArrayList<>());
			ht.get(bid).add(new Double(bus.getX()));
			ht.get(bid).add(new Double(bus.getY()));

			gp.setId(bid);
			gp.setLat(String.valueOf(bus.getY()));
			gp.setLon(String.valueOf(bus.getX()));
			gp.setCableSpan("90.000");

			try  {
				JsonParser jp = jsonFactory.createParser(gp.getPoleString());
				AssetData aData = objectMapper.readValue(jp, AssetData.class);
				IAsset asset = aData.createAsset();
				assetDataStore.addAsset(asset);

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// adding poles between buses
		String node1=null;
		String node2=null;
		double v1,v2,x0,y0 ;
		double ndist, numPoles, poleSpan;
		int id_count=0;

		for (RDTLines line : rdtline){
			node1 = line.getNode1_id();
			node2 = line.getNode2_id();
			x0 = ht.get(node1).get(0);
			y0 = ht.get(node1).get(1);

			v1 = ht.get(node2).get(0) - ht.get(node1).get(0);
			v2 = ht.get(node2).get(1) - ht.get(node1).get(1);
			ndist = Math.sqrt(v1*v1+v2*v2);
			// normalized vector
			v1 = v1/ndist;
			v2 = v2/ndist;

			// using 111.32 km per degree or 111,320 meters
			// pole spacing at 91 meters 0.000817463169242 degrees
			//0.000817463169242
			numPoles = Math.floor(ndist*111320)/ 91.0;
			numPoles = Math.floor(numPoles);
			id_count = 0;
			for(int i=0; i < numPoles; i++){

				gp.setId(node1+"-"+node2+"-"+id_count);
				gp.setLat(String.valueOf(y0));
				gp.setLon(String.valueOf(x0));
				gp.setCableSpan("90.0000");

				try  {
					JsonParser jp = jsonFactory.createParser(gp.getPoleString());
					AssetData aData = objectMapper.readValue(jp, AssetData.class);
					IAsset asset = aData.createAsset();
					assetDataStore.addAsset(asset);

				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// move next pole location
				x0 = x0+v1*0.000817463169242;
				y0 = y0+v2*0.000817463169242;
				id_count = id_count+1;

			}
			// spacing at 91 meters ~ 300 ft
		}


		IResponseEstimator responseEstimator = new  PowerPoleWindStressEstimator("PowerPoleWindStressEstimator");;
		responseEstimatorDataStore = new ResponseEstimatorDataStore();
		responseEstimatorDataStore.addResponseEstimator(responseEstimator);

		hazardFieldDataStore = new HazardFieldDataStore();

		JsonParser parser = null;
		gp.setFileUI("C:/Users/301338/Desktop/PROJECTS/fragility/micot-fragility/target/wf_clip.asc");

		try {
			parser = jsonFactory.createParser(gp.getHazardFields());
			HazardFieldData hazardFieldData = objectMapper.readValue(parser, HazardFieldData.class);
			String id = hazardFieldData.getId();
			String hazardQuantityType = hazardFieldData.getHazardQuantityType();
			RasterFieldData rasterFieldData = hazardFieldData.getRasterFieldData();
			RasterField rasterField = RasterDataFieldFactory.createRasterField(rasterFieldData);
			IHazardField hazardField = new HazardField(id, hazardQuantityType, rasterField);
			hazardFieldDataStore.addHazardField(hazardField);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(hazardFieldDataStore.size() + " hazard fields stored.");
	}

	private static void run() {

		if (exposureOnly) {
			IExposureEvaluator exposureEvaluator = new PointExposureEvaluator();
			exposures = exposureEngine.execute(assetDataStore, hazardFieldDataStore, exposureEvaluator);
		} else if (rdtData) {
			// probably not necessary -- same call as a non-lpnorm run...
			responses = responseEngine.execute(assetDataStore, hazardFieldDataStore, responseEstimatorDataStore);
		} else {
			responses = responseEngine.execute(assetDataStore, hazardFieldDataStore, responseEstimatorDataStore);
		}

	}

	private static void outputResults() {
		if (exposureOnly) {
			// Output the exposure data.
			writeJSONExposureOutput();
			System.out.println("Asset exposures written.");
		} else {
			// Output the response data.
			writeJSONResponseOutput();

		}
		System.out.println("Analysis complete.");
	}

	/**
	 * write RDT Input file as Output
	 */

	private static void generateRdtInput(ResponseData[] data) {
		// calculate down lines and write RDT input
		// add disabled lines
		Random r = new Random();
		List<String> lineIds = new ArrayList<>();

		Random rand = new Random();
		String uniqueId = null;
		String damagedLine = null;
		int idx1, idx2;

		List<RDTLines> rdtlines = rdt.getLines();
		HashMap<List<String>, String> nodesmap = new HashMap<>();
		List<String> ns;


		for(RDTLines line: rdtlines){
			ns = new ArrayList<>();
			ns.add(line.getNode1_id());
			ns.add(line.getNode2_id());
			nodesmap.put(ns, line.getId());
		}

		int count = 0;
		for (ResponseData rd : data) {

			// if response > rand(0,1) --> line is disabled
			if (rd.getValue() > r.nextFloat()) {

				count+=1;
				uniqueId = rd.getAssetID();

				if (uniqueId.contains("-")){
					idx1 = uniqueId.indexOf("-");
					idx2 = uniqueId.lastIndexOf("-");

					ns = new ArrayList<>();
					ns.add(uniqueId.substring(0,idx1)); // node 1
					ns.add(uniqueId.substring(idx1+1,idx2)); // node 2

					damagedLine = nodesmap.get(ns);

					if (!lineIds.contains(damagedLine)){
						// collect damaged lines
						lineIds.add(damagedLine);
					}

				}
			}
		} // end for loop

		RDTScenarios scene = new RDTScenarios();
		scene.setId("1");
		List<RDTScenarios> lscenario = new ArrayList<>();
		scene.setDisable_lines(new ArrayList<>(lineIds));
		lscenario.add(scene);
		rdt.setScenarios(lscenario);
		writeLpnorm("rdt_"+outputfile, rdt);
	}

	private static void writeLpnorm(String fileName, RDTData rdtFile) {

		try {
			FileOutputStream os = new FileOutputStream(fileName);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, rdtFile);
			os.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			System.out.println("RDT JSON processing issue.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not write RDT input.");
		}

	}

	private static RDTData readRDT(String fileName) {
		// read-in Template RDT file
		RDTData rdt = null;

		try {
			rdt = objectMapper.readValue(new File(fileName), RDTData.class);
		} catch (IOException e) {
			System.out.println("Could not read template RDT input file");
			e.printStackTrace();
		}

		return rdt;
	}

	private static JsonNode readJSONInput(InputStream instream) {
		try {
			return objectMapper.readTree(instream);
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
		if (node.isArray()) {
			for (JsonNode n : node) {
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
		if (node.isArray()) {
			for (JsonNode n : node) {
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
		if (node.isArray()) {
			for (JsonNode n : node) {
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());
					ResponseEstimatorData responseEstimatorData = objectMapper.readValue(parser,
							ResponseEstimatorData.class);
					String id = responseEstimatorData.getId();
					String responseEstimatorClass = responseEstimatorData.getResponseEstimatorClass();
					String assetClass = responseEstimatorData.getAssetClass();
					List<String> hazardQuantityList = new ArrayList<>();
					String[] hazardQuantityTypes = responseEstimatorData.getHazardQuantityTypes();
					Collections.addAll(hazardQuantityList, hazardQuantityTypes);
					String responseQuantityType = responseEstimatorData.getResponseQuantityType();
					Map<String, Object> dataProperties = responseEstimatorData.getProperties();

					Properties properties = new Properties();
					for (String key : dataProperties.keySet()) {
						Property prop = new Property(key, dataProperties.get(key));
						properties.addProperty(prop);
					}
					AbstractResponseEstimator responseEstimator = (AbstractResponseEstimator) estimatorFactory
							.createResponseEstimator(id, responseEstimatorClass, assetClass, hazardQuantityList,
									responseQuantityType, properties);
					responseEstimatorDataStore.addResponseEstimator(responseEstimator);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// TODO Specialized to point exposure values. Needs to be generalized.
	private static void writeJSONExposureOutput() {
		try {
			int nexposures = exposures.size();
			ExposureData[] exposureData = new ExposureData[nexposures];
			for (int i = 0; i < nexposures; i++) {
				IExposure exposure = exposures.get(i);
				ExposureData data = new ExposureData();
				IAsset asset = exposure.getAsset();
				data.setAssetID(asset.getID());
				data.setAssetClass(asset.getAssetClass());
				IHazardField hazardField = exposure.getHazardField();
				data.setHazardQuantityType(hazardField.getHazardQuantityType());
				IFeature feature = exposure.getExposure().getFeature(0);
				IProperty exposureProperty = feature.getProperty("exposure");
				data.setValue((double) exposureProperty.getValue());
				exposureData[i] = data;
			}
			FileOutputStream os = new FileOutputStream("fragility_"+outputfile);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, exposureData);
			os.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeJSONResponseOutput() {

		try {
			int nresponses = responses.size();
			ResponseData[] responseData = new ResponseData[nresponses];
			for (int i = 0; i < nresponses; i++) {
				IResponse response = responses.get(i);
				ResponseData data = new ResponseData();
				data.setAssetID(response.getAssetID());
				data.setAssetClass(response.getAssetClass());
				data.setHazardQuantityType(response.getHazardQuantityType());
				data.setResponseQuantityType(response.getResponseQuantityType());
				data.setValue(response.getValue());
				responseData[i] = data;
			}

			if (rdtData) {
				System.out.println("Writing RDT input.");
				generateRdtInput(responseData);
				FileOutputStream os = new FileOutputStream("fragility_"+outputfile);
				objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, responseData);
				os.close();
				System.out.println("Asset responses written.");

			} else {
				FileOutputStream os = new FileOutputStream("fragility_"+outputfile);
				objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, responseData);
				os.close();
				System.out.println("Asset responses written.");
			}

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
