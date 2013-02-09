/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.collection.multimap.client;

import com.hazelcast.client.ClientCommandHandler;
import com.hazelcast.collection.CollectionProxyId;
import com.hazelcast.collection.CollectionProxyType;
import com.hazelcast.collection.CollectionService;
import com.hazelcast.collection.multimap.ObjectMultiMapProxy;
import com.hazelcast.instance.Node;
import com.hazelcast.nio.Protocol;
import com.hazelcast.nio.serialization.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class MultiMapCommandHandler extends ClientCommandHandler {
    final protected CollectionService collectionService;

    public MultiMapCommandHandler(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Override
    public Protocol processCall(Node node, Protocol protocol) {
        String name = protocol.args[0];
        CollectionProxyId id = new CollectionProxyId(name, null, CollectionProxyType.MULTI_MAP);
        ObjectMultiMapProxy proxy = (ObjectMultiMapProxy) collectionService.createDistributedObjectForClient(id);
        return processCall(proxy, protocol);
    }

    protected Protocol success(ObjectMultiMapProxy proxy, Protocol protocol, Collection<Object> result) {
        List<Data> buffers = new ArrayList<Data>();
        for (Object o : result) {
            buffers.add(proxy.getNodeEngine().getSerializationService().toData(o));
        }
        return protocol.success(buffers.toArray(new Data[]{}));
    }

    protected abstract Protocol processCall(ObjectMultiMapProxy proxy, Protocol protocol);
}
