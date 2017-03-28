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
	private String inputPath = null;
	private String outputPath = null;
	private String schemaURI = null;
	private boolean validateInput = false;
	private boolean exposureOnly = false;
	
	
	public FragilityCommandLineParser(String[] args) {
		defineOptions();
		parse(args);
	}

	private static Options defineOptions(){
		options = new Options();
		options.addOption(new Option("h","help",false,"print fragility help"));
		options.addOption(new Option("s","schema",true,"validate schema using JSON schema at given URI"));
		options.getOption("s").setArgName("JSON_SCHEMA_URI");
		options.addOption(new Option("e","exposure",false,"evaluate exposure only"));
		return options;
	}
	
	public static void printUsage(){
		HelpFormatter formatter = new HelpFormatter();
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		String header = "fragility <INPUT_FILE_PATH> <OUTPUT_FILE_PATH> [OPTIONS]\n options:\n";
		String footer = 
				"\nExamples:\nfragility input.json output.json --exposure\n"+
				"fragility input.json output.json --schema http://org.lanl.fragility/schemas/fragilitySchema.json\n";
		formatter.printHelp(writer, 100, header, "\n", options, 4, 4, footer);
		System.out.println(swriter.toString());
	}
	
	private void parse(String[] args){
		CommandLine commandLine = null;
		// Parse the command line.
		CommandLineParser parser = new DefaultParser();
		try {
			// Must have at least two positional parameters for input and output file respectively.
			if(args.length<2){
				System.out.println("Error: Arguments must include two positional parameters for "
						+ "input and output file paths respectively.");
				printUsage();
				System.exit(1);
			}
			inputPath = args[0];
			outputPath = args[1];
			
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
		}
		catch( ParseException exp ) {
		    printUsage();
		    System.exit(1);
		}
	}

	public static Options getOptions() {
		return options;
	}

	public String getInputPath() {
		return inputPath;
	}

	public String getOutputPath() {
		return outputPath;
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

}
