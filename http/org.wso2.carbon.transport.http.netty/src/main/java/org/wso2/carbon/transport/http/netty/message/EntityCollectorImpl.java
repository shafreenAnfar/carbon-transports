/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty.message;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Non blocking entity collector
 */
public class EntityCollectorImpl implements EntityCollector {

    private boolean endOfMsgAdded;
    private Queue<HttpContent> messageBody;
    private Channel targetCtx;

    public EntityCollectorImpl() {
        this.messageBody = new LinkedList<>();
        this.endOfMsgAdded = false;
    }

    @Override
    public void addHttpContent(HttpContent httpContent) {
//        messageBody.add(httpContent);
        targetCtx.write(httpContent);
    }

    @Override
    public HttpContent getHttpContent() {
        return messageBody.poll();
    }

    @Override
    public ByteBuffer getMessageBody() {
        return messageBody.poll().content().nioBuffer();
    }

    @Override
    public void addMessageBody(ByteBuffer msgBody) {
//        messageBody.add(new DefaultHttpContent(Unpooled.copiedBuffer(msgBody)));
        targetCtx.write(new DefaultHttpContent(Unpooled.copiedBuffer(msgBody)));
    }

    @Override
    public List<ByteBuffer> getFullMessageBody() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return messageBody.isEmpty();
    }

    @Override
    public int getFullMessageLength() {
        return -1;
    }

    @Override
    public boolean isEndOfMsgAdded() {
        return endOfMsgAdded;
    }

    @Override
    public void markMessageEnd() {

    }

    @Override
    public void setEndOfMsgAdded(boolean endOfMsgAdded) {
        this.targetCtx.flush();
        this.endOfMsgAdded = endOfMsgAdded;
    }

    @Override
    public void release() {

    }

    @Override
    public boolean isAlreadyRead() {
        return false;
    }

    @Override
    public void setAlreadyRead(boolean alreadyRead) {

    }

    public Channel getTargetCtx() {
        return targetCtx;
    }

    public void setTargetCtx(Channel targetCtx) {
        this.targetCtx = targetCtx;
    }
}
