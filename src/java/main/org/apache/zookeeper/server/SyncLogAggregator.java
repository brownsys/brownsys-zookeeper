package org.apache.zookeeper.server;

import edu.brown.cs.systems.resourcereporting.Resource.Operation;
import edu.brown.cs.systems.resourcereporting.Resource.Type;
import edu.brown.cs.systems.resourcereporting.ZmqReporter;
import edu.brown.cs.systems.resourcereporting.aggregators.ResourceAggregator;
import edu.brown.cs.systems.xtrace.XTrace;

public class SyncLogAggregator extends ResourceAggregator {
	
	private final XTrace.Logger logger;
	private static final ZmqReporter reporter = new ZmqReporter(1000);
	
	public SyncLogAggregator(Class<?> cls, String id) {
		super(Type.HDFSBACKGROUND, id);
		reporter.register("zookeeper", this);
		logger = XTrace.getLogger(cls);
	}

	@Override
	public boolean enabled() {
		return true;
	}
	
	private static long total_written = 0;
	private static long duration = 0;
	private static long previous_sync_duration = 0;
	private static long previous_total_written = 0;
	
	public void write(long bytes, long duration_nanos) {
		if (logger.valid())
			logger.log("Wrote " + bytes + " bytes to sync log");
		total_written += bytes;
		duration += duration_nanos;
	}
	
	public void waitingForFlush() {
		logger.log("Awaiting flush");
		this.starting(Operation.FLUSH, XTrace.getTenantClass());
	}
	
	public void postFlush(long start, long end, long bytes) {
		long sync_work = previous_sync_duration * bytes / previous_total_written;
		this.finished(Operation.FLUSH, XTrace.getTenantClass(), sync_work, end-start);
//		if (logger.valid())
//			logger.log(String.format("Flush complete, waited %d ms for a flush, responsible for %.1f%% of flush", (end-start)/1000000, (100*bytes/(double)previous_total_written)));
	}
	
	public void synced(long duration_nanos) {
		duration += duration_nanos;
		previous_total_written = total_written;
		previous_sync_duration = duration;
		total_written = 0;
		duration = 0;
	}

}
