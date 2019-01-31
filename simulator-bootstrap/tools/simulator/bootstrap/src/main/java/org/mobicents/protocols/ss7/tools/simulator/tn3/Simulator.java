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

package org.mobicents.protocols.ss7.tools.simulator.tn3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.mobicents.protocols.ss7.tools.simulator.level1.M3uaManMBean;
import org.mobicents.protocols.ss7.tools.simulator.level2.SccpManMBean;
import org.mobicents.protocols.ss7.tools.simulator.level3.CapManMBean;
import org.mobicents.protocols.ss7.tools.simulator.management.TesterHost;
import org.mobicents.protocols.ss7.tools.simulator.management.TesterHostMBean;
import org.mobicents.protocols.ss7.tools.simulator.tests.cap.CamelConfigurationData;
import org.mobicents.protocols.ss7.tools.simulator.tests.cap.TestCapSsfManMBean;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class Simulator implements NotificationListener {


    private TesterHost testerHost;
    private TesterHostMBean testerHostMBean;
    //private M3uaManMBean m3ua; //verificar luego.
    //private SccpManMBean sccp;
    //private CapManMBean cap;
    //private boolean isRemote;

    //private TestCapScfManMBean capScf;
    private TestCapSsfManMBean capSsf;
    private javax.swing.Timer tm;
    //private javax.swing.Timer tm2;

    private int actualProgress;
    private boolean releaseCallReceived = false;
    private boolean establishTemporaryConnectionReceived = false;

    public Simulator() {
        //openTest();
    }

    public TesterHostMBean getHost(){
    	return this.testerHostMBean;
    }

    public int getActualProgress(){
    	return this.actualProgress;
    }

    public boolean getReleaseCallReceived(){
    	return this.releaseCallReceived;
    }

    public boolean getestablishTemporaryConnectionReceived(){
    	return this.establishTemporaryConnectionReceived;
    }

    public void openTest(Map <String, Object> inputParametersMap, CamelConfigurationData camelConfigurationData) {
        // Starting tests
    	int maxCallDuration = ((Integer) inputParametersMap.get("callDuration")).intValue();
    	long acrWaitTime = ((Long) inputParametersMap.get("acrWaitTime")).longValue();

    	this.testerHost.getTestCapSsfMan().setMaxCallDuration(maxCallDuration);
    	this.testerHost.getTestCapSsfMan().setCurrentCallDuration(0);
    	this.testerHost.getTestCapSsfMan().setPercentCallDuration(0);
    	this.testerHost.getTestCapSsfMan().setMaxCallPeriodDuration(0);
    	this.testerHost.getTestCapSsfMan().setPercentCallDuration(0);
    	this.testerHost.getTestCapSsfMan().setCamelConfigurationData(camelConfigurationData);
    	this.testerHost.getTestCapSsfMan().setAcrWaitTime(acrWaitTime);
    	this.actualProgress = 0;
        this.capSsf.performInitialDp("");
    }


    public void refreshState() {

        if (this.testerHostMBean instanceof TesterHost) {
            TesterHost thost = (TesterHost)testerHostMBean;
            thost.execute();
       }

       System.out.println("Level1 State : " + this.testerHostMBean.getL1State());
       System.out.println("Level2 State : " + this.testerHostMBean.getL2State());
       System.out.println("Level3 State : " + this.testerHostMBean.getL3State());
       System.out.println("Task   State : " + this.testerHostMBean.getTestTaskState());

    }

    public boolean getM3uaState() {

       boolean m3uaState = false;
    	if (this.testerHostMBean instanceof TesterHost) {
            TesterHost thost = (TesterHost)testerHostMBean;
            m3uaState = thost.getM3uaMan().getM3uaState();
       }

    	return m3uaState;

    }

    public boolean getSctpState() {
        boolean sctpState = false;
     	if (this.testerHostMBean instanceof TesterHost) {
             TesterHost thost = (TesterHost)testerHostMBean;
             sctpState = thost.getM3uaMan().getSctpState();
        }
        return sctpState;
    }

    public boolean getSccpState() {
        boolean sccpState = false;
     	if (this.testerHostMBean instanceof TesterHost) {
             TesterHost thost = (TesterHost)testerHostMBean;
             sccpState = thost.getSccpMan().getSccpState();
        }
        return sccpState;
    }

    protected void startHost(String appName, boolean isRemote, final TesterHost testerHost, TesterHostMBean host, M3uaManMBean m3ua,
            SccpManMBean sccp, CapManMBean cap, TestCapSsfManMBean capSsf ) {

        this.testerHost = testerHost;
        this.testerHostMBean = host;
        //this.m3ua = m3ua;
        //this.sccp = sccp;
        //this.cap = cap;
        this.capSsf = capSsf;
        //this.capScf = capScf;
        //this.isRemote = isRemote;


        this.tm = new javax.swing.Timer(500, new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                if (testerHost != null) {
                    testerHost.checkStore();
                    testerHost.execute();

                    // TODO: extra action for updating GUI from host notifications if a host is local
                }
            }
        });
        this.tm.start();
        this.testerHostMBean.start(); //added

        /*
        this.tm2 = new javax.swing.Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshState();
            }
        });
        this.tm2.start();
        */
    }

    public void stopHost(){
    	this.testerHostMBean.stop();
        testerHost.quit();
    }

    public void startLocal(Map <String , Object> inputParametersMap) {
        // creating a testerHost
    	String appName = (String) inputParametersMap.get("appName");
    	boolean configureStackFromFile = ((Boolean)inputParametersMap.get("configureStackFromFile")).booleanValue();
        String sim_home = System.getenv(TesterHost.SIMULATOR_HOME_VAR);
        if (sim_home != null)
            sim_home += File.separator + "data";
        TesterHost host = new TesterHost(appName, sim_home, configureStackFromFile);

        host.addNotificationListener(this, null, null);
        this.startHost(appName + "-local", false, host, host, host.getM3uaMan(), host.getSccpMan(),
                host.getCapMan(), host.getTestCapSsfMan());

    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
    	if (notification.getType().equals("SS7Event-TestCapSsf")&& notification.getMessage().equals("Received: applyCharging") && !notification.getUserData().equals("")){
        	this.actualProgress = Integer.valueOf(notification.getUserData().toString().split("%")[0]).intValue();
        	String progressBar = "[";
        	    for (int k = 1; k <= 100; k ++){
                    String character = " ";
                    if (k <= this.actualProgress)
                        character = "#";
                    progressBar += character;
                }
                progressBar += "] " + this.actualProgress + "%";
                System.out.print("\033[H\033[2J");
        	    System.out.flush();
        	    System.out.println(progressBar);
        }else if (notification.getType().equals("SS7Event-TestCapSsf")&& notification.getMessage().equals("Received: releaseCall") && !notification.getUserData().equals("")){
        	this.actualProgress = Integer.valueOf(notification.getUserData().toString().split("%")[0]).intValue();
        	String progressBar = "[";
        	    for (int k = 1; k <= 100; k ++){
                    String character = " ";
                    if (k <= this.actualProgress)
                        character = "#";
                    progressBar += character;
                }
                progressBar += "] " + this.actualProgress + "%";
                System.out.print("\033[H\033[2J");
        	    System.out.flush();
        	    System.out.println(progressBar);
        	    System.out.println("-> RC message received from OCS");
        	    this.releaseCallReceived = true;
        }else if (notification.getType().equals("SS7Event-TestCapSsf")&& notification.getMessage().equals("Received: establishTemporaryConnection") && !notification.getUserData().equals("")){
        	this.actualProgress = Integer.valueOf(notification.getUserData().toString().split("%")[0]).intValue();
        	String progressBar = "[";
        	    for (int k = 1; k <= 100; k ++){
                    String character = " ";
                    if (k <= this.actualProgress)
                        character = "#";
                    progressBar += character;
                }
                progressBar += "] " + this.actualProgress + "%";
                System.out.print("\033[H\033[2J");
        	    System.out.flush();
        	    System.out.println(progressBar);
        	    System.out.println("-> ETC message received from OCS");
        	    this.establishTemporaryConnectionReceived = true;
        }
    }
}
