/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.tools.simulator.tn3;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.mobicents.protocols.ss7.tools.simulator.tests.cap.CamelConfigurationData;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;


/**
 * @author
 */
public class MainStandaloneApplication {


    private static final String APP_NAME = "SS7 Simulator";
    private String command = null;
    private Map<String, Object> inputParametersMap = new HashMap<String, Object>();
    private Keyboard keyboard = new Keyboard();
	private Simulator simulator = null;

	private static final String CLASS_ATTRIBUTE = "type";
	private static final String PERSIST_FILE_NAME = "camel_configuration.xml";
	private static final String CAMEL_CONFIGURATION_DATA = "CamelConfigurationData";

	private static final String TEST_LOAD_XML_PERSIST_DIR_KEY = "test.load.xml.persist.dir";
	private static final String USER_DIR_KEY = "user.dir";

	private String persistDir = null;
	private final TextBuilder persistFile = TextBuilder.newInstance();
    private static final XMLBinding binding = new XMLBinding();

    public static String SIMULATOR_HOME_VAR = "SIMULATOR_HOME";
    private CamelConfigurationData camelConfigurationData = new CamelConfigurationData();


    public static void main(String[] args) throws Throwable {
        MainStandaloneApplication main = new MainStandaloneApplication();
        main.processCommandLine(args);
        main.boot();
    }

