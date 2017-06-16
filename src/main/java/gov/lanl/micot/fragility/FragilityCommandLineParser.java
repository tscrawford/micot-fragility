package gov.lanl.micot.fragility;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class FragilityCommandLineParser {
	
	private static Options options = defineOptions();
	//private String inputPath = null;
	//private String outputPath = null;
	private String schemaURI = null;
	private boolean validateInput = false;
	private boolean exposureOnly = false;

	private String rdtInputPath = null;
	private String polesInputPath = null;
	private String windFieldInputPath = null;
	private String outputFilePath = null;
	private boolean hasRdt = false;
	private boolean hasPoles = false;
	private boolean hasWindField = false;
	private boolean hasOutputFileName = false;
	
	public FragilityCommandLineParser(String[] args) {
		defineOptions();
		parse(args);
	}

	private static Options defineOptions(){
		options = new Options();

		options.addOption(new Option("h","help",false,
				"print fragility help"));
		options.addOption(new Option("s","schema",true,
				"validate schema using JSON schema at given URI"));
		options.getOption("s").setArgName("JSON_SCHEMA_URI");

		options.addOption(new Option("e","exposure",false,
				"evaluate exposure only"));
		options.addOption(new Option("r","rdt",true,
				"RDT JSON input"));
		options.addOption(new Option("p","poles",true,
				"pole JSON input"));
		options.addOption(new Option("wf","windField",true,
				"Wind field Esri Ascii input"));
		options.addOption(new Option("o","output",true,
				"output file name"));
		return options;
	}
	
	public static void printUsage(){
		HelpFormatter formatter = new HelpFormatter();
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		String header = "fragility <INPUT_FILE_PATH> <OUTPUT_FILE_PATH> [OPTIONS]\n options:\n";
		String footer = 
				"\nExamples:\n\nFragility.jar  -r <RDT_path.json> -o <output.json>\n\n"+
				"Fragility.jar -p <Poles_input.json> -o <output.json> --schema http://org.lanl.fragility/schemas/fragilitySchema.json \n\n"+
				"Fragility.jar -r <RDT_path.json> -wf <windHazard_path> -o <RDToutput.json> \n\n";
		formatter.printHelp(writer, 100, header, "\n", options, 4, 4, footer);
		System.out.println(swriter.toString());
	}

	private void parse(String[] args){
		CommandLine commandLine = null;
		// Parse the command line.
		CommandLineParser parser = new DefaultParser();
		try {
			// Must have at least two positional parameters for input and output file respectively.
			if( args.length == 0){
				System.out.println("Error: Arguments must be prescripted with option parameters");
				printUsage();
				System.exit(1);
			}
			
			// Parse the options and return the command line object.
		    commandLine = parser.parse( options, args );
		    
		    // Check to see if only help is requested.
		    if(commandLine.hasOption("help")){
		    	System.out.println("Fragility help:");
		    	printUsage();
		    	System.exit(0);
		    }
		    
		    schemaURI = null;
		    exposureOnly = false;

		    if(commandLine.hasOption("schema")){
		    	validateInput = true;
		    	schemaURI = commandLine.getOptionValue("schema");
		    }
		    if(commandLine.hasOption("exposure")){
		    	exposureOnly=true;
		    }

		    // lpnorm --
		    if(commandLine.hasOption("rdt")){
				hasRdt = true;
				rdtInputPath = commandLine.getOptionValue("rdt");
			}

			if(commandLine.hasOption("output")){
				hasOutputFileName = true;
				outputFilePath = commandLine.getOptionValue("output");
			}

			if(commandLine.hasOption("poles")){
				hasPoles = true;
				polesInputPath = commandLine.getOptionValue("poles");
			}

			if(commandLine.hasOption("windField")){
				hasWindField = true;
				windFieldInputPath = commandLine.getOptionValue("windField");
			}

		}
		catch( ParseException exp ) {
		    printUsage();
		    System.exit(1);
		}
	}

	public static Options getOptions() {
		return options;
	}

	public String getSchemaURI() {
		return schemaURI;
	}

	public boolean isValidateInput() {
		return validateInput;
	}
	
	public boolean isExposureOnly() {
		return exposureOnly;
	}

	public String getRdtInputPath(){
		return this.rdtInputPath;
	}

	public String getPolesInputPath(){
		return this.polesInputPath;
	}

	public String getWindFieldInputPath(){
		return this.windFieldInputPath;
	}

	public String getOutputPath(){
		return this.outputFilePath;
	}
	
	public boolean hasRdt() {
		return hasRdt;
	}

	public boolean hasPoles(){
		return hasPoles;
	}

	public boolean hasWindField(){
		return hasWindField;
	}

	public boolean hasOutputFile(){
		return hasOutputFileName;
	}

}
