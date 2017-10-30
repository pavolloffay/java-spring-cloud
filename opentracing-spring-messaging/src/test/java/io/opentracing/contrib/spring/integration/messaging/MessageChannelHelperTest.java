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
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessageChannel;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageChannelHelperTest {

  @Mock
  private MessageChannel messageChannel;

  @Mock
  private AbstractMessageChannel abstractMessageChannel;

  private MessageChannelHelper messageChannelHelper;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    messageChannelHelper = new MessageChannelHelper();
  }

  @Test
  public void shouldGetDefaultName() {
    when(messageChannel.toString()).thenReturn("test");
    assertThat(messageChannelHelper.getName(messageChannel)).isEqualTo("test");
  }

  @Test
  public void shouldGetNameFromAnAbstractMessageChannel() {
    when(abstractMessageChannel.getFullChannelName()).thenReturn("test");
    assertThat(messageChannelHelper.getName(abstractMessageChannel)).isEqualTo("test");
  }

  // TODO test IntegrationObjectSupport

}