    private void processCommandLine(String[] args) {
        int c;
        String arg;
        LongOpt[] longopts = new LongOpt[4];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("name", LongOpt.REQUIRED_ARGUMENT, null, 'n');
        longopts[2] = new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f');
        longopts[2] = new LongOpt("acrwaittime", LongOpt.REQUIRED_ARGUMENT, null, 'w');

        Getopt g = new Getopt(APP_NAME, args, "-:n:f:w:h", longopts);
        g.setOpterr(false); // We'll do our own error handling

        this.inputParametersMap.put("appName", "main");
        this.inputParametersMap.put("callDuration", new Integer(300));
        this.inputParametersMap.put("configureStackFromFile", new Boolean(false));
        this.inputParametersMap.put("acrWaitTime", new Long(0));

        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'n':
                    arg = g.getOptarg();
                    this.inputParametersMap.put("appName", arg);
                    //this.appName = arg;
                    break;
                case 'f':
                    arg = g.getOptarg();
                    this.inputParametersMap.put("configureStackFromFile", arg.equals("Y") ? new Boolean(true) : new Boolean(false));
                    //this.configureStackFromFile = arg.equals("Y") ? true : false;
                    break;
                case 'w':
                    arg = g.getOptarg();
                    this.inputParametersMap.put("acrWaitTime",new Long(arg));
                    break;
                case 'h':
                    this.genericHelp();
                    break;
                case ':':
                    System.out.println("You need an argument for option " + (char) g.getOptopt());
                    System.exit(0);
                    break;
                case '?':
                    System.out.println("The option '" + (char) g.getOptopt() + "' is not valid");
                    System.exit(0);
                    break;
                case 1:
                    String optArg = g.getOptarg();
                    if (optArg.equals("console")) {
                        this.command = "console";
                    } else if (optArg.equals("help")) {
                        if (this.command == null) {
                            this.genericHelp();
                        } else if (this.command.equals("console")) {
                            this.consoleHelp();
                        } else {
                            System.out.println("Invalid command " + optArg);
                            this.genericHelp();
                        }
                    } else {
                        System.out.println("Invalid command " + optArg);
                        this.genericHelp();
                    }
                    break;
                default:
                    this.genericHelp();
                    break;
            }
        }

    }

    private void genericHelp() {
        System.out.println("usage: " + APP_NAME + "<command> [options]");
        System.out.println();
        System.out.println("command:");
        System.out.println("    console       Start the SS7 simulator console mode");
        System.out.println();
        System.out.println("see 'run <command> help' for more information on a specific command:");
        System.out.println();
        System.exit(0);
    }

    private void consoleHelp() {
        System.out.println("console : Starts the simulator");
        System.out.println();
        System.out.println("usage: " + APP_NAME + " console [options]");
        System.out.println();
        System.out.println("options:");
        System.out.println("    -n, --name=<simulator name>   Simulator name. If not passed default is main");
        System.out.println("    -f, --file=<Y / N> Read from file Stack Sctp/M3ua. If not passed default is false");
        System.out.println("    -w, --acrwaittime=<wait time betweeen each ACR> Wait Time between ACRs in milliseconds");
        System.out.println();
        System.exit(0);
    }

    protected void boot() throws Throwable {
        if (this.command == null) {
            System.out.println("No command passed");
            this.genericHelp();
        } else if (this.command.equals("console")) {
            simulator = new Simulator();
            String sim_home = System.getenv(MainStandaloneApplication.SIMULATOR_HOME_VAR);
            if (sim_home != null)
                sim_home += File.separator + "data";
            loadCamelConfigurationDataFromXMLFile (sim_home);
        	while (true){
        		System.out.print("\033[H\033[2J");
        	    System.out.flush();
        	    menu();
            }
        }
    }

    public void menu () throws InterruptedException{
    	System.out.println(" ______________________________________ ");
    	System.out.println("|                                      |");
    	System.out.println("|             OPTIONS MENU             |");
    	System.out.println("|                                      |");
    	System.out.println("| 1 - Startup L1/L2/L3/L4 stack        |");
	    System.out.println("| 2 - Make Call                        |");
	    System.out.println("| 3 - Stack Status                     |");
	    System.out.println("| 4 - Salir                            |");
	    System.out.println("|______________________________________|");

	    int option  = keyboard.getOption("select the option : ");

	    switch (option){
	        case 1:
	        	if (simulator.getHost() == null){
	        	    System.out.println("Starting up Tester, please wait to initialize ....");
	        	    //boolean configureStackFromFile = false;
	        	    simulator.startLocal(this.inputParametersMap);
	        	    boolean stackUp = false;
	        	    boolean sctpUp = false;
	        	    boolean sccpUp = false;
	        	    boolean m3uaUp = false;
	        	    long start = System.currentTimeMillis();
	        	    long timeWaiting = 0;
	        	    while (!stackUp){
	        	    	Thread.sleep(200);
	        	    	long end = System.currentTimeMillis();
	        	    	timeWaiting = end - start;
	        	    	m3uaUp = this.simulator.getM3uaState();
	        	    	sctpUp = this.simulator.getSctpState();
	        	    	sccpUp = this.simulator.getSccpState();
	        	        //System.out.println("m3uaUp : " + m3uaUp + " sctpUp : " + sctpUp + " sccpUp : " + sccpUp);
	        	    	if (sctpUp && m3uaUp && sccpUp){
	        	    	    stackUp = true;
	        	    	    break;
	        	        }
	        	    	if (timeWaiting >= 60000)
	        	    		break;
	        	    }
	        	    if (stackUp)
	        	    	keyboard.makePause("Initialized ... to ensure complete stack is UP, use option \"3\", Press ENTER to return :");
	        	    else
	        	    	keyboard.makePause("Tester timed out ... to check Stack, use option \"3\", Press ENTER to return :");
	        	}else{
	        		keyboard.makePause("Tester already initialized, try \"3\" to check stack status, Press ENTER to return :");
	        	}
		        break;
	        case 2:
	        	if (simulator.getHost() != null){
	        	    int callDurationInput  = keyboard.getInt("Enter call duration (seconds) :");
	        	    this.inputParametersMap.put("callDuration", callDurationInput * 10);
	        	    //this.callDuration  = callDurationInput * 10;
	        	    simulator.openTest(this.inputParametersMap, this.camelConfigurationData);
	        	    boolean callCompleted = false;
	        	    while (!callCompleted){
	        	    	Thread.sleep(200);
	        	    	if (simulator.getActualProgress() >= 100 || simulator.getReleaseCallReceived() || simulator.getestablishTemporaryConnectionReceived())
	        	            callCompleted = true;
	        	    }
	        	    keyboard.makePause("Press ENTER to return :");
	        	}else{
	        		keyboard.makePause("Tester not started, try option \"1\", Press ENTER to return :");
	        	}
		        break;
	        case 3:
	        	if (simulator.getHost() != null){
	        	    simulator.refreshState();
	        	    keyboard.makePause("Press ENTER to return :");
	            }else{
	            	keyboard.makePause("Tester not started, try option \"1\" , Press ENTER to return :");
	            }
	        	break;
	        case 4:
	        	System.out.println("Releasing Resources ....");
	        	if (simulator.getHost() != null){
	        	    simulator.stopHost();
	        	}
	        	System.out.println("Resources released ....");
		        System.exit(0);
		        break;
		    default :
		    	System.out.println("\"" + String.valueOf(option) + "\"" + " is not a valid option, try again");
		        break;
        }
    }

    private void loadCamelConfigurationDataFromXMLFile(String persistDir) {

    	String appName = (String) this.inputParametersMap.get("appName");
    	this.persistDir = persistDir;

		binding.setClassAttribute(CLASS_ATTRIBUTE);
        this.persistFile.clear();
        if (this.persistDir != null)
            this.persistFile.append(persistDir).append(File.separator).append(appName).append("_")
                    .append(PERSIST_FILE_NAME);
        else
            this.persistFile.append(System.getProperty(TEST_LOAD_XML_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
                    .append(File.separator).append(appName).append("_").append(PERSIST_FILE_NAME);
        File fn = new File(persistFile.toString());

        XMLObjectReader reader = null;
        try {
            if (!fn.exists()) {
                System.out.println("Error while reading the XML Camel Configuration file : file not found: ");
                System.exit(1);
            }

            reader = XMLObjectReader.newInstance(new FileInputStream(fn));
            reader.setBinding(binding);
            this.camelConfigurationData = reader.read(CAMEL_CONFIGURATION_DATA, CamelConfigurationData.class);
            reader.close();
        } catch (Exception ex) {
              ex.printStackTrace();
        }
    }

}
