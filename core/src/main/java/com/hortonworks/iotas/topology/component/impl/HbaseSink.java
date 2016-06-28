package com.hortonworks.iotas.topology.component.impl;

import com.hortonworks.iotas.topology.component.IotasSink;
import com.hortonworks.iotas.topology.component.TopologyDagVisitor;

public class HbaseSink extends IotasSink {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}