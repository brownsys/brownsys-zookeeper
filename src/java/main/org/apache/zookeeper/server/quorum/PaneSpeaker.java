package org.apache.zookeeper.server.quorum;

import paneclient.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import java.util.LinkedList;

public class PaneSpeaker implements Runnable {

    int _paneResvSec;
    int _quorumPort;
    int _electionPort;
    int _clientPort;
    //InetAddress _paneAddress;
    //int _panePort;
    InetSocketAddress _paneAddress;
    PaneClientImpl _client;
    PaneShare _share;
    PaneHelper _helper;
    int _bandwidth;
    Long _myid;
    Logger LOG;
    Map<Long, QuorumServer> _quorumPeers;

    boolean _running;

    public PaneSpeaker(Long myid, Map<Long, QuorumServer> quorumPeers, int quorumPort, int electionPort,
        InetSocketAddress paneAddress, int paneResvSec, int bandwidth, int clientPort, Logger logger) {
        _myid = myid;
        _quorumPeers = quorumPeers;
        _quorumPort = quorumPort;
        _electionPort = electionPort;
        _clientPort = clientPort;
        _bandwidth = bandwidth;
        _paneResvSec = paneResvSec;
        _paneAddress = paneAddress;
        LOG = logger;        
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
    
    @Override
    public void run() {
        try {

            _client = new PaneClientImpl(_paneAddress.getAddress(), _paneAddress.getPort());
            _share = _client.getRootShare();
            _client.authenticate("root");

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
                        " and client port: " + _clientPort + " pane address:" + _paneAddress.getAddress().getHostAddress() + 
                        "and pane port:" + _paneAddress.getPort());                
                try {
                    Thread.sleep(_paneResvSec);
                } catch(InterruptedException e) {
                    LOG.debug("PANE sleeping thread interrupted.");
                }
            } 
        } catch(PaneException.InvalidAuthenticateException e) {
            LOG.debug("Invalid Authentication Exception:" + e.getMessage());           
        } catch(PaneException.InvalidResvException e) {
            LOG.debug("Invalid Reservation Exception:" + e.getMessage());
        } catch(IOException e) {
            LOG.debug("IOException" + e.getMessage());
        }
    }
}
