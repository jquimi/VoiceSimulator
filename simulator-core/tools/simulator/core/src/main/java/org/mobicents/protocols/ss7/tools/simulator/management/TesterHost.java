/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
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

package org.mobicents.protocols.ss7.tools.simulator.management;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mobicents.protocols.ss7.mtp.Mtp3UserPart;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.tools.simulator.Stoppable;
import org.mobicents.protocols.ss7.tools.simulator.common.ConfigurationData;
import org.mobicents.protocols.ss7.tools.simulator.level1.M3uaConfigurationData_OldFormat;
import org.mobicents.protocols.ss7.tools.simulator.level1.M3uaMan;
import org.mobicents.protocols.ss7.tools.simulator.level2.NatureOfAddressType;
import org.mobicents.protocols.ss7.tools.simulator.level2.NumberingPlanSccpType;
import org.mobicents.protocols.ss7.tools.simulator.level2.SccpConfigurationData_OldFormat;
import org.mobicents.protocols.ss7.tools.simulator.level2.SccpMan;
import org.mobicents.protocols.ss7.tools.simulator.level3.CapMan;
import org.mobicents.protocols.ss7.tools.simulator.tests.cap.TestCapSsfMan;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class TesterHost extends NotificationBroadcasterSupport implements TesterHostMBean, Stoppable {
    private static final Logger logger = Logger.getLogger(TesterHost.class);

    private static final String TESTER_HOST_PERSIST_DIR_KEY = "testerhost.persist.dir";
    private static final String USER_DIR_KEY = "user.dir";
    public static String SOURCE_NAME = "HOST";
    public static String SS7_EVENT = "SS7Event";

    private static final String CLASS_ATTRIBUTE = "type";
    private static final String TAB_INDENT = "\t";
    private static final String PERSIST_FILE_NAME_OLD = "simulator.xml";
    private static final String PERSIST_FILE_NAME = "simulator2.xml";
    private static final String CONFIGURATION_DATA = "configurationData";

    public static String SIMULATOR_HOME_VAR = "SIMULATOR_HOME";

    private final String appName;
    private String persistDir = null;
    private final TextBuilder persistFile = TextBuilder.newInstance();
    private static final XMLBinding binding = new XMLBinding();

    // SETTINGS
    private boolean isStarted = false;
    private boolean needQuit = false;
    private boolean needStore = false;
    private ConfigurationData configurationData = new ConfigurationData();
    private long sequenceNumber = 0;
    private boolean configureStackFromFile = false;

    // Layers
    private Stoppable instance_L1_B = null;
    private Stoppable instance_L2_B = null;
    private Stoppable instance_L3_B = null;
    private Stoppable instance_TestTask_B = null;

    // levels
    M3uaMan m3ua;
    SccpMan sccp;
    CapMan cap;
    TestCapSsfMan testCapSsfMan;

    // testers

    public TesterHost(String appName, String persistDir, boolean configureStackFromFile) {
        this.appName = appName;
        this.persistDir = persistDir;
        this.configureStackFromFile = configureStackFromFile;

        this.m3ua = new M3uaMan(appName);
        this.m3ua.setTesterHost(this);

        this.sccp = new SccpMan(appName);
        this.sccp.setTesterHost(this);

        this.cap = new CapMan(appName);
        this.cap.setTesterHost(this);

        this.testCapSsfMan = new TestCapSsfMan(appName);
        this.testCapSsfMan.setTesterHost(this);

        this.setupLog4j(appName);

        binding.setClassAttribute(CLASS_ATTRIBUTE);

        this.persistFile.clear();
        TextBuilder persistFileOld = new TextBuilder();

        if (persistDir != null) {
            persistFileOld.append(persistDir).append(File.separator).append(this.appName).append("_")
                    .append(PERSIST_FILE_NAME_OLD);
            this.persistFile.append(persistDir).append(File.separator).append(this.appName).append("_")
                    .append(PERSIST_FILE_NAME);
        } else {
            persistFileOld.append(System.getProperty(TESTER_HOST_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
                    .append(File.separator).append(this.appName).append("_").append(PERSIST_FILE_NAME_OLD);
            this.persistFile.append(System.getProperty(TESTER_HOST_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
                    .append(File.separator).append(this.appName).append("_").append(PERSIST_FILE_NAME);
        }

        File fnOld = new File(persistFileOld.toString());
        File fn = new File(persistFile.toString());

        if (this.loadOld(fnOld)) {
            this.store();
        } else {
            this.load(fn);
        }
        if (fnOld.exists())
            fnOld.delete();

    }

    public ConfigurationData getConfigurationData() {
        return this.configurationData;
    }

    public M3uaMan getM3uaMan() {
        return this.m3ua;
    }

    public SccpMan getSccpMan() {
        return this.sccp;
    }

    public CapMan getCapMan() {
        return this.cap;
    }

    public TestCapSsfMan getTestCapSsfMan() {
        return this.testCapSsfMan;
    }

    private void setupLog4j(String appName) {

        // InputStream inStreamLog4j = getClass().getResourceAsStream("/log4j.properties");

        String propFileName = appName + ".log4j.properties";
        File f = new File("./" + propFileName);
        if (f.exists()) {

            try {
                InputStream inStreamLog4j = new FileInputStream(f);
                Properties propertiesLog4j = new Properties();

                propertiesLog4j.load(inStreamLog4j);
                PropertyConfigurator.configure(propertiesLog4j);
            } catch (Exception e) {
                e.printStackTrace();
                BasicConfigurator.configure();
            }
        } else {
            BasicConfigurator.configure();
        }

        // logger.setLevel(Level.TRACE);
        logger.debug("log4j configured");

    }

    public void sendNotif(String source, String msg, Throwable e, Level logLevel) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement st : e.getStackTrace()) {
            if (sb.length() > 0)
                sb.append("\n");
            sb.append(st.toString());
        }
        this.doSendNotif(source, msg + " - " + e.toString(), sb.toString());

        logger.log(logLevel, msg, e);
        // if (showInConsole) {
        // logger.error(msg, e);
        // } else {
        // logger.debug(msg, e);
        // }
    }

    public void sendNotif(String source, String msg, String userData, Level logLevel) {

       this.doSendNotif(source, msg, userData);
       logger.log(Level.INFO, msg + "\n" + userData);
    }

    private synchronized void doSendNotif(String source, String msg, String userData) {
        Notification notif = new Notification(SS7_EVENT + "-" + source, "TesterHost", ++sequenceNumber,
                System.currentTimeMillis(), msg);
        notif.setUserData(userData);
        this.sendNotification(notif);
    }

    public boolean isNeedQuit() {
        return needQuit;
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public Instance_L1 getInstance_L1() {
        return configurationData.getInstance_L1();
    }

    @Override
    public void setInstance_L1(Instance_L1 val) {
        configurationData.setInstance_L1(val);
        this.markStore();
    }

    @Override
    public Instance_L2 getInstance_L2() {
        return configurationData.getInstance_L2();
    }

    @Override
    public void setInstance_L2(Instance_L2 val) {
        configurationData.setInstance_L2(val);
        this.markStore();
    }

    @Override
    public Instance_L3 getInstance_L3() {
        return configurationData.getInstance_L3();
    }

    @Override
    public void setInstance_L3(Instance_L3 val) {
        configurationData.setInstance_L3(val);
        this.markStore();
    }

    @Override
    public Instance_TestTask getInstance_TestTask() {
        return configurationData.getInstance_TestTask();
    }

    @Override
    public void setInstance_TestTask(Instance_TestTask val) {
        configurationData.setInstance_TestTask(val);
        this.markStore();
    }

    @Override
    public String getInstance_L1_Value() {
        return configurationData.getInstance_L1().toString();
    }

    @Override
    public String getInstance_L2_Value() {
        return configurationData.getInstance_L2().toString();
    }

    @Override
    public String getInstance_L3_Value() {
        return configurationData.getInstance_L3().toString();
    }

    @Override
    public String getInstance_TestTask_Value() {
        return configurationData.getInstance_TestTask().toString();
    }

    @Override
    public String getState() {
        return TesterHost.SOURCE_NAME + ": " + (this.isStarted() ? "Started" : "Stopped");
    }

    @Override
    public String getL1State() {
        if (this.instance_L1_B != null)
            return this.instance_L1_B.getState();
        else
            return "";
    }

    @Override
    public String getL2State() {
        if (this.instance_L2_B != null)
            return this.instance_L2_B.getState();
        else
            return "";
    }

    @Override
    public String getL3State() {
        if (this.instance_L3_B != null)
            return this.instance_L3_B.getState();
        else
            return "";
    }

    @Override
    public String getTestTaskState() {
        if (this.instance_TestTask_B != null)
            return this.instance_TestTask_B.getState();
        else
            return "";
    }

    @Override
    public void start() {

        this.store();
        this.stop();

        // L1
        boolean started = false;
        Mtp3UserPart mtp3UserPart = null;
        switch (this.configurationData.getInstance_L1().intValue()) {
            case Instance_L1.VAL_M3UA:
                this.instance_L1_B = this.m3ua;
                started = this.m3ua.start();
                mtp3UserPart = this.m3ua.getMtp3UserPart();
                break;
            default:
                // TODO: implement others test tasks ...
                this.sendNotif(TesterHost.SOURCE_NAME, "Instance_L1." + this.configurationData.getInstance_L1().toString()
                        + " has not been implemented yet", "", Level.WARN);
                break;
        }
        if (!started) {
            this.sendNotif(TesterHost.SOURCE_NAME, "Layer 1 has not started", "", Level.WARN);
            this.stop();
            return;
        }

        // L2
        started = false;
        SccpStack sccpStack = null;
        switch (this.configurationData.getInstance_L2().intValue()) {
            case Instance_L2.VAL_SCCP:
                if (mtp3UserPart == null) {
                    this.sendNotif(TesterHost.SOURCE_NAME, "Error initializing SCCP: No Mtp3UserPart is defined at L1", "",
                            Level.WARN);
                } else {
                    this.instance_L2_B = this.sccp;
                    this.sccp.setMtp3UserPart(mtp3UserPart);
                    started = this.sccp.start();
                    sccpStack = this.sccp.getSccpStack();
                }
                break;
            case Instance_L2.VAL_ISUP:
                // TODO Implement L2 = ISUP
                this.sendNotif(TesterHost.SOURCE_NAME, "Instance_L2.VAL_ISUP has not been implemented yet", "", Level.WARN);
                break;

            default:
                // TODO: implement others test tasks ...
                this.sendNotif(TesterHost.SOURCE_NAME, "Instance_L2." + this.configurationData.getInstance_L2().toString()
                        + " has not been implemented yet", "", Level.WARN);
                break;
        }
        if (!started) {
            this.sendNotif(TesterHost.SOURCE_NAME, "Layer 2 has not started", "", Level.WARN);
            this.stop();
            return;
        }

        // L3
        started = false;
        CapMan curCap = null;
        switch (this.configurationData.getInstance_L3().intValue()) {
            case Instance_L3.VAL_CAP:
                if (sccpStack == null) {
                    this.sendNotif(TesterHost.SOURCE_NAME, "Error initializing TCAP+CAP: No SccpStack is defined at L2", "",
                            Level.WARN);
                } else {
                    this.instance_L3_B = this.cap;
                    this.cap.setSccpStack(sccpStack);
                    started = this.cap.start();
                    curCap = this.cap;
                }
                break;
            default:
                // TODO: implement others test tasks ...
                this.sendNotif(TesterHost.SOURCE_NAME, "Instance_L3." + this.configurationData.getInstance_L3().toString()
                        + " has not been implemented yet", "", Level.WARN);
                break;
        }
        if (!started) {
            this.sendNotif(TesterHost.SOURCE_NAME, "Layer 3 has not started", "", Level.WARN);
            this.stop();
            return;
        }

        // Testers
        started = false;
        switch (this.configurationData.getInstance_TestTask().intValue()) {
            case Instance_TestTask.VAL_CAP_TEST_SSF:
                if (curCap == null) {
                    this.sendNotif(TesterHost.SOURCE_NAME,
                            "Error initializing VAL_CAP_TEST_SSF: No CAP stack is defined at L3", "", Level.WARN);
                } else {
                    this.instance_TestTask_B = this.testCapSsfMan;
                    this.testCapSsfMan.setCapMan(curCap);
                    started = this.testCapSsfMan.start();
                }
                break;

            default:
                // TODO: implement others test tasks ...
                this.sendNotif(TesterHost.SOURCE_NAME, "Instance_TestTask."
                        + this.configurationData.getInstance_TestTask().toString() + " has not been implemented yet", "",
                        Level.WARN);
                break;
        }
        if (!started) {
            this.sendNotif(TesterHost.SOURCE_NAME, "Testing task has not started", "", Level.WARN);
            this.stop();
            return;
        }

        this.isStarted = true;
    }

    @Override
    public void stop() {

        this.isStarted = false;

        // TestTask
        if (this.instance_TestTask_B != null) {
            this.instance_TestTask_B.stop();
            this.instance_TestTask_B = null;
        }

        // L3
        if (this.instance_L3_B != null) {
            this.instance_L3_B.stop();
            this.instance_L3_B = null;
        }

        // L2
        if (this.instance_L2_B != null) {
            this.instance_L2_B.stop();
            this.instance_L2_B = null;
        }

        // L1
        if (this.instance_L1_B != null) {
            this.instance_L1_B.stop();
            this.instance_L1_B = null;
        }
    }

    @Override
    public void execute() {
        if (this.instance_L1_B != null) {
            this.instance_L1_B.execute();
        }
        if (this.instance_L2_B != null) {
            this.instance_L2_B.execute();
        }
        if (this.instance_L3_B != null) {
            this.instance_L3_B.execute();
        }
        if (this.instance_TestTask_B != null) {
            this.instance_TestTask_B.execute();
        }
    }

    @Override
    public void quit() {
        this.stop();
        this.store();
        this.needQuit = true;
    }

    @Override
    public void putInstance_L1Value(String val) {
        Instance_L1 x = Instance_L1.createInstance(val);
        if (x != null)
            this.setInstance_L1(x);
    }

    @Override
    public void putInstance_L2Value(String val) {
        Instance_L2 x = Instance_L2.createInstance(val);
        if (x != null)
            this.setInstance_L2(x);
    }

    @Override
    public void putInstance_L3Value(String val) {
        Instance_L3 x = Instance_L3.createInstance(val);
        if (x != null)
            this.setInstance_L3(x);
    }

    @Override
    public void putInstance_TestTaskValue(String val) {
        Instance_TestTask x = Instance_TestTask.createInstance(val);
        if (x != null)
            this.setInstance_TestTask(x);
    }

    public String getName() {
        return appName;
    }

    public String getPersistDir() {
        return persistDir;
    }

//    public void setPersistDir(String persistDir) {
//        this.persistDir = persistDir;
//    }

    public boolean getConfigureStackFromFile() {
        return this.configureStackFromFile;
    }

    public void markStore() {
        needStore = true;
    }

    public void checkStore() {
        if (needStore) {
            needStore = false;
            this.store();
        }
    }

    public synchronized void store() {

        try {
            XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
            writer.setBinding(binding);
            // writer.setReferenceResolver(new XMLReferenceResolver());
            writer.setIndentation(TAB_INDENT);

            writer.write(this.configurationData, CONFIGURATION_DATA, ConfigurationData.class);

            writer.close();
        } catch (Exception e) {
            this.sendNotif(SOURCE_NAME, "Error while persisting the Host state in file", e, Level.ERROR);
        }
    }

    private boolean load(File fn) {

        XMLObjectReader reader = null;
        try {
            if (!fn.exists()) {
                this.sendNotif(SOURCE_NAME, "Error while reading the Host state from file: file not found: " + persistFile, "",
                        Level.WARN);
                return false;
            }

            reader = XMLObjectReader.newInstance(new FileInputStream(fn));

            reader.setBinding(binding);

            this.configurationData = reader.read(CONFIGURATION_DATA, ConfigurationData.class);

            reader.close();

            return true;

        } catch (Exception ex) {
            this.sendNotif(SOURCE_NAME, "Error while reading the Host state from file", ex, Level.WARN);
            return false;
        }
    }

    private boolean loadOld(File fn) {

        XMLObjectReader reader = null;
        try {
            if (!fn.exists()) {
                // this.sendNotif(SOURCE_NAME, "Error while reading the Host state from file: file not found: " + persistFile,
                // "", Level.WARN);
                return false;
            }

            reader = XMLObjectReader.newInstance(new FileInputStream(fn));

            reader.setBinding(binding);
            this.configurationData.setInstance_L1(Instance_L1.createInstance(reader.read(ConfigurationData.INSTANCE_L1,
                    String.class)));
            this.configurationData.setInstance_L2(Instance_L2.createInstance(reader.read(ConfigurationData.INSTANCE_L2,
                    String.class)));
            this.configurationData.setInstance_L3(Instance_L3.createInstance(reader.read(ConfigurationData.INSTANCE_L3,
                    String.class)));
            this.configurationData.setInstance_TestTask(Instance_TestTask.createInstance(reader.read(
                    ConfigurationData.INSTANCE_TESTTASK, String.class)));

            M3uaConfigurationData_OldFormat _m3ua = reader.read(ConfigurationData.M3UA, M3uaConfigurationData_OldFormat.class);
            this.m3ua.setSctpLocalHost(_m3ua.getLocalHost());
            this.m3ua.setSctpLocalPort(_m3ua.getLocalPort());
            this.m3ua.setSctpRemoteHost(_m3ua.getRemoteHost());
            this.m3ua.setSctpRemotePort(_m3ua.getRemotePort());
            this.configurationData.getM3uaConfigurationData().setIpChannelType(_m3ua.getIpChannelType());
            this.m3ua.setSctpIsServer(_m3ua.getIsSctpServer());
            this.m3ua.doSetExtraHostAddresses(_m3ua.getSctpExtraHostAddresses());
            this.configurationData.getM3uaConfigurationData().setM3uaFunctionality(_m3ua.getM3uaFunctionality());
            this.configurationData.getM3uaConfigurationData().setM3uaIPSPType(_m3ua.getM3uaIPSPType());
            this.configurationData.getM3uaConfigurationData().setM3uaExchangeType(_m3ua.getM3uaExchangeType());
            this.m3ua.setM3uaDpc(_m3ua.getDpc());
            this.m3ua.setM3uaOpc(_m3ua.getOpc());
            this.m3ua.setM3uaSi(_m3ua.getSi());


            SccpConfigurationData_OldFormat _sccp = reader.read(ConfigurationData.SCCP, SccpConfigurationData_OldFormat.class);
            this.sccp.setRouteOnGtMode(_sccp.isRouteOnGtMode());
            this.sccp.setRemoteSpc(_sccp.getRemoteSpc());
            this.sccp.setLocalSpc(_sccp.getLocalSpc());
            this.sccp.setNi(_sccp.getNi());
            this.sccp.setRemoteSsn(_sccp.getRemoteSsn());
            this.sccp.setLocalSsn(_sccp.getLocalSsn());
            this.sccp.setGlobalTitleType(_sccp.getGlobalTitleType());
            this.sccp.setNatureOfAddress(new NatureOfAddressType(_sccp.getNatureOfAddress().getValue()));
            this.sccp.setNumberingPlan(new NumberingPlanSccpType(_sccp.getNumberingPlan().getValue()));
            this.sccp.setTranslationType(_sccp.getTranslationType());
            this.sccp.setCallingPartyAddressDigits(_sccp.getCallingPartyAddressDigits());
            // this.sccp.setExtraLocalAddressDigits(_sccp.getExtraLocalAddressDigits());


            reader.close();

            return true;

        } catch (Exception ex) {
            this.sendNotif(SOURCE_NAME, "Error while reading the Host state from file", ex, Level.WARN);
            return false;
        }
    }
}
