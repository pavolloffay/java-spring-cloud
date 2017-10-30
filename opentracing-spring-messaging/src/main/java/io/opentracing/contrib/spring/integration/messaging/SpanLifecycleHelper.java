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
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class SpanLifecycleHelper {

  private final Tracer tracer;

  public SpanLifecycleHelper(Tracer tracer) {
    this.tracer = tracer;
  }

  public ActiveSpan start(String operationName, SpanContext parent) {
    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);

    if (parent != null) {
      spanBuilder.ignoreActiveSpan()
          .asChildOf(parent);
    }

    return spanBuilder.startActive();
  }

  public SpanContext getParent(TextMap carrier) {
    SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, carrier);
    if (spanContext != null) {
      return spanContext;
    }

    ActiveSpan span = tracer.activeSpan();
    if (span != null) {
      return span.context();
    }
    return null;
  }

  public void inject(SpanContext spanContext, TextMap carrier) {
    tracer.inject(spanContext, Format.Builtin.TEXT_MAP, carrier);
  }

}
