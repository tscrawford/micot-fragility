package gov.lanl.micot.fragility;

import gov.lanl.micot.fragility.lpnorm.NetworkDataStore;
import gov.lanl.micot.fragility.lpnorm.NetworkLine;
import gov.lanl.micot.fragility.lpnorm.NetworkNode;
import gov.lanl.micot.fragility.lpnorm.RDTData;
import gov.lanl.micot.fragility.lpnorm.RDTScenarios;
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
import gov.lanl.nisac.fragility.responseEstimators.ResponseEstimatorFactory;
import gov.lanl.nisac.fragility.responseModels.IResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
public class Fragility {

	private static FragilityEngine responseEngine;

	private static IExposureEngine exposureEngine;

	private static String inputfile;

	private static String outputfile;

	private static AssetDataStore assetDataStore;

	private static NetworkDataStore networkDataStore; // network topology

	private static HazardFieldDataStore hazardFieldDataStore;

	private static ResponseEstimatorDataStore responseEstimatorDataStore;

	private static List<IResponse> responses = new ArrayList<>();

	private static List<IExposure> exposures = new ArrayList<>();

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final JsonFactory jsonFactory = new JsonFactory();

	private static final ResponseEstimatorFactory estimatorFactory = new ResponseEstimatorFactory();

	private static String schemaURI = null;

	private static String lpnormNetwork = null;

	private static boolean exposureOnly = false;

	private static boolean validateInput = false;

	private static boolean lpnormOnly = false;

	private static ResponseData[] responseData;

	public Fragility() {
	}

	public static void main(String[] args) {
		startUp(args);
		run();
		outputResults();
	}

	private static void startUp(String[] args) {

		// Parse the command line.
		FragilityCommandLineParser parser = new FragilityCommandLineParser(args);
		inputfile = parser.getInputPath();
		outputfile = parser.getOutputPath();
		exposureOnly = parser.isExposureOnly();
		validateInput = parser.isValidateInput();
		lpnormOnly = parser.isLpnormOnly();

		schemaURI = parser.getSchemaURI();
		lpnormNetwork = parser.getLpnormNetwork();

		// Configure the fragility and exposure engine instances.
		exposureEngine = new DefaultExposureEngine();
		responseEngine = new DefaultFragilityEngine();

		// Read the input data. Validate against schema if required.
		InputStream instream = null;
		InputStream instreamLpnorm = null;

		JSONSchemaValidator validator = null;
		try {
			instream = new FileInputStream(inputfile);
			if (validateInput) {
				URL schemaURL = new URL(schemaURI);
				validator = new JacksonJSONSchemaValidator(schemaURL, true);
				JSONSchemaValidatorReport report = validator.validate(instream);
				if (!report.isSuccess()) {
					System.out.println(report);
				}
				// Reset input stream for reading.
				instream = new FileInputStream(inputfile);
			}

			JsonNode root = readJSONInput(instream);
			System.out.println("Input read successfully.");

			// Parse the asset data.
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

			// LPNORM Option --
			if (lpnormOnly) {

				// temporary LPNORM option to read-in Power Network topology
				System.out.println("*** Running LPNORM option *** ");
				instreamLpnorm = new FileInputStream(lpnormNetwork);

				JsonNode rootlpnorm = readJSONInput(instreamLpnorm);
				System.out.println("Network data read successfully.");

				// Parse the asset data.
				parseNetworkTopology(rootlpnorm.findValue("graphs"));

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private static void run() {
		if (exposureOnly) {
			IExposureEvaluator exposureEvaluator = new PointExposureEvaluator();
			exposures = exposureEngine.execute(assetDataStore, hazardFieldDataStore, exposureEvaluator);
		} else if (lpnormOnly) {
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

		Random r = new Random();
		List<String> lineIds = new ArrayList<String>();

		for (ResponseData rd : data) {

			// if response > rand(0,1) --> line is disabled
			if (rd.getValue() > r.nextFloat()) {

				// get damaged line(s)
				List<String> lineItr = networkDataStore.getNodeLineId(rd.getAssetID());

				if (!lineItr.isEmpty()) {
					lineItr.forEach((damagedLine) -> {
						if (!lineIds.contains(damagedLine)) {
							// collecting damaged lines
							lineIds.add(damagedLine);
						}
					});
				}
			}
		} // end for loop

		// read-in Template RDT file
		RDTData rdt = null;
		RDTData rdtFile = readRDT("RDT_template.json");

		// add disabled lines
		RDTScenarios scene = new RDTScenarios();
		scene.setId("1");
		List<RDTScenarios> lscenario = new ArrayList<RDTScenarios>();
		scene.setDisable_lines(new ArrayList<String>(lineIds));
		lscenario.add(scene);
		rdtFile.setScenarios(lscenario);

		writeLpnorm(outputfile, rdtFile);

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

	private static void parseNetworkTopology(JsonNode node) {
		networkDataStore = new NetworkDataStore();
		NetworkNode singleNode = null;
		NetworkLine singleLine = null;

		if (node.isArray()) {
			JsonNode lpnormLines = node.findValue("lines");
			JsonNode lpnormNodes = node.findValue("nodes");

			// storing network lines
			for (JsonNode n : lpnormLines) {
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());
					singleLine = objectMapper.readValue(parser, NetworkLine.class);
					networkDataStore.addLine(singleLine);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// storing network nodes
			for (JsonNode n : lpnormNodes) {
				try {
					JsonParser parser = jsonFactory.createParser(n.toString());
					singleNode = objectMapper.readValue(parser, NetworkNode.class);
					networkDataStore.addNode(singleNode);
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
					for (String type : hazardQuantityTypes) {
						hazardQuantityList.add(type);
					}
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
			FileOutputStream os = new FileOutputStream(outputfile);
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
			responseData = new ResponseData[nresponses];
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
			if (lpnormOnly) {

				System.out.println("Writing RDT input.");
				generateRdtInput(responseData);

			} else {
				FileOutputStream os = new FileOutputStream(outputfile);
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

	public static String getLpnormNetwork() {
		return lpnormNetwork;
	}

	public static void setLpnormNetwork(String lpnormNetwork) {
		Fragility.lpnormNetwork = lpnormNetwork;
	}

}
