package gov.lanl.micot.fragility.test;

import gov.lanl.micot.fragility.Fragility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit test for the Fragility application.
 */
public class FragilityTest extends TestCase
{
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final String[] ids={"pdp1234","pdp5678"};
	private static Set<String> idSet = new HashSet<>();
	static{idSet.add(ids[0]);idSet.add(ids[1]);}
	private static final String assetClass="PowerDistributionPole";
	private static final String hazard="Windspeed";
	private static final double response=0.04576955673440508;
	private static final double exposure=130.0;
 
    public void testFragility()
    {
    	Path currentRelativePath = Paths.get("");
    	String s = currentRelativePath.toAbsolutePath().toString();
    	s=s.replace("\\", "/");
    	String inputFile = s+"/src/test/resources/GFM_test_example.json";
    	String outFile1 = s+"/src/test/resources/GFM_test_out1.json";
    	String outFile2 = s+"/src/test/resources/GFM_test_out2.json";
    	String[] args1 = {inputFile, outFile1};
    	String[] args2 = {inputFile, outFile2, "--exposure"};
    	
    	Fragility.main(args1);
    	Fragility.main(args2);
    	assertResults(outFile1, outFile2);
    }
    
    private void assertResults(String outFile1, String outFile2){
    	try {
			InputStream instream = new FileInputStream(outFile1);
			ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(instream);
			for(int i=0;i<arrayNode.size();i++){
				ObjectNode n = (ObjectNode) arrayNode.get(i);
				String id = n.get("assetID").asText();
				String aClass = n.get("assetClass").asText();
				String haz = n.get("hazardQuantityType").asText();
				double r = n.get("value").asDouble();
				assertTrue(idSet.contains(id));
				assertTrue(aClass.equals(assetClass));
				assertTrue(haz.equals(hazard));
				assertTrue(r==response);
			}
			
			instream = new FileInputStream(outFile2);
			arrayNode = (ArrayNode) objectMapper.readTree(instream);
			for(int i=0;i<arrayNode.size();i++){
				ObjectNode n = (ObjectNode) arrayNode.get(i);
				String id = n.get("assetID").asText();
				String aClass = n.get("assetClass").asText();
				String haz = n.get("hazardQuantityType").asText();
				double e = n.get("value").asDouble();
				assertTrue(idSet.contains(id));
				assertTrue(aClass.equals(assetClass));
				assertTrue(haz.equals(hazard));
				assertTrue(e==exposure);
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
