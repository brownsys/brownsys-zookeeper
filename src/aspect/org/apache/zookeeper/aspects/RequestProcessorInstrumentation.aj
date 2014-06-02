package org.apache.zookeeper.aspects;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.RequestProcessor;

import edu.brown.cs.systems.resourcetracing.Proxy;
import edu.brown.cs.systems.resourcetracing.ResourceTracing;
import edu.brown.cs.systems.xtrace.Context;
import edu.brown.cs.systems.xtrace.XTrace;

public aspect RequestProcessorInstrumentation {
	
	private static final ResourceTracing.Logger XTRACE = ResourceTracing.getXTraceLogger(RequestProcessorInstrumentation.class);
	
	public interface Tracked {
		public Tracked save();
		public Tracked resume();
	}
	private Context Tracked.xtrace;
	
	declare parents: org.apache.zookeeper.server.Request+ implements Tracked;
	declare parents: org.apache.zookeeper.server.quorum.QuorumPacket+ implements Tracked;
	declare parents: org.apache.zookeeper.ClientCnxn.Packet+ implements Tracked;
	declare parents: org.apache.zookeeper.ClientCnxn.WatcherSetEventPair+ implements Tracked;

	public Tracked Tracked.resume() {
		if (this.xtrace!=null) {
			XTrace.join(this.xtrace);
			//this.xtrace=null;
		}
		return this;
	}
	
	public Tracked Tracked.save() {
		if (XTrace.active()) // && this.xtrace==null)
			this.xtrace = XTrace.get();
		return this;
	}
	
	before(Tracked t): args(t) && (call(* Collection+.add(..)) || call(* Collection+.offer(..)) || call(* Collection+.put(..)) ){
		t.save();
	}
	
	after() returning(Tracked t): call(* Collection+.*(..)) {
		t.resume();
	}
	
	void around(Tracked t, RequestProcessor p): args(t) && this(p) && execution(void RequestProcessor+.processRequest(Request)) {
		if (XTRACE.valid())
			XTRACE.log(thisJoinPointStaticPart, p.getClass().getSimpleName() + " processing request", "Signature", thisJoinPointStaticPart.getSignature().toShortString());
		proceed(t, p);
	}
	
	void around(Object t): args(t) && execution(void org.apache.zookeeper.ClientCnxn.EventThread.processEvent(Object)) {
		if (XTRACE.valid())
			XTRACE.log(thisJoinPointStaticPart, "ClientCnxn Event Thread processing event " + (t==null ? "" : t.toString()));
		try {
			proceed(t);
		} finally {
			if (XTRACE.valid())
				XTRACE.log(thisJoinPointStaticPart, "ClientCnxn Event Thread processing complete");
		}
	}
}
