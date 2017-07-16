/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.transport.http.netty.sender.channel.pool;


import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.http.netty.sender.channel.TargetChannel;

/**
 * A class which creates a TargetChannel pool for each route.
 */
public class PoolableTargetChannelFactoryPerSrcHndlr implements PoolableObjectFactory {

    private static final Logger log = LoggerFactory.getLogger(PoolableTargetChannelFactoryPerSrcHndlr.class);

    private final ObjectPool genericObjectPool;

    public PoolableTargetChannelFactoryPerSrcHndlr(ObjectPool genericObjectPool) {
        this.genericObjectPool = genericObjectPool;
    }

    @Override
    public Object makeObject() throws Exception {
        TargetChannel targetChannel = (TargetChannel) this.genericObjectPool.borrowObject();
        log.debug("Created channel: {}", targetChannel);
        return targetChannel;
    }

    @Override
    public void destroyObject(Object o) throws Exception {
        if (((TargetChannel) o).getChannel().isActive()) {
            this.genericObjectPool.returnObject(o);
        } else {
            this.genericObjectPool.invalidateObject(o);
        }
        log.debug("Destroying channel: {}", o);
    }

    @Override
    public boolean validateObject(Object o) {
        boolean answer = ((TargetChannel) o).getChannel().isActive();
        log.debug("Validating channel: {} -> {}", o, answer);
        return answer;
    }

    @Override
    public void activateObject(Object o) throws Exception {}

    @Override
    public void passivateObject(Object o) throws Exception {}
}
