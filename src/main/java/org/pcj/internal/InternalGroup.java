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
import org.pcj.AsyncTask;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.ReduceOperation;
import org.pcj.internal.message.at.AsyncAtRequestMessage;
import org.pcj.internal.message.at.AsyncAtStates;
import org.pcj.internal.message.barrier.BarrierStates;
import org.pcj.internal.message.broadcast.BroadcastRequestMessage;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.message.collect.CollectRequestMessage;
import org.pcj.internal.message.collect.CollectStates;
import org.pcj.internal.message.get.ValueGetRequestMessage;
import org.pcj.internal.message.get.ValueGetStates;
import org.pcj.internal.message.peerbarrier.PeerBarrierMessage;
import org.pcj.internal.message.peerbarrier.PeerBarrierStates;
import org.pcj.internal.message.put.ValuePutRequestMessage;
import org.pcj.internal.message.put.ValuePutStates;
import org.pcj.internal.message.reduce.ReduceRequestMessage;
import org.pcj.internal.message.reduce.ReduceStates;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class InternalGroup extends InternalCommonGroup implements Group {

    private final int myThreadId;
    private final ValueGetStates valueGetStates;
    private final ValuePutStates valuePutStates;
    private final AsyncAtStates asyncAtStates;
    private final PeerBarrierStates peerBarrierStates;


    public InternalGroup(int threadId, InternalCommonGroup internalGroup) {
        super(internalGroup);

        this.myThreadId = threadId;

        this.valueGetStates = new ValueGetStates();
        this.valuePutStates = new ValuePutStates();
        this.asyncAtStates = new AsyncAtStates();
        this.peerBarrierStates = new PeerBarrierStates();
    }

    public int myId() {
        return myThreadId;
    }

    @Override
    public PcjFuture<Void> asyncBarrier() {
        BarrierStates states = super.getBarrierStates();
        int round = states.getNextRound(myThreadId);
        BarrierStates.State state = states.getOrCreate(round, this);
        state.processLocal(this);

        return state.getFuture();
    }

    @Override
    public PcjFuture<Void> asyncBarrier(int threadId) {
        if (myThreadId == threadId) {
            throw new IllegalArgumentException("Cannot barrier with myself: " + threadId);
        }

        PeerBarrierStates.State state = peerBarrierStates.getOrCreate(threadId);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        PeerBarrierMessage message = new PeerBarrierMessage(super.getGroupId(), myThreadId, threadId);

        InternalPCJ.getNetworker().send(socket, message);

        return state.doMineBarrier();
    }

    public PeerBarrierStates getPeerBarrierStates() {
        return peerBarrierStates;
    }

    @Override
    public <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        ValueGetStates.State<T> state = valueGetStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        ValueGetRequestMessage message = new ValueGetRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                variable.getDeclaringClass().getName(), variable.name(), indices);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncCollect(Enum<?> variable, int... indices) {
        String sharedEnumClassName = variable.getDeclaringClass().getName();
        String variableName = variable.name();

        CollectStates states = super.getCollectStates();
        CollectStates.State<T> state = states.create(myThreadId, this);

        CollectRequestMessage message = new CollectRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId,
                sharedEnumClassName, variableName, indices);

        int physicalMasterId = super.getCommunicationTree().getMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalMasterId);

        InternalPCJ.getNetworker().send(masterSocket, message);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncReduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        String sharedEnumClassName = variable.getDeclaringClass().getName();
        String variableName = variable.name();

        ReduceStates states = super.getReduceStates();
        ReduceStates.State<T> state = states.create(myThreadId, this);

        ReduceRequestMessage<T> message = new ReduceRequestMessage<>(
                super.getGroupId(), state.getRequestNum(), myThreadId,
                sharedEnumClassName, variableName, indices, function
        );

        int physicalMasterId = super.getCommunicationTree().getMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalMasterId);

        InternalPCJ.getNetworker().send(masterSocket, message);

        return state.getFuture();
    }

    public ValueGetStates getValueGetStates() {
        return valueGetStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        ValuePutStates.State state = valuePutStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        ValuePutRequestMessage message = new ValuePutRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    public ValuePutStates getValuePutStates() {
        return valuePutStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        BroadcastStates states = super.getBroadcastStates();
        BroadcastStates.State state = states.create(myThreadId, this);

        BroadcastRequestMessage message = new BroadcastRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId,
                variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        int physicalMasterId = super.getCommunicationTree().getMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalMasterId);

        InternalPCJ.getNetworker().send(masterSocket, message);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) {
        AsyncAtStates.State<T> state = asyncAtStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

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
