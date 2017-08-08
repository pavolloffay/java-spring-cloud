package io.opentracing.contrib.spring.cloud.jms;

import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Configuration
@EnableJms
public class JmsTestConfiguration {
  private static final Logger log = Logger.getLogger(JmsTestConfiguration.class.getName());

  @Bean
  public MsgListener msgListener() {
    return new MsgListener();
  }

  @Bean
  public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                  DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    // This provides all boot's default to this factory, including the message converter
    configurer.configure(factory, connectionFactory);
    // You could still override some of Boot's default if necessary.
    return factory;
  }

  public static class MsgListener {
    private Message message;

    public Message getMessage() {
      return message;
    }

    @JmsListener(destination = "fooQueue", containerFactory = "myFactory")
    public void processMessage(Message msg) throws Exception {
      log.info("Received msg: " + msg.toString());
      message = msg;
    }
  }
}
