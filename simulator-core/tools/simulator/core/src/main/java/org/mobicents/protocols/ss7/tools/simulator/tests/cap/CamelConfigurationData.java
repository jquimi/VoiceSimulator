
package org.mobicents.protocols.ss7.tools.simulator.tests.cap;

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ApplyChargingReportRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitialDPRequestImpl;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 *
 * @author ccobena/xulloa
 *
 */

public class CamelConfigurationData {

    protected static final String INITIAL_DP_REQUEST = "initialDPRequest";
    protected static final String APPLY_CHARGING_REPORT_REQUEST = "applyChargingReportRequest";

    private InitialDPRequest initialDPRequest = new InitialDPRequestImpl();
    private ApplyChargingReportRequest applyChargingReportRequest = new ApplyChargingReportRequestImpl();

    public InitialDPRequest getInitialDPRequest() {
        return this.initialDPRequest;
    }

    public void setInitialDPRequest(InitialDPRequest initialDPRequest) {
        this.initialDPRequest = initialDPRequest;
    }

    public ApplyChargingReportRequest getApplyChargingReportRequest() {
        return this.applyChargingReportRequest;
    }

    public void setApplyChargingReportRequest(ApplyChargingReportRequest applyChargingReportRequest) {
        this.applyChargingReportRequest = applyChargingReportRequest;
    }

    protected static final XMLFormat<CamelConfigurationData> XML = new XMLFormat<CamelConfigurationData>(
            CamelConfigurationData.class) {

        @Override
    public void write(CamelConfigurationData m3ua, OutputElement xml) throws XMLStreamException {

        }

        @Override
    public void read(InputElement xml, CamelConfigurationData camelConfigurationData) throws XMLStreamException {
            InitialDPRequest initialDPRequest = xml.get(INITIAL_DP_REQUEST, InitialDPRequestImpl.class);
            if (initialDPRequest != null)
                camelConfigurationData.initialDPRequest = initialDPRequest;

            ApplyChargingReportRequest applyChargingReportRequest = xml.get(APPLY_CHARGING_REPORT_REQUEST, ApplyChargingReportRequestImpl.class);
            if (applyChargingReportRequest != null)
                camelConfigurationData.applyChargingReportRequest = applyChargingReportRequest;
        }
    };
}
