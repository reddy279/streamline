/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.storm.hbase.bolt;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.TupleUtils;
import backtype.storm.Config;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;

/**
 * Basic bolt for writing to HBase.
 *
 * Note: Each HBaseBolt defined in a topology is tied to a specific table.
 *
 */
public class HBaseBolt  extends AbstractHBaseBolt {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseBolt.class);

    boolean writeToWAL = true;
    List<Mutation> batchMutations;
    List<Tuple> tupleBatch;

    public HBaseBolt(String tableName, HBaseMapper mapper) {
        super(tableName, mapper);
        this.batchMutations = new LinkedList<>();
        this.tupleBatch = new LinkedList<>();
    }

    public HBaseBolt writeToWAL(boolean writeToWAL) {
        this.writeToWAL = writeToWAL;
        return this;
    }

    public HBaseBolt withConfigKey(String configKey) {
        this.configKey = configKey;
        return this;
    }

    public HBaseBolt withBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public HBaseBolt withFlushIntervalSecs(int flushIntervalSecs) {
        this.flushIntervalSecs = flushIntervalSecs;
        return this;
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        Map<String, Object> conf = super.getComponentConfiguration();
        if (conf == null) {
            conf = new Config();
        }

        if (conf.containsKey("topology.message.timeout.secs") && flushIntervalSecs == 0) {
            Integer topologyTimeout = Integer.parseInt(conf.get("topology.message.timeout.secs").toString());
            flushIntervalSecs = (int)(Math.floor(topologyTimeout / 2));
            LOG.debug("Setting flush interval to [{}] based on topology.message.timeout.secs", flushIntervalSecs);
        }

        LOG.info("Enabling tick tuple with interval [{}]", flushIntervalSecs);
        conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, flushIntervalSecs);
        return conf;
    }


    @Override
    public void execute(Tuple tuple) {
        boolean flush = false;
        try {
            if (TupleUtils.isTick(tuple)) {
                LOG.debug("TICK received! current batch status [" + tupleBatch.size() + "/" + batchSize + "]");
                flush = true;
            } else {
                byte[] rowKey = this.mapper.rowKey(tuple);
                ColumnList cols = this.mapper.columns(tuple);
                List<Mutation> mutations = hBaseClient.constructMutationReq(rowKey, cols, writeToWAL? Durability.SYNC_WAL : Durability.SKIP_WAL);
                batchMutations.addAll(mutations);
                tupleBatch.add(tuple);
                if (tupleBatch.size() >= batchSize) {
                    flush = true;
                }
            }

            if (flush && !tupleBatch.isEmpty()) {
                this.hBaseClient.batchMutate(batchMutations);
                LOG.debug("acknowledging tuples after batchMutate");
                for(Tuple t : tupleBatch) {
                    collector.ack(t);
                }
                tupleBatch.clear();
                batchMutations.clear();
            }
        } catch(Exception e){
            this.collector.reportError(e);
            for (Tuple t : tupleBatch) {
                collector.fail(t);
            }
            tupleBatch.clear();
            batchMutations.clear();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}