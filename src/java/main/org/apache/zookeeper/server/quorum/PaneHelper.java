package org.apache.zookeeper.server.quorum;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;

import edu.brown.cs.paneclient.PaneClient;
import edu.brown.cs.paneclient.PaneException;
import edu.brown.cs.paneclient.PaneFlowGroup;
import edu.brown.cs.paneclient.PaneRelativeTime;
import edu.brown.cs.paneclient.PaneReservation;
import edu.brown.cs.paneclient.PaneShare;

public class PaneHelper {

    PaneClient _client;
    PaneShare _share;
    InetAddress _myIP;
    int _paneResvSec;
    int _quorumPort;
    int _electionPort;
    int _clientPort;
    int _bandwidth;
    double _timeout;
    LinkedList<InetAddress> _peers;

    public double getPaneResvSec() {
        return _paneResvSec;
    }

    public void set(PaneClient client, PaneShare share, InetAddress myIP,
         int quorumPort, int electionPort, int clientPort, int paneResvSec, LinkedList<InetAddress> peers, int bandwidth) {
        _client = client;
        _share = share;
        _myIP = myIP;
        _quorumPort = quorumPort;
        _electionPort = electionPort;
        _clientPort = clientPort;
        _paneResvSec = paneResvSec;
        _peers = peers;
        _bandwidth = bandwidth;
    }

    public void makeReservationOnAll() throws PaneException.InvalidResvException, IOException {
        for(InetAddress address : _peers) {
            makeReservation(address);
        }
    }

    public void makeReservation(InetAddress srcHost) throws PaneException.InvalidResvException, IOException {
        PaneReservation resv;
        PaneRelativeTime start = new PaneRelativeTime();
        PaneRelativeTime end = new PaneRelativeTime();
        PaneFlowGroup fg;

        start.setRelativeTime(0);
        end.setRelativeTime(_paneResvSec);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setSrcPort(_quorumPort);
        fg.setTransportProto(PaneFlowGroup.PROTO_TCP);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _share.reserve(resv);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setDstPort(_quorumPort);
        fg.setTransportProto(PaneFlowGroup.PROTO_TCP);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _share.reserve(resv);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setSrcPort(_electionPort);
        fg.setTransportProto(PaneFlowGroup.PROTO_TCP);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _share.reserve(resv);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setDstPort(_electionPort);
        fg.setTransportProto(PaneFlowGroup.PROTO_TCP);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _share.reserve(resv);

    }


}
