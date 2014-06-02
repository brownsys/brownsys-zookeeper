/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.quorum;

import java.util.ArrayList;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.ZooDefs.OpCode;
import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.RequestProcessor;

import edu.brown.cs.systems.resourcetracing.resources.QueueResource;
import edu.brown.cs.systems.xtrace.XTrace;

/**
 * This RequestProcessor matches the incoming committed requests with the
 * locally submitted requests. The trick is that locally submitted requests that
 * change the state of the system will come back as incoming committed requests,
 * so we need to match them up.
 */
public class CommitProcessor extends Thread implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CommitProcessor.class);

    /**
     * Requests that we are holding until the commit comes in.
     */
    LinkedList<Request> queuedRequests = new LinkedList<Request>();

    /**
     * Requests that have been committed.
     */
    LinkedList<Request> committedRequests = new LinkedList<Request>();

    RequestProcessor nextProcessor;
    ArrayList<Request> toProcess = new ArrayList<Request>();

    private QueueResource commitRPThreadResource = new QueueResource("CommitRP", 1);
//    private QueueResource committedRequestsQueueResource = new QueueResource("CommitRP_committed", 1);
//    private QueueResource toProcessRequestsQueueResource = new QueueResource("CommitRP_toProcess", 1);

    /**
     * This flag indicates whether we need to wait for a response to come back from the
     * leader or we just let the sync operation flow through like a read. The flag will
     * be true if the CommitProcessor is in a Leader pipeline.
     */
    boolean matchSyncs;

    public CommitProcessor(RequestProcessor nextProcessor, String id, boolean matchSyncs) {
        super("CommitProcessor:" + id);
        this.nextProcessor = nextProcessor;
        this.matchSyncs = matchSyncs;
    }

    volatile boolean finished = false;

    @Override
    public void run() {
        try {
            Request nextPending = null;            
            while (!finished) {
                int len = toProcess.size();
                for (int i = 0; i < len; i++) {
                	Request r = toProcess.get(i);
                	// Retro
                	long start = System.nanoTime();
                	commitRPThreadResource.starting(r.commitRP_enqueue_nanos, start);
                	
                    nextProcessor.processRequest(r);
                    
                    // Retro
                    commitRPThreadResource.finished(r.commitRP_enqueue_nanos, start, System.nanoTime());
                    XTrace.stop();
                }
                toProcess.clear();
                synchronized (this) {
                    if ((queuedRequests.size() == 0 || nextPending != null)
                            && committedRequests.size() == 0) {
                        wait();
                        continue;
                    }
                    // First check and see if the commit came in for the pending
                    // request
                    if ((queuedRequests.size() == 0 || nextPending != null)
                            && committedRequests.size() > 0) {
                        Request r = committedRequests.remove();
                        
                        // Retro
                        long start = System.nanoTime();
                        commitRPThreadResource.starting(r.commitRP_enqueue_nanos, start);

                        /*
                         * We match with nextPending so that we can move to the
                         * next request when it is committed. We also want to
                         * use nextPending because it has the cnxn member set
                         * properly.
                         */
                        if (nextPending != null
                                && nextPending.sessionId == r.sessionId
                                && nextPending.cxid == r.cxid) {
                            // we want to send our version of the request.
                            // the pointer to the connection in the request
                            nextPending.hdr = r.hdr;
                            nextPending.txn = r.txn;
                            nextPending.zxid = r.zxid;

                        	// Retro
                        	nextPending.commitRP_enqueue_nanos = System.nanoTime();
                        	commitRPThreadResource.enqueue();

                        	toProcess.add(nextPending);
                            nextPending = null;
                        } else {
                            // this request came from someone else so just
                            // send the commit packet
                        	
                        	// Retro
                        	r.commitRP_enqueue_nanos = System.nanoTime();
                        	commitRPThreadResource.enqueue();

                        	toProcess.add(r);
                        }
                        
                        // Retro
                        commitRPThreadResource.finished(r.commitRP_enqueue_nanos, start, System.nanoTime());
                        XTrace.stop();
                    }
                }

                // We haven't matched the pending requests, so go back to
                // waiting
                if (nextPending != null) {
                    continue;
                }

                synchronized (this) {
                    // Process the next requests in the queuedRequests
                    while (nextPending == null && queuedRequests.size() > 0) {
                        Request request = queuedRequests.remove();
                        
//                        // Retro
//                        long start = System.nanoTime();
//                        commitRPThreadResource.starting(request.commitRP_enqueue_nanos, start);
                        
                        switch (request.type) {
                        case OpCode.create:
                        case OpCode.delete:
                        case OpCode.setData:
                        case OpCode.multi:
                        case OpCode.setACL:
                        case OpCode.createSession:
                        case OpCode.closeSession:
                            nextPending = request;
                            break;
                        case OpCode.sync:
                            if (matchSyncs) {
                                nextPending = request;
                            } else {
                            	// Retro
                            	request.commitRP_enqueue_nanos = System.nanoTime();
                            	commitRPThreadResource.enqueue();
                            	
                                toProcess.add(request);
                            }
                            break;
                        default:
                        	// Retro
                        	request.commitRP_enqueue_nanos = System.nanoTime();
                        	commitRPThreadResource.enqueue();

                        	toProcess.add(request);
                        }
                        
//                        commitRPThreadResource.finished(request.commitRP_enqueue_nanos, start, System.nanoTime());
                        XTrace.stop();
                    }
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted exception while waiting", e);
        } catch (Throwable e) {
            LOG.error("Unexpected exception causing CommitProcessor to exit", e);
        }
        LOG.info("CommitProcessor exited loop!");
        XTrace.stop();
    }

    synchronized public void commit(Request request) {
        if (!finished) {
            if (request == null) {
                LOG.warn("Committed a null!",
                         new Exception("committing a null! "));
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Committing request:: " + request);
            }
            // Retro
            request.commitRP_enqueue_nanos = System.nanoTime();
            commitRPThreadResource.enqueue();
            
            committedRequests.add(request);
            notifyAll();
        }
    }

    synchronized public void processRequest(Request request) {
        // request.addRQRec(">commit");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        
        if (!finished) {
//        	request.commitRP_enqueue_nanos = System.nanoTime();
//        	commitRPThreadResource.enqueue();
        	
            queuedRequests.add(request);
            notifyAll();
        }
    }

    public void shutdown() {
        LOG.info("Shutting down");
        synchronized (this) {
            finished = true;
            queuedRequests.clear();
            notifyAll();
        }
        if (nextProcessor != null) {
            nextProcessor.shutdown();
        }
    }

}
