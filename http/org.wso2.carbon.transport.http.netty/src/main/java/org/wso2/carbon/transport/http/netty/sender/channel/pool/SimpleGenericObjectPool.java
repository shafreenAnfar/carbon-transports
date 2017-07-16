package org.wso2.carbon.transport.http.netty.sender.channel.pool;

import org.apache.commons.pool.BaseObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Simplified version of the GenericObjectPool. Please note that this is not thread-safe.
 */
public class SimpleGenericObjectPool extends BaseObjectPool {

    private static Logger log = LoggerFactory.getLogger(SimpleGenericObjectPool.class);

    private PoolableObjectFactory factory;
    private List<Object> genericObjects = new LinkedList<>();

    public SimpleGenericObjectPool(PoolableObjectFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object borrowObject() throws Exception {
        if (genericObjects.isEmpty()) {
            return this.factory.makeObject();
        }
        Object o = genericObjects.get(0);
        if (this.factory.validateObject(o)) {
            return o;
        } else {
            genericObjects.remove(o);
            return this.factory.makeObject();
        }
    }

    @Override
    public void returnObject(Object o) throws Exception {
        genericObjects.add(o);
    }

    @Override
    public void invalidateObject(Object o) throws Exception {
        if (genericObjects.remove(o)) {
            factory.destroyObject(o);
        } else {
            log.warn("Couldn't remove the object from the pool");
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        genericObjects.forEach((k) -> {
            Object o = genericObjects.remove(0);
            try {
                factory.destroyObject(o);
            } catch (Exception e) {
                log.error("Couldn't destroy objects in the pool", e);
            }
        });
    }
}
