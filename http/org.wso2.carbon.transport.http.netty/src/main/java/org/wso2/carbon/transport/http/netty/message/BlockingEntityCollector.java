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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Blocking entity collector
 */
public class BlockingEntityCollector implements EntityCollector {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingEntityCollector.class);

    private int soTimeOut;
    private AtomicBoolean alreadyRead;
    private AtomicBoolean endOfMsgAdded;
    private BlockingQueue<HttpContent> httpContentQueue;
    private BlockingQueue<HttpContent> outContentQueue;
    private BlockingQueue<HttpContent> garbageCollected;

    public BlockingEntityCollector(int soTimeOut) {
        this.soTimeOut = soTimeOut;
        this.alreadyRead = new AtomicBoolean(false);
        this.endOfMsgAdded = new AtomicBoolean(false);
        this.httpContentQueue = new LinkedBlockingQueue<>();
        this.garbageCollected = new LinkedBlockingQueue<>();
        this.outContentQueue = new LinkedBlockingQueue<>();
    }

    public void addHttpContent(HttpContent httpContent) {
        try {
            httpContentQueue.add(httpContent);
        } catch (Exception e) {
            LOG.error("Cannot put content to queue", e);
        }
    }

    public HttpContent getHttpContent() {
        try {
            return httpContentQueue.poll(soTimeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Error while retrieving http content from queue.", e);
            return null;
        }
    }

    public ByteBuffer getMessageBody() {
        try {
            HttpContent httpContent = httpContentQueue.poll(soTimeOut, TimeUnit.SECONDS);
            if (httpContent instanceof LastHttpContent) {
                this.endOfMsgAdded.set(true);
            }
            ByteBuf buf = httpContent.content();
            garbageCollected.add(httpContent);
            return buf.nioBuffer();
        } catch (InterruptedException e) {
            LOG.error("Error while retrieving message body from queue.", e);
            return null;
        }
    }

    public List<ByteBuffer> getFullMessageBody() {
        List<ByteBuffer> byteBufferList = new ArrayList<>();

        boolean isEndOfMessageProcessed = false;
        while (!isEndOfMessageProcessed) {
            try {
                HttpContent httpContent = httpContentQueue.poll(soTimeOut, TimeUnit.SECONDS);
                // This check is to make sure we add the last http content after getClone and avoid adding
                // empty content to bytebuf list again and again
                if (httpContent instanceof EmptyLastHttpContent) {
                    break;
                }

                if (httpContent instanceof LastHttpContent) {
                    isEndOfMessageProcessed = true;
                }
                ByteBuf buf = httpContent.content();
                garbageCollected.add(httpContent);
                byteBufferList.add(buf.nioBuffer());
            } catch (InterruptedException e) {
                LOG.error("Error while getting full message body", e);
            }
        }

        return byteBufferList;
    }

    public boolean isEmpty() {
        return this.httpContentQueue.isEmpty();
    }

    public int getFullMessageLength() {
        List<HttpContent> contentList = new ArrayList<>();
        boolean isEndOfMessageProcessed = false;
        while (!isEndOfMessageProcessed) {
            try {
                HttpContent httpContent = httpContentQueue.poll(soTimeOut, TimeUnit.SECONDS);
                if ((httpContent instanceof LastHttpContent) || (isEndOfMsgAdded() && httpContentQueue.isEmpty())) {
                    isEndOfMessageProcessed = true;
                }
                contentList.add(httpContent);

            } catch (InterruptedException e) {
                LOG.error("Error while getting full message length", e);
            }
        }
        int size = 0;
        for (HttpContent httpContent : contentList) {
            size += httpContent.content().readableBytes();
            httpContentQueue.add(httpContent);
        }

        return size;
    }

    public boolean isEndOfMsgAdded() {
        return endOfMsgAdded.get();
    }

    public void addMessageBody(ByteBuffer msgBody) {
        if (isAlreadyRead()) {
            outContentQueue.add(new DefaultHttpContent(Unpooled.copiedBuffer(msgBody)));
        } else {
            httpContentQueue.add(new DefaultHttpContent(Unpooled.copiedBuffer(msgBody)));
        }
    }

    public void markMessageEnd() {
        if (isAlreadyRead()) {
            outContentQueue.add(new EmptyLastHttpContent());
        } else {
            httpContentQueue.add(new EmptyLastHttpContent());
        }
    }

    public void setEndOfMsgAdded(boolean endOfMsgAdded) {
        this.endOfMsgAdded.compareAndSet(false, endOfMsgAdded);
        if (isAlreadyRead()) {
            httpContentQueue.addAll(outContentQueue);
            outContentQueue.clear();
        }
    }

    public void release() {
        httpContentQueue.forEach(ReferenceCounted::release);
        garbageCollected.forEach(ReferenceCounted::release);
    }

    public boolean isAlreadyRead() {
        return alreadyRead.get();
    }

    public void setAlreadyRead(boolean alreadyRead) {
        this.alreadyRead.set(alreadyRead);
    }

    @Override
    public void setTargetCtx(Channel targetCtx) {

    }
}
