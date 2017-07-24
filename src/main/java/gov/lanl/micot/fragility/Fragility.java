package gov.lanl.micot.fragility;

import gov.lanl.micot.fragility.lpnorm.Poles.*;
import gov.lanl.micot.fragility.lpnorm.RDT.*;
import gov.lanl.micot.fragility.FragilityCommandLineParser;


import gov.lanl.nisac.fragility.assets.IAsset;
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
import java.security.Key;
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

	private static RDTData rdt;
	private static PoleData polesData;
	private static HashMap<String,String> nodeToLine;
	private static FragilityCommandLineParser clp;


	public Fragility() {
	}

	public static void main(String[] args) {

		startUp(args);
		run();
		outputResults();

	}

	private static void startUp(String[] args) {


		// Parse the command line.
		clp = new FragilityCommandLineParser(args);

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
			String rdtPath = clp.getRdtInputPath();
		}

		poleData = clp.hasPoles();
		String polesPath;
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

		if (windFieldData && rdtData){
			System.out.println("Hazard field and RDT only options....");
			rdt = readRDT(parser.getRdtInputPath());
			inferPoleData(rdt);
		}
		else if(rdtData &&  !windFieldData){
			System.out.println("RDT and Pole data options...");
			rdt = readRDT(parser.getRdtInputPath());
			InputStream instream = null;
			System.out.println(parser.getPolesInputPath());
			inferPoleData(rdt);

			try {
				instream = new FileInputStream(parser.getPolesInputPath());
				JsonNode root = readJSONInput(instream);

				//Parse the asset data.
				assert root != null;
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

		else if( poleData ){
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

	private static PoleAssets definePole(int id, String line, double[] coord){

		PoleAssets pole = new PoleAssets();
		PoleProperties poleProperty = new PoleProperties();

		pole.setAssetGeometry(new PoleAssetGeometry());
		pole.getAssetGeometry().setCoordinates(coord);
		pole.getAssetGeometry().setType("Point");
		pole.setAssetClass("PowerDistributionPole");
		pole.setId(String.valueOf(id));
		pole.setProperties( poleProperty );

		poleProperty.setBaseDiameter((float) 0.2222);
		poleProperty.setCableSpan((float) 90.0);
		poleProperty.setCommAttachmentHeight((float) 4.7244);
		poleProperty.setCommCableDiameter((float) 0.04 );
		poleProperty.setCommCableNumber(2);
		poleProperty.setCommCableWireDensity((float) 2700.0);
		poleProperty.setHeight((float)9.144);
		poleProperty.setMeanPoleStrength((float) 38600000.0);
		poleProperty.setPowerAttachmentHeight((float) 5.6388);
		poleProperty.setPowerCableDiameter((float)0.0094742 );
		poleProperty.setPowerCableNumber(2);
		poleProperty.setPowerCableWireDensity((float) 2700.0);

		poleProperty.setLineId(line);
		poleProperty.setPowerCircuitName(String.valueOf(id));
		poleProperty.setStdDevPoleStrength((float) 7700000.0);
		poleProperty.setTopDiameter((float) 0.15361635107);
		poleProperty.setWoodDensity((float) 500.0);

		return pole;
	}



	private static void inferPoleData(RDTData rdtFile) {
		System.out.println("Producing generic pole data . . .");
		assetDataStore = new AssetDataStore();

		List<RDTLines> rdtline = rdtFile.getLines();
		HashMap<String,String> nodeToLine = new HashMap<>();

		for (RDTLines i : rdtline){
			nodeToLine.put(i.getNode1_id(), i.getId());
			nodeToLine.put(i.getNode2_id(), i.getId());
		}

		List<RDTBuses> rdtbuses = rdtFile.getBuses();
		List<PoleAssets> assets = new ArrayList();
		PoleAssets npa;

		HashMap<String, List<Double>> ht = new HashMap<>();
		int id_count=0;
		String bid,name=null;

		for (RDTBuses bus : rdtbuses) {
			bid = bus.getId();
			ht.put(bid, new ArrayList<>());
			ht.get(bid).add(new Double(bus.getX()));
			ht.get(bid).add(new Double(bus.getY()));


			try  {

				String busId = nodeToLine.get(bus.getId());
				npa = definePole(id_count, busId, new double[]{bus.getX(), bus.getY()});
				assets.add(npa);
				String npastr = objectMapper.writeValueAsString(npa);
				JsonParser jp = jsonFactory.createParser(npastr);
				AssetData aData = objectMapper.readValue(jp, AssetData.class);
				IAsset asset = aData.createAsset();
				assetDataStore.addAsset(asset);


			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			id_count +=1;
		}// end for

		// adding poles between buses
		String node1=null;
		String node2=null;
		String lid=null;
		double v1,v2,x0,y0 ;
		double ndist, numPoles;


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

			// using 111.111 km per degree or 111,111 meters
			// pole spacing at 91 meters 0.000817463169242 degrees
			// 0.000817463169242
			numPoles = Math.floor(ndist*111111)/ 91.0;
			numPoles = Math.floor(numPoles);

//			id_count = 0;
			for(int i=0; i < numPoles; i++){
				lid = line.getId();

				try  {
					npa = definePole(id_count, lid, new double[]{x0, y0});
					assets.add(npa);

					String npastr = objectMapper.writeValueAsString(npa);
					JsonParser jp = jsonFactory.createParser(npastr);

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
		}// end for

		System.out.println("Number of poles created: "+assets.size());

		polesData = new PoleData();
		polesData.setAssets(assets);



		IResponseEstimator responseEstimator = new  PowerPoleWindStressEstimator("PowerPoleWindStressEstimator");;
		responseEstimatorDataStore = new ResponseEstimatorDataStore();
		responseEstimatorDataStore.addResponseEstimator(responseEstimator);

		hazardFieldDataStore = new HazardFieldDataStore();

		if(!poleData) {

			JsonParser parser = null;
			String urlpath = null;
			try {
				urlpath = String.valueOf(new File(System.getProperty("user.dir")).toURI().toURL());
				urlpath = urlpath.substring(6);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			urlpath = urlpath + clp.getWindFieldInputPath();

			List<PoleHazardFields> hfl = new ArrayList<>();
			PoleHazardFields phf = new PoleHazardFields();
			phf.setRasterFieldData(new PoleRasterFieldData());
			phf.getRasterFieldData().setCrsCode("EPSG:4326");
			phf.getRasterFieldData().setGridFormat("ArcGrid");
			phf.getRasterFieldData().setnBands(1);
			phf.getRasterFieldData().setRasterBand(1);
			phf.getRasterFieldData().setValueType("double");
			phf.getRasterFieldData().setUri("file:///" + urlpath);
			phf.setId("GFM-generated-ID");

			List<PoleResponseEstimators> pre = new ArrayList<>();
			PoleResponseEstimators pr = new PoleResponseEstimators();
			pr.setId("PowerPoleWindStressEstimator");
			pr.setResponseEstimatorClass("PowerPoleWindStressEstimator");
			pr.setAssetClass("PowerDistributionPole");
			pr.setProperties(new PoleResponseEstimatorsProperties());

			List<String> ls = new ArrayList<>();
			ls.add("Windspeed");
			pr.setHazardQuantityTypes(ls);
			pr.setResponseQuantityType("DamageProbability");
			pre.add(pr);

			phf.setHazardQuantityType("Windspeed");
			hfl.add(phf);
			polesData.setResponseEstimators(pre);
			polesData.setHazardFields(hfl);
			writePoleData("RDT-to-Poles.json", polesData);

			// Fragility's hazard class
			HazardFieldData hdd = new HazardFieldData();
			hdd.setRasterFieldData(new RasterFieldData());
			hdd.getRasterFieldData();
			hdd.getRasterFieldData().setUri("file:///" + urlpath);
			hdd.getRasterFieldData().setnBands(1);
			hdd.getRasterFieldData().setRasterBand(1);
			hdd.getRasterFieldData().setCrsCode("EPSG:4326");
			hdd.setHazardQuantityType("Windspeed");
			hdd.getRasterFieldData().setGridFormat("ArcGrid");
			hdd.getRasterFieldData().setValueType(Double.class);
			hdd.setId("TesGrid");

			try {
				parser = jsonFactory.createParser(objectMapper.writeValueAsString(hdd));
				HazardFieldData hazardFieldData = objectMapper.readValue(parser, HazardFieldData.class);
				String id = hazardFieldData.getId();
				String hazardQuantityType = hazardFieldData.getHazardQuantityType();
				RasterFieldData rasterFieldData = hdd.getRasterFieldData();
				RasterField rasterField = RasterDataFieldFactory.createRasterField(rasterFieldData);
				IHazardField hazardField = new HazardField(id, hazardQuantityType, rasterField);
				hazardFieldDataStore.addHazardField(hazardField);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println(hazardFieldDataStore.size() + " hazard fields stored.");
		}
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
		} else if(poleData){
			writeJSONResponseOutput();
		}
			else {
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

		//List<String> lineIds = new ArrayList<>();

		String uniqueId = null;
		String damagedLine = null;
		int idx1;

		List<RDTLines> rdtlines = rdt.getLines();
		HashMap<List<String>, String> nodesmap = new HashMap<>();
		List<String> ns;

		for(RDTLines line: rdtlines){
			ns = new ArrayList<>();
			ns.add(line.getId());
			nodesmap.put(ns, line.getId());
		}

		int numTimes = 0;

		if (clp.isNumScenarios())numTimes = clp.getNumberOfScenarios();
		else numTimes = 1;

		int timeCt = 0;
		RDTScenarios scene;
		List<RDTScenarios> lscenario = new ArrayList<>();
		List<String> lineIds;
		Random r = new Random();

		HashMap<String, String> poleToLine = new HashMap<>();
		for (PoleAssets pa : polesData.getAssets()) poleToLine.put(pa.getId(), pa.getProperties().getLineId());

		do {

			lineIds = new ArrayList<>();
			scene =  new RDTScenarios();

			for (ResponseData rd : data) {

				// if response > rand(0,1) --> line is disabled


				if (rd.getValue() > r.nextFloat()) {

					uniqueId = poleToLine.get(rd.getAssetID());

					ns = new ArrayList<>();
					ns.add(uniqueId); // line id
					damagedLine = nodesmap.get(ns);

					if (!lineIds.contains(damagedLine)){
							// collect damaged lines
							lineIds.add(damagedLine);
					}
				}
			} // end for loop

			scene.setId(String.valueOf(timeCt));
			scene.setDisable_lines(new ArrayList<>(lineIds));
			lscenario.add(scene);
			timeCt +=1;

		}while(timeCt < numTimes);

		rdt.setScenarios(lscenario);
		writeLpnorm("rdt_"+outputfile, rdt);

	}

	private static void writePoleData(String fileName, PoleData pd) {

		try {
			FileOutputStream os = new FileOutputStream(fileName);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, pd);
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

	private static PoleData readPoleData(String fileName) {
		// read-in Template RDT file
		PoleData pd = null;

		try {
			pd = objectMapper.readValue(new File(fileName), PoleData.class);
		} catch (IOException e) {
			System.out.println("Could not read template RDT input file");
			e.printStackTrace();
		}

		return pd;
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

			} else if (poleData){
//				generateRdtInput(responseData);
				generateRdtScenarioBlock(responseData);
				FileOutputStream os = new FileOutputStream("fragility_"+outputfile);
				objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, responseData);
				os.close();
				System.out.println("Asset responses written.");
			}
			else {
				generateRdtInput(responseData);
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

	private static void generateRdtScenarioBlock(ResponseData[] data) {

		int numTimes;
		int timeCt = 0;

		if (clp.isNumScenarios()) numTimes = clp.getNumberOfScenarios();
		else numTimes = 1;

		RDTScenarios rs = new RDTScenarios();
		rs.setDisable_lines(new ArrayList<>());
		Random r = new Random();

		List<String> ns;
		RDTScenarios scene = null;
		List<RDTScenarios> lscenario = new ArrayList<>();

		do {
			scene =  new RDTScenarios();
			ns = new ArrayList<>();

			for (ResponseData rd : data) {

				// if response > rand(0,1) --> line is disabled
				if (rd.getValue() > r.nextFloat()) {
					String lineId = null;
					lineId =assetDataStore.getAsset("PowerDistributionPole",rd.getAssetID()).getID();
					if (!ns.contains(lineId)){
						// collect damaged lines
						ns.add(lineId);
					}
				}
			} // end for loop

			scene.setId(String.valueOf(timeCt));
			scene.setDisable_lines(new ArrayList<>(ns));
			lscenario.add(scene);
			timeCt +=1;

		}while(timeCt < numTimes);

		RDTScenarioBlock rsb = new RDTScenarioBlock();
		rsb.setScenarios(lscenario);

		try {
			FileOutputStream os = new FileOutputStream("RDTScenarioBlock.json");
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, rsb);
			os.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			System.out.println("RDTScenarioBlock JSON processing issue.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not write RDTScenarioBlock.");
		}
	}
}
