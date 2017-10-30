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

import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageBuilderTextMap implements TextMap {

  private final MessageBuilder<?> delegate;

  public MessageBuilderTextMap(MessageBuilder<?> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    Map<String, String> map = new HashMap<>();
    delegate.build()
        .getHeaders()
        .entrySet()
        .parallelStream()
        .forEach(e -> map.put(e.getKey(), String.valueOf(e.getValue())));

    return map.entrySet()
        .iterator();
  }

  @Override
  public void put(String key, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }

    Message<?> initialMessage = this.delegate.build();
    MessageHeaderAccessor accessor = MessageHeaderAccessor
        .getMutableAccessor(initialMessage);
    accessor.setHeader(key, value);
    if (accessor instanceof NativeMessageHeaderAccessor) {
      NativeMessageHeaderAccessor nativeAccessor = (NativeMessageHeaderAccessor) accessor;
      nativeAccessor.setNativeHeader(key, value);
    }

    delegate.copyHeaders(accessor.toMessageHeaders());
  }
}
