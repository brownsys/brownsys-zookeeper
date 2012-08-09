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
    int _port1;
    int _port2;
    int _clientPort;
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
         int port1, int port2, int clientPort, int paneResvSec, LinkedList<InetAddress> peers) {
        _client = client;
        _root = root;
    	_myIP = myIP;
    	_port1 = port1;  
    	_port2 = port2;
    	_clientPort = clientPort;
    	_paneResvSec = paneResvSec; 
        _peers = peers; 	
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
        fg.setSrcPort(_port1);
        resv = new PaneReservation(10, fg, start, end);
        _root.reserve(resv);

        fg.setDstPort(_port1);
        resv = new PaneReservation(10, fg, start, end);
        _root.reserve(resv);
        
        fg.setSrcPort(_port2);
        resv = new PaneReservation(10, fg, start, end);
        _root.reserve(resv);

        fg.setDstPort(_port2);
        resv = new PaneReservation(10, fg, start, end);
        _root.reserve(resv);        

    }

    
}
