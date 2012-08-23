package org.apache.zookeeper.server.quorum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;

import paneclient.*;

public class PaneSpeaker implements Runnable {

    int _paneResvSec;
    int _quorumPort;
    int _electionPort;
    int _clientPort;
    InetSocketAddress _paneAddress;
    PaneClientImpl _client;
    PaneShare _share;
    PaneHelper _helper;
    int _bandwidth;
    Long _myid;
    String _userName;
    private static final Logger LOG = LoggerFactory.getLogger(PaneSpeaker.class);
    Map<Long, QuorumServer> _quorumPeers;

    boolean _running;

    public PaneSpeaker(Long myid, Map<Long, QuorumServer> quorumPeers, int quorumPort, int electionPort,
        InetSocketAddress paneAddress, int paneResvSec, int bandwidth, int clientPort, String userName) {
        _myid = myid;
        _quorumPeers = quorumPeers;
        _quorumPort = quorumPort;
        _electionPort = electionPort;
        _clientPort = clientPort;
        _bandwidth = bandwidth;
        _paneResvSec = paneResvSec;
        _paneAddress = paneAddress;
        _userName = userName;
    }

    public int getPaneResvSec() {
        return _paneResvSec;
    }

    public InetSocketAddress getPaneAddress() {
        return _paneAddress;
    }

    public int getClientPort() {
        return _clientPort;
    }

    public int getQuorumPort() {
        return _quorumPort;
    }

    public int getElectionPort() {
        return _electionPort;
    }

    void begin() {
        _running = true;
    }

    void end() {
        _running = false;
    }

    public void run() {
        try {

            _client = new PaneClientImpl(_paneAddress.getAddress(), _paneAddress.getPort());
            _share = _client.getRootShare();
            _client.authenticate(_userName);

            InetAddress myIP = _quorumPeers.get(_myid).addr.getAddress();
            LinkedList<InetAddress> others = new LinkedList<InetAddress>();

            _helper = new PaneHelper();
            _helper.set(_client, _share, myIP, _quorumPort, _electionPort, _clientPort, _paneResvSec, others, _bandwidth);

            for (Entry<Long, QuorumServer> entry : _quorumPeers.entrySet()) {
                Long id = entry.getKey();
                if (id == _myid) {
                    continue;
                }
                InetAddress address = entry.getValue().addr.getAddress();
                others.add(address);
            }

            while(_running) {

                _helper.makeReservationOnAll();
                LOG.info("making reservation for peer port:" + _quorumPort + " and election port:" + _electionPort +
                        " and client port: " + _clientPort + " PANE address:" + _paneAddress.getAddress().getHostAddress() +
                        "and PANE port:" + _paneAddress.getPort());

                int remaining = _paneResvSec;
                int start =  (int) (System.nanoTime()/1000000000);

                while (remaining > 0) {
                    try {
                        Thread.sleep(remaining);
                    } catch(InterruptedException e) {
                        LOG.error("PANE sleeping thread interrupted.");
                    }
                    int now = (int) (System.nanoTime()/1000000000);
                    remaining = _paneResvSec - (now - start);
                }
            }
        } catch(PaneException.InvalidAuthenticateException e) {
            LOG.error("Invalid Authentication Exception:" + e.getMessage());
        } catch(PaneException.InvalidResvException e) {
            LOG.error("Invalid Reservation Exception:" + e.getMessage());
        } catch(IOException e) {
            LOG.error("IOException" + e.getMessage());
        }
    }
}
