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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentracing.ActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenTracingChannelInterceptorTest {

  @Mock
  private Tracer mockTracer;

  @Mock
  private ActiveSpan mockActiveSpan;

  @Mock
  private SpanContext mockSpanContext;

  @Mock
  private MessageChannel mockMessageChannel;

  @Mock
  private SpanLifecycleHelper mockSpanLifecycleHelper;

  @Mock
  private MessageChannelHelper mockMessageChannelHelper;

  private OpenTracingChannelInterceptor interceptor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(mockSpanLifecycleHelper.start(anyString(), any())).thenReturn(mockActiveSpan);
    when(mockMessageChannelHelper.getName(any())).thenReturn("test");
    when(mockActiveSpan.context()).thenReturn(mockSpanContext);

    interceptor = new OpenTracingChannelInterceptor(mockTracer, mockSpanLifecycleHelper, mockMessageChannelHelper);
  }

  @Test
  public void preSendShouldAttachTracingDataInProducerSide() {
    Message<String> messageBefore = MessageBuilder.withPayload("test")
        .setHeader("testKey", "testValue")
        .build();
    Message<?> messageAfter = interceptor.preSend(messageBefore, mockMessageChannel);

    assertThat(messageAfter.getPayload()).isEqualTo("test");
    assertThat(messageAfter.getHeaders()).containsKey("id")
        .containsEntry("testKey", "testValue")
        .containsEntry("messageSent", true);

    verify(mockSpanLifecycleHelper).getParent(any(MessageBuilderTextMap.class));
    verify(mockSpanLifecycleHelper).start("message:test", null);
    verify(mockSpanLifecycleHelper).inject(eq(mockSpanContext), any(MessageBuilderTextMap.class));
    verify(mockMessageChannelHelper).getName(mockMessageChannel);
    verify(mockActiveSpan).log("send");
    verify(mockActiveSpan).setTag("component", "spring-messaging");
    verify(mockActiveSpan).setTag("message_bus.destination", "test");
    verify(mockActiveSpan).setTag("span.kind", "producer");
  }

  @Test
  public void preSendShouldAttachTracingDataInConsumerSide() {
    Message<String> messageBefore = MessageBuilder.withPayload("test")
        .setHeader("testKey", "testValue")
        .setHeader("messageSent", true)
        .build();
    Message<?> messageAfter = interceptor.preSend(messageBefore, mockMessageChannel);

    assertThat(messageAfter.getPayload()).isEqualTo("test");
    assertThat(messageAfter.getHeaders()).containsKey("id")
        .containsEntry("testKey", "testValue")
        .containsEntry("messageSent", true);

    verify(mockSpanLifecycleHelper).getParent(any(MessageBuilderTextMap.class));
    verify(mockSpanLifecycleHelper).start("message:test", null);
    verify(mockSpanLifecycleHelper).inject(eq(mockSpanContext), any(MessageBuilderTextMap.class));
    verify(mockMessageChannelHelper).getName(mockMessageChannel);
    verify(mockActiveSpan).log("received");
    verify(mockActiveSpan).setTag("component", "spring-messaging");
    verify(mockActiveSpan).setTag("message_bus.destination", "test");
    verify(mockActiveSpan).setTag("span.kind", "consumer");
  }

  @Test
  public void preSendShouldAttachTracingDataToAndErrorMessage() {
    Message<?> failedMessage = MessageBuilder.withPayload("test")
        .setHeader("testKey1", "testValue1")
        .build();
    MessagingException messagingException = new MessagingException(failedMessage);
    Message<MessagingException> messageBefore = MessageBuilder.withPayload(messagingException)
        .setHeader("testKey2", "testValue2")
        .build();
    Message<?> messageAfter = interceptor.preSend(messageBefore, mockMessageChannel);

    assertThat(messageAfter.getPayload()).isEqualTo(messagingException);
    assertThat(messageAfter.getHeaders()).containsKey("id")
        .containsEntry("testKey1", "testValue1")
        .containsEntry("testKey2", "testValue2")
        .containsEntry("messageSent", true);

    verify(mockSpanLifecycleHelper).getParent(any(MessageBuilderTextMap.class));
    verify(mockSpanLifecycleHelper).start("message:test", null);
    verify(mockSpanLifecycleHelper).inject(eq(mockSpanContext), any(MessageBuilderTextMap.class));
    verify(mockMessageChannelHelper).getName(mockMessageChannel);
    verify(mockActiveSpan).log("send");
    verify(mockActiveSpan).setTag("component", "spring-messaging");
    verify(mockActiveSpan).setTag("message_bus.destination", "test");
    verify(mockActiveSpan).setTag("span.kind", "producer");
  }

  @Test
  public void afterSendCompletionShouldNotFinishNotExistingSpan() {
    interceptor.afterSendCompletion(null, null, false, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyMap());
  }

  @Test
  public void afterSendCompletionShouldFinishSpanInProducerSide() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterSendCompletion(null, null, false, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyMap());
    verify(mockActiveSpan).close();
    // TODO verify logs
  }

  @Test
  public void afterSendCompletionShouldFinishSpanInConsumerSide() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterSendCompletion(null, null, false, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyMap());
    verify(mockActiveSpan).close();
    // TODO verify logs
  }

  @Test
  public void afterSendCompletionShouldFinishSpanWithException() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterSendCompletion(null, null, false, new Exception("test"));

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Collections.singletonMap("error", "test"));
    verify(mockActiveSpan).close();
  }

  @Test
  public void beforeHandleShouldNotLogEventWithoutSpan() {
    interceptor.beforeHandle(null, null, null);

    verify(mockActiveSpan, times(0)).log("received");
  }

  @Test
  public void beforeHandleShouldLogEvent() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.beforeHandle(null, null, null);

    verify(mockActiveSpan).log("received");
  }

  @Test
  public void afterMessageHandledShouldIgnoreNotExistingSpan() {
    interceptor.afterMessageHandled(null, null, null, null);

    verify(mockActiveSpan, times(0)).log("sent");
    verify(mockActiveSpan, times(0)).log(anyMap());
  }

  @Test
  public void afterMessageHandledShouldLogEvent() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterMessageHandled(null, null, null, null);

    verify(mockActiveSpan).log("sent");
    verify(mockActiveSpan, times(0)).log(anyMap());
  }

  @Test
  public void afterMessageHandledShouldLogEventAndException() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterMessageHandled(null, null, null, new Exception("test"));

    verify(mockActiveSpan).log("sent");
    verify(mockActiveSpan).log(Collections.singletonMap("error", "test"));
  }

}
