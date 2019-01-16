/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.AsyncTask;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.internal.message.at.AsyncAtStates;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.futures.PeerBarrierState;
import org.pcj.internal.message.at.AsyncAtRequestMessage;
import org.pcj.internal.message.MessagePeerBarrier;
import org.pcj.internal.message.broadcast.BroadcastValueRequestMessage;
import org.pcj.internal.message.get.AsyncGetStates;
import org.pcj.internal.message.get.ValueGetRequestMessage;
import org.pcj.internal.message.put.ValuePutRequestMessage;
import org.pcj.internal.message.put.AsyncPutStates;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class InternalGroup extends InternalCommonGroup implements Group {

    private final int myThreadId;
    private final AtomicInteger barrierRoundCounter;
    private final AsyncGetStates asyncGetStates;
    private final AsyncPutStates asyncPutStates;
    private final AsyncAtStates asyncAtStates;
    private final ConcurrentMap<Integer, PeerBarrierState> peerBarrierStateMap;

    public InternalGroup(int threadId, InternalCommonGroup internalGroup) {
        super(internalGroup);

        this.myThreadId = threadId;

        barrierRoundCounter = new AtomicInteger(0);

        this.asyncGetStates = new AsyncGetStates();
        this.asyncPutStates = new AsyncPutStates();
        this.asyncAtStates = new AsyncAtStates();

        peerBarrierStateMap = new ConcurrentHashMap<>();
    }

    @Override
    public int myId() {
        return myThreadId;
    }

    @Override
    public PcjFuture<Void> asyncBarrier() {
        return super.barrier(myThreadId, barrierRoundCounter.incrementAndGet());
    }

    @Override
    public PcjFuture<Void> asyncBarrier(int threadId) {
        if (myThreadId == threadId) {
            throw new IllegalArgumentException("Given PCJ Thread Id should be different from current PCJ Thread Id.");
        }

        PeerBarrierState peerBarrierState = getPeerBarrierState(threadId);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        MessagePeerBarrier message = new MessagePeerBarrier(super.getGroupId(), myThreadId, threadId);
        InternalPCJ.getNetworker().send(socket, message);

        return peerBarrierState.mineBarrier();
    }

    public PeerBarrierState getPeerBarrierState(int threadId) {
        return peerBarrierStateMap.computeIfAbsent(threadId, key -> new PeerBarrierState());
    }

    @Override
    public <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        AsyncGetStates.State<T> state = asyncGetStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        ValueGetRequestMessage message = new ValueGetRequestMessage(
                        super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    public AsyncGetStates getAsyncGetStates() {
        return asyncGetStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        AsyncPutStates.State state = asyncPutStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        ValuePutRequestMessage message = new ValuePutRequestMessage(
                        super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    public AsyncPutStates getAsyncPutStates() {
        return asyncPutStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        BroadcastStates states = super.getBroadcastStates();
        BroadcastStates.State state = states.create(myThreadId, getChildrenNodes().size());

        BroadcastValueRequestMessage message = new BroadcastValueRequestMessage(
                        super.getGroupId(), state.getRequestNum(), myThreadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        int physicalMasterId = super.getGroupMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalMasterId);

        InternalPCJ.getNetworker().send(masterSocket, message);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) {
        AsyncAtStates.State<T> state = asyncAtStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        AsyncAtRequestMessage<T> message = new AsyncAtRequestMessage<>(
                        super.getGroupId(), state.getRequestNum(), myThreadId,
                threadId, asyncTask);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    public AsyncAtStates getAsyncAtStates() {
        return asyncAtStates;
    }
}
