/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.remoting;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;

@Deprecated
public interface ChannelHandler extends org.apache.dubbo.remoting.ChannelHandler {

    void connected(org.apache.dubbo.remoting.Channel channel) throws org.apache.dubbo.remoting.RemotingException;

    void disconnected(org.apache.dubbo.remoting.Channel channel) throws org.apache.dubbo.remoting.RemotingException;

    void sent(org.apache.dubbo.remoting.Channel channel, Object message) throws org.apache.dubbo.remoting.RemotingException;

    void received(org.apache.dubbo.remoting.Channel channel, Object message) throws org.apache.dubbo.remoting.RemotingException;

    void caught(org.apache.dubbo.remoting.Channel channel, Throwable exception) throws org.apache.dubbo.remoting.RemotingException;

    @Override
    default void connected(Channel channel) throws RemotingException {

    }

    @Override
    default void disconnected(Channel channel) throws RemotingException {

    }

    @Override
    default void sent(Channel channel, Object message) throws RemotingException {

    }

    @Override
    default void received(Channel channel, Object message) throws RemotingException {

    }

    @Override
    default void caught(Channel channel, Throwable exception) throws RemotingException {

    }
}
