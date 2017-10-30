/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.opentracing.contrib.spring.integration.messaging;

import io.opentracing.ActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Collections;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenTracingChannelInterceptor extends ChannelInterceptorAdapter implements ExecutorChannelInterceptor {

  private static final Log log = LogFactory.getLog(OpenTracingChannelInterceptor.class);

  private static final String MESSAGE_COMPONENT = "message";

  private final Tracer tracer;

  private final SpanLifecycleHelper spanLifecycleHelper;

  private final MessageChannelHelper messageChannelHelper;

  public OpenTracingChannelInterceptor(Tracer tracer, SpanLifecycleHelper spanLifecycleHelper,
      MessageChannelHelper messageChannelHelper) {
    this.tracer = tracer;
    this.spanLifecycleHelper = spanLifecycleHelper;
    this.messageChannelHelper = messageChannelHelper;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    log.trace("Processing message before sending it to the channel");

    // This could be replaced by a headers map.
    Message<?> sanitisedMessage = sanitiseMessage(message);
    MessageBuilder<?> messageBuilder = MessageBuilder.fromMessage(sanitisedMessage);
    MessageBuilderTextMap carrier = new MessageBuilderTextMap(messageBuilder);
    SpanContext parentSpan = spanLifecycleHelper.getParent(carrier);

    log.trace(String.format("Parent span is %s", parentSpan));

    String channelName = messageChannelHelper.getName(channel);
    String operationName = String.format("%s:%s", MESSAGE_COMPONENT, channelName);

    log.trace(String.format("Name of the span will be [%s]", operationName));

    ActiveSpan span = spanLifecycleHelper.start(operationName, parentSpan);

    Tags.COMPONENT.set(span, "spring-messaging");
    Tags.MESSAGE_BUS_DESTINATION.set(span, channelName);

    if (message.getHeaders()
        .containsKey("messageSent")) { // TODO defined header name
      log.trace("Marking span with server received");

      span.log("received"); // TODO define messages
      Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CONSUMER);
    } else {
      log.trace("Marking span with client send");
      span.log("send"); // TODO define messages
      Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_PRODUCER);
      // TODO defined header name
      messageBuilder.setHeader("messageSent", true);
    }

    spanLifecycleHelper.inject(span.context(), carrier);

    MessageHeaderAccessor headers = MessageHeaderAccessor.getMutableAccessor(message);
    headers.copyHeaders(messageBuilder.build()
        .getHeaders());

    // TODO why not recreated a message using builder?
    return new GenericMessage<>(message.getPayload(), headers.getMessageHeaders());
  }

  @Override
  public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
    ActiveSpan activeSpan = tracer.activeSpan();

    log.trace(String.format("Completed sending and current span is %s", activeSpan));

    if (activeSpan == null) {
      return;
    }

    // TODO check server received and log send/receive events

    if (ex != null) {
      // TODO define event type
      activeSpan.log(Collections.singletonMap("error", ex.getMessage()));
    }

    log.trace("Closing messaging span " + activeSpan);

    activeSpan.close(); // TODO is Span#finish() needed?

    log.trace(String.format("Messaging span %s successfully closed", activeSpan));
  }

  @Override
  public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
    ActiveSpan activeSpan = tracer.activeSpan(); // TODO should probably check message headers

    log.trace(String.format("Continuing span %s before handling message", activeSpan));

    if (activeSpan != null) {
      log.trace("Marking span with server received");

      activeSpan.log("received"); // TODO define message

      log.trace(String.format("Span %s successfully continued", activeSpan));
    }

    return message;
  }

  @Override
  public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
    ActiveSpan activeSpan = tracer.activeSpan();

    log.trace(String.format("Continuing span %s after message handled", activeSpan));

    if (activeSpan == null) {
      return;
    }

    log.trace("Marking span with server send");

    activeSpan.log("sent");

    if (ex != null) {
      activeSpan.log(Collections.singletonMap("error", ex.getMessage()));
    }
  }

  private Message<?> sanitiseMessage(Message<?> message) {
    Object payload = message.getPayload();

    if (payload instanceof MessagingException) {
      MessagingException e = (MessagingException) payload;
      return e.getFailedMessage();
    }

    return message;
  }

}
