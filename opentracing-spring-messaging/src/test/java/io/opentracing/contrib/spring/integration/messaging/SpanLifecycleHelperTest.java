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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentracing.ActiveSpan;
import io.opentracing.BaseSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class SpanLifecycleHelperTest {

  @Mock
  private Tracer mockTracer;

  @Mock
  private Tracer.SpanBuilder mockSpanBuilder;

  @Mock
  private ActiveSpan mockActiveSpan;

  @Mock
  private SpanContext mockSpanContext;

  @Mock
  private TextMap mockCarrier;

  private String operationName = "test-operation";

  private SpanLifecycleHelper spanLifecycleHelper;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.ignoreActiveSpan()).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.startActive()).thenReturn(mockActiveSpan);

    spanLifecycleHelper = new SpanLifecycleHelper(mockTracer);
  }

  @Test
  public void shouldStartSpanWithoutParent() {
    ActiveSpan activeSpan = spanLifecycleHelper.start(operationName, null);

    assertThat(activeSpan).isEqualTo(mockActiveSpan);
    verify(mockTracer).buildSpan(operationName);
    verify(mockSpanBuilder).startActive();
    verify(mockSpanBuilder, times(0)).ignoreActiveSpan();
    verify(mockSpanBuilder, times(0)).asChildOf(any(SpanContext.class));
    verify(mockSpanBuilder, times(0)).asChildOf(any(BaseSpan.class));
  }

  @Test
  public void shouldStartSpanWithParent() {
    ActiveSpan activeSpan = spanLifecycleHelper.start(operationName, mockSpanContext);

    assertThat(activeSpan).isEqualTo(mockActiveSpan);
    verify(mockTracer).buildSpan(operationName);
    verify(mockSpanBuilder).ignoreActiveSpan();
    verify(mockSpanBuilder).asChildOf(mockSpanContext);
    verify(mockSpanBuilder).startActive();
  }

  @Test
  public void shouldNotGetParent() {
    assertThat(spanLifecycleHelper.getParent(mockCarrier)).isNull();
    verify(mockTracer).extract(Format.Builtin.TEXT_MAP, mockCarrier);
    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).context();
  }

  @Test
  public void shouldGetParentFromCarrier() {
    when(mockTracer.extract(Format.Builtin.TEXT_MAP, mockCarrier)).thenReturn(mockSpanContext);

    assertThat(spanLifecycleHelper.getParent(mockCarrier)).isEqualTo(mockSpanContext);
    verify(mockTracer).extract(Format.Builtin.TEXT_MAP, mockCarrier);
    verify(mockTracer, times(0)).activeSpan();
  }

  @Test
  public void shouldGetParentFromActiveSpan() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);
    when(mockActiveSpan.context()).thenReturn(mockSpanContext);

    assertThat(spanLifecycleHelper.getParent(mockCarrier)).isEqualTo(mockSpanContext);
    verify(mockTracer).extract(Format.Builtin.TEXT_MAP, mockCarrier);
    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).context();
  }

  @Test
  public void shouldInject() {
    spanLifecycleHelper.inject(mockSpanContext, mockCarrier);
    verify(mockTracer).inject(mockSpanContext, Format.Builtin.TEXT_MAP, mockCarrier);
  }

}
