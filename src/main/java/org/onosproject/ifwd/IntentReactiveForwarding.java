/*
 * Copiright Edu Reina! for UNIBO
 * Flow controll using flow and intent frameworks of Onos controller for an Specific network topolgy simulated with mininet 
 */
package org.onosproject.ifwd;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ElementId;
//import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
//import org.onosproject.net.flow.DefaultTrafficTreatment.Builder;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
//import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
//import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
//import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.EnumSet;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */
@Component(immediate = true)
public class IntentReactiveForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private ApplicationId appId;

    private static final int DROP_RULE_TIMEOUT = 300;

    private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
                                                                            IntentState.WITHDRAWING,
                                                                            IntentState.WITHDRAW_REQ);

    protected DeviceService deviceService;
    private DeviceId deviceId;
    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.ifwd");

//        this.flowRuleService = flowRuleService;
//        this.deviceService = deviceService;
//        this.deviceId = deviceService.getAvailableDevices().iterator().next().id();

        packetService.addProcessor(processor, PacketProcessor.director(2));

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();

            ElementId actualNode = pkt.receivedFrom().elementId();

            DeviceId actualSwitch = pkt.receivedFrom().deviceId();
            String idActualSwitch = actualSwitch.toString();
//            log.debug("Checking mastership");
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

            switch (idActualSwitch) {
                case "of:0000000000000001":
                    switch (srcId.toString()) {
                        case "00:00:00:00:00:01/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:02/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:03/-1":
                                    packetOut(context, PortNumber.portNumber(3), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:04/-1":
                                    packetOut(context, PortNumber.portNumber(4), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:02/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:01/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:03/-1":
                                    packetOut(context, PortNumber.portNumber(4), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:04/-1":
                                    packetOut(context, PortNumber.portNumber(3), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:03/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:01/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:02/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:04/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:01/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:02/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                        break;
                    }
                    break;
                case "of:0000000000000002":
                    switch (srcId.toString()) {
                        case "00:00:00:00:00:01/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:03/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:04/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:02/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:03/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:04/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:03/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:01/-1":
                                    packetOut(context, PortNumber.portNumber(3), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:02/-1":
                                    packetOut(context, PortNumber.portNumber(4), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:04/-1":
                                    packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case "00:00:00:00:00:04/-1":
                            switch (dstId.toString()) {
                                case "00:00:00:00:00:01/-1":
                                    packetOut(context, PortNumber.portNumber(4), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:02/-1":
                                    packetOut(context, PortNumber.portNumber(3), srcId, dstId);
                                    break;
                                case "00:00:00:00:00:03/-1":
                                    packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case "of:0000000000000003":
                    switch (srcId.toString()) {
                        case "00:00:00:00:00:01/-1":
                            packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                            break;
                        case "00:00:00:00:00:02/-1":
                            packetOut(context, PortNumber.portNumber(2), srcId, dstId);
                            break;
                        case "00:00:00:00:00:03/-1":
                            packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                            break;
                        case "00:00:00:00:00:04/-1":
                            packetOut(context, PortNumber.portNumber(1), srcId, dstId);
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }

        }

   /*         // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            if (dst == null) {
                flood(context);
                return;
            }
*/
            // Otherwise forward and be done with it.
//            setUpConnectivity(context, srcId, dstId);
//            DeviceId actualSwitch = pkt.parsed()
//            forwardPacketToDst(context, dst);

    }


    // Floods the specified packet if permissible.
//    private void flood(PacketContext context) {
//        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
//                                             context.inPacket().receivedFrom())) {
//            packetOut(context, PortNumber.FLOOD);
//        } else {
//            context.block();
//        }
//    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber, HostId srcId, HostId dstId) {
        TrafficTreatment.Builder treatmentBuilder = context.treatmentBuilder().setOutput(portNumber);
        context.send();

        TrafficTreatment treatment = treatmentBuilder.build();
        installIntent(treatment, srcId, dstId);
    }

    private void forwardPacketToDst(PacketContext context, Host dst) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                                                          treatment, context.inPacket().unparsed());
        packetService.emit(packet);
        log.info("sending packet: {}", packet);

    }

    private void installIntent(TrafficTreatment treatment, HostId srcId, HostId dstId) {

        Key key;
        if (srcId.toString().compareTo(dstId.toString()) < 0) {
            key = Key.of(srcId.toString() + dstId.toString(), appId);
        } else {
            key = Key.of(dstId.toString() + srcId.toString(), appId);
        }

        HostToHostIntent hostIntent = HostToHostIntent.builder()
                .appId(appId)
                .key(key)
                .one(srcId)
                .two(dstId)
                .treatment(treatment)
                .build();

        intentService.submit(hostIntent);
    }


  /*
    private FlowRule buildFlowRule(String flow) {
        FlowRuleExtPayLoad payLoad = FlowRuleExtPayLoad.flowRuleExtPayLoad(flow.getBytes());
        FlowRule flowRule = new DefaultFlowRule(deviceId, null, null, 0, appId, 0, false, payLoad);
            return flowRule;
    }


    // Install a rule forwarding the packet to the specified port.
    private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId) {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        Key key;
        if (srcId.toString().compareTo(dstId.toString()) < 0) {
            key = Key.of(srcId.toString() + dstId.toString(), appId);
        } else {
            key = Key.of(dstId.toString() + srcId.toString(), appId);
        }

        HostToHostIntent intent = (HostToHostIntent) intentService.getIntent(key);
        // TODO handle the FAILED state
        if (intent != null) {
            if (WITHDRAWN_STATES.contains(intentService.getIntentState(key))) {
                HostToHostIntent hostIntent = HostToHostIntent.builder()
                        .appId(appId)
                        .key(key)
                        .one(srcId)
                        .two(dstId)
                        .selector(selector)
                        .treatment(treatment)
                        .build();

                intentService.submit(hostIntent);
            } else if (intentService.getIntentState(key) == IntentState.FAILED) {

                TrafficSelector objectiveSelector = DefaultTrafficSelector.builder()
                        .matchEthSrc(srcId.mac()).matchEthDst(dstId.mac()).build();

                TrafficTreatment dropTreatment = DefaultTrafficTreatment.builder()
                        .drop().build();

                ForwardingObjective objective = DefaultForwardingObjective.builder()
                        .withSelector(objectiveSelector)
                        .withTreatment(dropTreatment)
                        .fromApp(appId)
                        .withPriority(intent.priority() - 1)
                        .makeTemporary(DROP_RULE_TIMEOUT)
                        .withFlag(ForwardingObjective.Flag.VERSATILE)
                        .add();

                flowObjectiveService.forward(context.outPacket().sendThrough(), objective);
            }

        } else if (intent == null) {
            HostToHostIntent hostIntent = HostToHostIntent.builder()
                    .appId(appId)
                    .key(key)
                    .one(srcId)
                    .two(dstId)
                    .selector(selector)
                    .treatment(treatment)
                    .build();

            intentService.submit(hostIntent);
        }

    }
*/
}
