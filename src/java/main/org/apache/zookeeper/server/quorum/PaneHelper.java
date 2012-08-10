package org.apache.zookeeper.server.quorum;

import java.net.InetAddress;
import java.util.LinkedList;
import paneclient.*;
import java.io.IOException;

public class PaneHelper {

    PaneClient _client;
    PaneShare _root;
    InetAddress _myIP;
    int _paneResvSec;
    int _quorumPort;
    int _electionPort;
    int _clientPort;
    int _bandwidth;
    double _lastTime;
    double _currentTime;
    double _timeout;
    LinkedList<InetAddress> _peers;

    public void setTimeout(double timeout) {
    	//if no remaining Time, give a 0
        _timeout = timeout;
    }   
    
    public double getTimeout() {
        return _timeout;
    }

    public double getRemainingTime() {
    	double ret = _paneResvSec - ((double)System.nanoTime() - _lastTime);
    	return ret > 0? ret:0;	
    }

    public void setLastTime(double time) {
        _lastTime = time;
    }
    
    public void setCurrentTime(double time) {
        _currentTime = time;
    }

    public double getLastTime() {
        return _lastTime;
    }

    public double getCurrentTime() {
        return _currentTime;
    }

    public double getPaneResvSec() {
        return _paneResvSec;
    }

    public void set(PaneClient client, PaneShare root, InetAddress myIP, 
         int quorumPort, int electionPort, int clientPort, int paneResvSec, LinkedList<InetAddress> peers, int bandwidth) {
        _client = client;
        _root = root;
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
        PaneFlowGroup fg = new PaneFlowGroup();

        start.setRelativeTime(0);
        end.setRelativeTime(_paneResvSec);
        
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setSrcPort(_quorumPort);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _root.reserve(resv);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setDstPort(_quorumPort);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _root.reserve(resv);
        
        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setSrcPort(_clientPort);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _root.reserve(resv);

        fg = new PaneFlowGroup();
        fg.setDstHost(_myIP);
        fg.setSrcHost(srcHost);
        fg.setDstPort(_clientPort);
        resv = new PaneReservation(_bandwidth, fg, start, end);
        _root.reserve(resv);        

    }

    
}
