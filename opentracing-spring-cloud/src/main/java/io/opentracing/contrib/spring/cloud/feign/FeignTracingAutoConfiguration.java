package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.Feign;
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnClass(Client.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignTracingAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  @ConditionalOnProperty(name = "feign.hystrix.enabled", havingValue = "false", matchIfMissing = true)
  public Feign.Builder feignBuilder(BeanFactory beanFactory) {
    return Feign.builder().client(new TracingClient(getClient(beanFactory), tracer));
  }

  private Client getClient(BeanFactory beanFactory) {
    return beanFactory.getBean(Client.class);
  }
}
