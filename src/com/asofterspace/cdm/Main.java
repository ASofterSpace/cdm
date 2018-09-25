package com.asofterspace.cdm;

import com.asofterspace.toolbox.cdm.CdmCtrl;
import com.asofterspace.toolbox.cdm.CdmMonitoringControlElement;
import com.asofterspace.toolbox.cdm.CdmNode;
import com.asofterspace.toolbox.cdm.exceptions.AttemptingEmfException;
import com.asofterspace.toolbox.cdm.exceptions.CdmLoadingException;
import com.asofterspace.toolbox.coders.ConversionException;
import com.asofterspace.toolbox.coders.UuidEncoderDecoder;
import com.asofterspace.toolbox.coders.UuidEncoderDecoder.UuidKind;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.Utils;
import com.asofterspace.toolbox.utils.ProgressIndicator;
import com.asofterspace.toolbox.utils.NoOpProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Main {

	public final static String PROGRAM_TITLE = "cdm commandline tool";
	public final static String VERSION_NUMBER = "0.0.0.8(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "13. September 2018 - 21. September 2018";

	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		// if we were called without arguments...
		if (args.length < 1) {
			// ... tell everyone that this is basically nonsense!
			showHelp(null, null);
			System.exit(1);
		}
		
		// get the first arg...
		String firstarg = args[0].toLowerCase();
		
		// ... get a map of all modifiers ...
		Map<String, String> arguments = new HashMap<String, String>();
		
		// we start at 1 (as 0 is already the firstarg), and go up to < args.length - 2,
		// as we want to be strictly less than args.length, but one less because that is
		// already the lastarg, if there is one
		for (int i = 1; i < args.length - 1; i++) {
			if (args[i].startsWith("-")) {
				arguments.put(args[i].toLowerCase(), args[i+1]);
				i++;
			} else {
				System.err.println("The argument '" + args[i] + "' was not understood - please check  cdm help " + firstarg);
				System.exit(4);
			}
		}
		
		// ... get the last argument
		String lastarg = null;
		if (args.length > 1) {
			// in the case of  cdm uuid -k ecore  we have no last arg, as we have an odd amount of arguments!
			if (args.length % 2 == 0) {
				lastarg = args[args.length - 1];
			}
		}

		// check all the arguments
		switch (firstarg) {
			case "create":
				createCdm(lastarg, arguments);
				break;
			case "convert":
				convertCdm(lastarg, arguments);
				break;
			case "validate":
				validate(lastarg, arguments);
				break;
			case "info":
				showInfo(lastarg, arguments);
				break;
			case "root":
				showRoot(lastarg, arguments);
				break;
			case "tree":
				showTree(lastarg, arguments);
				break;
			case "find":
				findInCdm(lastarg, arguments);
				break;
			case "uuid":
				uuid(lastarg, arguments);
				break;
			case "version":
				showVersion(lastarg, arguments);
				break;
			case "help":
				showHelp(lastarg, arguments);
				break;
			default:
				System.err.println("Sorry, I did not understand the argument '" + args[0] + "' - call  cdm help  to get a list of possible commands.");
				System.exit(2);
		}

		// all is shiny! all is good! exit code 0!
		System.exit(0);
	}

	private static void loadCdm(String pathArg, boolean loadFullModel) {

		Directory cdmDir = new Directory(pathArg);
		ProgressIndicator noProgress = new NoOpProgressIndicator();

		try {
			if (loadFullModel) {
				CdmCtrl.loadCdmDirectory(cdmDir, noProgress);
			} else {
				CdmCtrl.loadCdmDirectoryFaster(cdmDir, noProgress);
			}
		} catch (AttemptingEmfException | CdmLoadingException e) {
			System.err.println(e.getMessage());
			System.exit(3);
		}
	}

	private static void createCdm(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm create  but did not specify a CDM path at which the CDM should be created - please do.");
			System.exit(4);
		}

		// read out the given arguments
		String template = "-";
		String format = "-";
		String prefix = "-";
		String version = "-";
		
		if (arguments.containsKey("-t")) {
			template = arguments.get("-t");
		}
		
		if (arguments.containsKey("-f")) {
			format = arguments.get("-f");
		}
		
		if (arguments.containsKey("-p")) {
			prefix = arguments.get("-p");
		}
		
		if (arguments.containsKey("-v")) {
			version = arguments.get("-v");
		}
		
		// replace defaults
		if ("-".equals(template)) {
			template = CdmCtrl.getTemplates().get(0);
		}

		if ("-".equals(format)) {
			format = "xml";
		}

		if ("-".equals(version)) {
			version = CdmCtrl.getHighestKnownCdmVersion();
		}

		// if no prefix is specified, take the correct one automagically!
		if ("-".equals(prefix)) {
			prefix = CdmCtrl.getPrefixForVersion(version);
			if (prefix == null) {
				System.err.println("I do not know which prefix is associated with CDM version " + version + ".");
				System.err.println("Please explicitly specify a prefix, e.g. call  cdm create (...) prefix:" + version + " (...)");
				System.exit(7);
			}
		}

		// actually perform the work
		try {
			// TODO :: do not ignore the formate ;)
			CdmCtrl.createNewCdm(pathArg, version, prefix, template);
			
		} catch (AttemptingEmfException | CdmLoadingException e) {
			System.err.println(e.getMessage());
			System.exit(8);
		}
		
		System.out.println("The new CDM has been created!");
	}
	
	private static void findInCdm(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm find  but did not specify a CDM path to open - please do.");
			System.exit(4);
		}

		// TODO :: if this is just one file (e.g. toLowerCase() ends on .cdm) then actually just load that one file instead!
		loadCdm(pathArg, false);
		
		Set<CdmNode> nodesFound = new HashSet<>();

		// TODO :: add another switch that allows searching only for elements that have this AND that instead of this OR that
		// (right now, we search e.g. for elements with this name OR that tag, but maybe someone wants to search for elements
		// that have this name AND that tag!)

		// find by UUID
		if (arguments.containsKey("-u")) {
			String uuid = arguments.get("-u");
			try {
				uuid = UuidEncoderDecoder.ensureUUIDisEcore(uuid);
			} catch (ConversionException e) {
				System.err.println(e.getMessage());
				System.exit(11);
			}
			nodesFound.addAll(CdmCtrl.findByUuid(uuid));
		}
		
		// find by name
		if (arguments.containsKey("-n")) {
			nodesFound.addAll(CdmCtrl.findByName(arguments.get("-n")));
		}
		
		// find by type
		if (arguments.containsKey("-t")) {
			nodesFound.addAll(CdmCtrl.findByType(arguments.get("-t")));
		}
		
		// find by xml tag
		if (arguments.containsKey("-x")) {
			nodesFound.addAll(CdmCtrl.findByXmlTag(arguments.get("-x")));
		}
		
		if (nodesFound.size() == 0) {
			System.out.println("No entities have been found, sorry.");
			return;
		}
		
		if (nodesFound.size() == 1) {
			System.out.println("1 entity has been found:");
		} else {
			System.out.println(nodesFound.size() + " entities have been found:");
		}
		
		for (CdmNode node : nodesFound) {
			System.out.println("");
			node.print();
		}
	}

	private static void showInfo(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm info  but did not specify a CDM path of the CDM for which information should be shown - please do.");
			System.exit(4);
		}

		// TODO :: if this is just one file (e.g. toLowerCase() ends on .cdm) then actually just load that one file instead!
		loadCdm(pathArg, false);

		String cdmVersion = CdmCtrl.getCdmVersion();
		String cdmPrefix = CdmCtrl.getCdmVersionPrefix();
		System.out.println("CDM version: " + cdmVersion);
		System.out.println("CDM version prefix: " + cdmPrefix);
		System.out.println("CDM compatible with EGS-CC release: " + CdmCtrl.getCompatWithEGSCCstr(cdmVersion, cdmPrefix));
		System.out.println("CDM compatible with RTF Framework CDM editor version: " + CdmCtrl.getCompatWithMCDEstr(cdmVersion, cdmPrefix));
	}

	private static void showRoot(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm root  but did not specify a CDM path of the CDM for which the root should be accessed - please do.");
			System.exit(4);
		}

		// TODO :: if this is just one file (e.g. toLowerCase() ends on .cdm) then actually just load that one file instead!
		// (however, not sure if the tree can be constructed from just one file... ah well, it will work out somehow ^^)
		loadCdm(pathArg, true);
		
		Set<CdmMonitoringControlElement> roots = CdmCtrl.getAllMcmTreeRoots();
		
		if (roots.size() < 1) {
			System.err.println("The CDM that you specified does not seem to contain an MCM tree!");
			System.exit(12);
		}
		
		String setName = "-";
		String destinationPath = "-";
		
		if (arguments.containsKey("-n")) {
			setName = arguments.get("-n");
		}
		
		if (arguments.containsKey("-d")) {
			destinationPath = arguments.get("-d");
		}
		
		if ("-".equals(setName)) {
		
			if (!"-".equals(destinationPath)) {
				// if they want us to save even though nothing changed... oookay xD
				saveToDestinationPath(destinationPath, pathArg);
			}
		
		} else {
		
			for (CdmNode node : roots) {
				node.setName(setName);
			}
			
			saveToDestinationPath(destinationPath, pathArg);
		}
		
		if (roots.size() == 1) {
			roots.iterator().next().print();
			return;
		}
		
		System.out.println(roots.size() + " different root nodes have been found in this CDM!");
		System.out.println("Here they are:");
		
		for (CdmNode node : roots) {
			System.out.println("");
			node.print();
		}
	}
	
	private static void showTree(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm tree  but did not specify a CDM path of the CDM for which the tree should be accessed - please do.");
			System.exit(4);
		}

		// TODO :: if this is just one file (e.g. toLowerCase() ends on .cdm) then actually just load that one file instead!
		// (however, not sure if the tree can be constructed from just one file... ah well, it will work out somehow ^^)
		loadCdm(pathArg, true);
		
		Set<CdmMonitoringControlElement> roots = CdmCtrl.getAllMcmTreeRoots();
		
		if (roots.size() < 1) {
			System.err.println("The CDM that you specified does not seem to contain an MCM tree!");
			System.exit(12);
		}
		
		boolean isFirst = true;

		for (CdmMonitoringControlElement root : roots) {

			if (!isFirst) {
				System.out.println("");
			}

			recursivelyShowTree(root, "");

			isFirst = false;
		}
	}
	
	private static void recursivelyShowTree(CdmMonitoringControlElement mce, String prefix) {
		
		System.out.println(prefix + mce.getName());
		
		for (CdmMonitoringControlElement child : mce.getSubElements()) {
			recursivelyShowTree(child, "  " + prefix);
		}
	}

	/**
	 * Open the CDM in originPath and convert it to the specified version (possibly including a prefix separated with a colon) and format.
	 * If destination path is null, overwrite the CDM in place.
	 * If destination path is given, store the conversion result there.
	 */
	private static void convertCdm(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm create  but did not specify a CDM path at which the CDM should be created - please do.");
			System.exit(4);
		}

		// read out the given arguments
		String template = "-";
		String format = "-";
		String toPrefix = "-";
		String toVersion = "-";
		String destinationPath = "-";
		
		if (arguments.containsKey("-t")) {
			template = arguments.get("-t");
		}
		
		if (arguments.containsKey("-f")) {
			format = arguments.get("-f");
		}
		
		if (arguments.containsKey("-p")) {
			toPrefix = arguments.get("-p");
		}
		
		if (arguments.containsKey("-v")) {
			toVersion = arguments.get("-v");
		}
		
		if (arguments.containsKey("-d")) {
			destinationPath = arguments.get("-d");
		}
		
		// replace defaults
		String conversionTargetStr = "";

		// first check if the target format selected actually can be done (and if not, complain immediately!)
		if (!"-".equals(format)) {
			format = format.toUpperCase();

			switch (format) {
				case "XML":
					break;
				default:
					System.err.println("Sorry, the target format " + format + " is not yet supported!");
					System.exit(5);
			}

			conversionTargetStr = format + " format";
		}

		// now attempt to load the CDM from the origin path
		loadCdm(pathArg, false);

		// do the conversion to a different version (and possibly prefix)
		if ("-".equals(toVersion)) {
			toVersion = null;
		}

		if ("-".equals(toPrefix)) {
			toPrefix = null;
		}

		// if no prefix is specified, take the correct one automagically!
		// (however, if the target version is null - so kept the same - then the prefix can also be null - also be kept the same)
		if ((toPrefix == null) && (toVersion != null)) {
			toPrefix = CdmCtrl.getPrefixForVersion(toVersion);
			if (toPrefix == null) {
				System.err.println("I do not know which prefix is associated with CDM version " + toVersion + ".");
				System.err.println("Please explicitly specify a prefix (even '-' would be enough to keep the current one), e.g. call  cdm convert -:" + toVersion + " (...)");
				System.exit(7);
			}
		}

		if ((toPrefix != null) || (toVersion != null)) {
			CdmCtrl.convertTo(toVersion, toPrefix);

			if ("".equals(conversionTargetStr)) {
				conversionTargetStr = "CDM version " + CdmCtrl.getCdmVersion();
			} else {
				conversionTargetStr += " and CDM version " + CdmCtrl.getCdmVersion();
			}
		}
		
		saveToDestinationPath(destinationPath, pathArg);

		if ("".equals(conversionTargetStr)) {
			System.out.println("No conversion done - as I was told to keep both version and format the same! :)");
		} else {
			System.out.println("Conversion to " + conversionTargetStr + " done!");
		}
	}
	
	private static void saveToDestinationPath(String destinationPath, String pathArg) {
	
		// now actually save the result
		if ((destinationPath == null) || ("-".equals(destinationPath))) {

			// overwrite the original with the new result
			// TODO :: do not ignore the target format once we have more than XML available!
			CdmCtrl.save();

		} else {

			Directory destDir = new Directory(destinationPath);

			// complain if the directory is not empty, unless the source and destination are the same
			if (!pathArg.equals(destinationPath)) {
				Boolean isEmpty = destDir.isEmpty();
				if ((isEmpty == null) || !isEmpty) {
					System.err.println("The specified destination directory is not empty - please save the conversion result into an empty directory!");
					System.exit(9);
				}
			}

			// save the result to the new destination path
			// TODO :: do not ignore the target format once we have more than XML available!
			CdmCtrl.saveTo(destDir);
		}
	}

	private static void validate(String pathArg, Map<String, String> arguments) {

		if (pathArg == null) {
			System.err.println("You called  cdm validate  but did not specify a CDM path of the CDM that should be validated - please do.");
			System.exit(4);
		}
		
		// TODO :: if this is just one file (e.g. toLowerCase() ends on .cdm) then actually just load that one file instead!
		loadCdm(pathArg, true);

		List<String> problems = new ArrayList<>();

		int problemAmount = CdmCtrl.checkValidity(problems);

		if (problemAmount > 0) {
			System.err.println("The CDM does not seem to be valid.");
			if (problemAmount == 1) {
				System.err.println("There is one problem:");
			} else {
				System.err.println("There are " + problemAmount + " problems:");
			}
			System.err.println("");
			for (String problem : problems) {
				System.err.println(problem);
			}
			System.exit(6);
		}

		System.out.println("The CDM looks valid to me!");
	}

	private static void uuid(String mainArg, Map<String, String> arguments) {

		String kind = "-";
		
		if (arguments.containsKey("-k")) {
			kind = arguments.get("-k");
		}
		
		// if no argument is given...
		if (mainArg == null) {
			// ... generate a UUID
			
			switch (kind.toLowerCase()) {
				case "java":
				case "-":
					System.out.println(UuidEncoderDecoder.generateJavaUUID());
					return;
				case "ecore":
				case "emf":
					System.out.println(UuidEncoderDecoder.generateEcoreUUID());
					return;
				default:
					System.err.println("A UUID in the format '" + kind +
						"' cannot be created, as the format is not known... sorry!");
					System.exit(10);
			}
		}
		
		// on the other hand, if an argument is given... convert!
	
		UuidKind currentKind = UuidEncoderDecoder.detectUUIDkind(mainArg);
	
		switch (kind.toLowerCase()) {
			case "java":
			case "-":
				try {
					System.out.println(UuidEncoderDecoder.ensureUUIDisJava(mainArg));
				} catch (ConversionException e) {
					System.err.println(e.getMessage());
					System.exit(11);
				}
				break;
			case "ecore":
			case "emf":
				try {
					System.out.println(UuidEncoderDecoder.ensureUUIDisEcore(mainArg));
				} catch (ConversionException e) {
					System.err.println(e.getMessage());
					System.exit(11);
				}
				break;
			default:
				System.err.println("Your UUID cannot be converted into the format '" + kind +
					"', as the format is not known... sorry!");
				System.exit(10);
		}
	}

	private static void showHelp(String mainArg, Map<String, String> arguments) {

		final String HELP_CREATE = "create [-t <template>] [-f <format>] [-p <versionPrefix>] [-v <version>] <cdmPath> .. creates a new CDM";
		final String HELP_CONVERT = "convert [-f <format>] [-p <versionPrefix>] [-v <version>] [-d <destinationCdmPath>] <cdmPath> .. converts the CDM";
		final String HELP_VALIDATE = "validate <cdmPath> .. validates the CDM";
		final String HELP_INFO = "info <cdmPath> .. shows information about the CDM";
		final String HELP_ROOT = "root [-n <name>] [-d <destinationCdmPath>] <cdmPath> .. shows the root of the MCM tree";
		// TODO :: optionally specify to show only MCEs (like now), or also parameters, also activities, also events, ...
		final String HELP_TREE = "tree <cdmPath> .. shows the MCM tree";
		final String HELP_FIND = "find [-u <uuid>] [-n <name>] [-t <type>] [-x <xmltag>] <cdmPath> .. finds an element in the CDM";
		final String HELP_UUID = "uuid [-k <kind>] [<uuid>] .. generates or converts a UUID";
		final String HELP_VERSION = "version .. shows the version of the " + PROGRAM_TITLE;
		final String HELP_HELP = "help [<command>] .. shows the help, optionally detailed help for a specific command";

		if (mainArg == null) {
			System.out.println("Welcome to the " + Utils.getFullProgramIdentifier() + "! :)");
			System.out.println("");
			System.out.println("Available commands:");
			System.out.println("");
			System.out.println("* " + HELP_CREATE);
			System.out.println("* " + HELP_CONVERT);
			System.out.println("* " + HELP_VALIDATE);
			System.out.println("* " + HELP_INFO);
			System.out.println("* " + HELP_ROOT);
			System.out.println("* " + HELP_TREE);
			System.out.println("* " + HELP_FIND);
			System.out.println("* " + HELP_UUID);
			System.out.println("* " + HELP_VERSION);
			System.out.println("* " + HELP_HELP);
			// TODO :: add list commands, e.g. list parameters, list activities, list scripts, ...
			// TODO :: add command to automagically fix problems, e.g. when there is a link to a UUID that is in a different file than the link indicates, but we can fix it because the UUID is - wait for it - unique :D
			// TODO :: add command to read a particular script, that is, get the content of that script and print it to system out
		} else {

			switch (mainArg.toLowerCase()) {

				case "create":
					System.out.println(HELP_CREATE);
					System.out.println("");
					System.out.println("  Available templates for -t are:");
					List<String> templates = CdmCtrl.getTemplates();
					List<String> templatesShort = CdmCtrl.getTemplatesShort();
					for (int i = 0; i < templates.size(); i++) {
						System.out.println("    " + templatesShort.get(i) + " .. " + templates.get(i));
					}
					System.out.println("    - .. default template: " + templatesShort.get(0));
					System.out.println("");
					System.out.println("  Supported target formats for -f are:");
					System.out.println("    xml .. human-readable CDM format based on XML");
					System.out.println("    - .. default: xml");
					// TODO :: also add EMF, JSON, CSV, ZIP, etc.
					System.out.println("");
					System.out.println("  The version prefix for -p is automatically selected based on the version; only select it manually if you really have to.");
					System.out.println("");
					System.out.println("  Supported versions for -v are:");
					for (String ver : CdmCtrl.getKnownCdmVersions()) {
						System.out.println("    " + ver);
					}
					System.out.println("    - .. default: highest available version (" + CdmCtrl.getHighestKnownCdmVersion() + ")");
					break;

				case "info":
					System.out.println(HELP_INFO);
					break;

				case "root":
					System.out.println(HELP_ROOT);
					System.out.println("  If a name is selected with -n, the root element will be renamed to this name.");
					System.out.println("  If no destination CDM path is selected using -d, then the CDM that is opened will be overwritten in-place in case the name is changed.");
					break;

				case "tree":
					System.out.println(HELP_TREE);
					break;

				case "convert":
					System.out.println(HELP_CONVERT);
					System.out.println("");
					System.out.println("  Supported target formats for -f are:");
					System.out.println("    xml .. human-readable CDM format based on XML");
					System.out.println("    - .. default: keep the current format");
					// TODO :: also add EMF, JSON, CSV, ZIP, etc.
					System.out.println("");
					System.out.println("  The version prefix for -p is automatically selected based on the version; only select it manually if you really have to.");
					System.out.println("");
					System.out.println("  Supported target versions for -v are:");
					for (String ver : CdmCtrl.getKnownCdmVersions()) {
						System.out.println("    " + ver);
					}
					System.out.println("    - .. default: keep the current version");
					System.out.println("");
					System.out.println("  If no destination CDM path is selected using -d, then the CDM that is opened will be overwritten in-place.");
					break;

				case "validate":
					System.out.println(HELP_VALIDATE);
					break;

				case "find":
					System.out.println(HELP_FIND);
					System.out.println("");
					System.out.println("  -u UUID .. if specified, find an element by its UUID");
					System.out.println("  -n name .. if specified, find an element by its name");
					System.out.println("  -t type .. if specified, find an element by its xsi type");
					System.out.println("  -x xmltag .. if specified, find an element by its xml tag");
					break;

				case "uuid":
					System.out.println(HELP_UUID);
					System.out.println("");
					System.out.println("  Available UUID kinds for -k are:");
					System.out.println("    java .. default: generate a Java UUID");
					System.out.println("    ecore .. generate an EMF / Ecore UUID as used inside CDM files");
					System.out.println("    emf .. same as ecore ");
					System.out.println("");
					System.out.println("If no <uuid> argument is given, a new random UUID is generated.");
					System.out.println("If a <uuid> arguments is given, the given UUID is converted to a UUID of the specified kind.");
					break;

				case "version":
					System.out.println(HELP_VERSION);
					break;

				case "help":
					System.out.println(HELP_HELP);
					break;

				default:
					System.err.println("Whoopsie! I do not actually know the command '" + mainArg +
						"', so I cannot offer any help with it...");
			}
		}
	}

	private static void showVersion(String mainArg, Map<String, String> arguments) {
		System.out.println(Utils.getFullProgramIdentifierWithDate());
	}

}
