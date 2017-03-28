package gov.lanl.micot.fragility.test;

import gov.lanl.micot.fragility.FragilityCommandLineParser;
import junit.framework.TestCase;

public class FragilityCommandLineParserTest extends TestCase {

	public void testParser(){

		FragilityCommandLineParser.printUsage();

		String[] args1 = {"input.json", "output.json", "--schema", "http://org.lanl.fragility/schemas/fragilitySchema.json"};
		String[] args2 = {"input.json", "output.json", "--exposure"};
		
		System.out.println("Command line:");
		for(String arg:args1) {
			System.out.print(arg+" ");
		}
		System.out.print("\n");
		FragilityCommandLineParser parser = new FragilityCommandLineParser(args1);
		String inputPath = parser.getInputPath();
		assertTrue(inputPath.equals("input.json"));
		System.out.println("Input path = "+inputPath);
		
		String outputPath = parser.getOutputPath();
		assertTrue(outputPath.equals("output.json"));
		System.out.println("Output path = "+outputPath);
		
		String schemaURI = parser.getSchemaURI();
		assertTrue(schemaURI.equals("http://org.lanl.fragility/schemas/fragilitySchema.json"));
		System.out.println("Schema URI = "+ schemaURI);
		
		boolean exposureOnly = parser.isExposureOnly();
		assertTrue(!exposureOnly);
		System.out.println("Not exposure-only");


		System.out.println();
		System.out.println("Command line:");
		for(String arg:args2) {
			System.out.print(arg+" ");
		}
		System.out.print("\n");		
		parser = new FragilityCommandLineParser(args2);
		inputPath = parser.getInputPath();
		assertTrue(inputPath.equals("input.json"));
		System.out.println("Input path = "+inputPath);
		
		outputPath = parser.getOutputPath();
		assertTrue(outputPath.equals("output.json"));
		System.out.println("Output path = "+outputPath);

		schemaURI = parser.getSchemaURI();
		assertTrue(schemaURI==null);
		System.out.println("Schema URI absent");
		
		exposureOnly = parser.isExposureOnly();
		assertTrue(exposureOnly);
		System.out.println("Exposure-only");
		
		
	}

}
