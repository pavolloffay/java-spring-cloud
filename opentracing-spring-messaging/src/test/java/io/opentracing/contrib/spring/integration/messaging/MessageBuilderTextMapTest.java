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

import java.util.AbstractMap;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageBuilderTextMapTest {

  @Test
  public void shouldAddHeader() {
    MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("test");
    MessageBuilderTextMap map = new MessageBuilderTextMap(messageBuilder);
    map.put("testKey", "testValue");

    Message<String> message = messageBuilder.build();
    assertThat(message.getHeaders()).containsKeys("testKey");
    assertThat(message.getHeaders()).containsValue("testValue");
  }

  @Test
  public void shouldGetIterator() {
    MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("test");
    messageBuilder.setHeader("testKey", "testValue");
    MessageBuilderTextMap map = new MessageBuilderTextMap(messageBuilder);

    assertThat(map.iterator()).contains(new AbstractMap.SimpleEntry<>("testKey", "testValue"));
  }

}
