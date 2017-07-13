package io.opentracing.contrib.spring.cloud.feign;

import static org.junit.Assert.assertEquals;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestSpringWebTracing.TestController;
import io.opentracing.contrib.spring.cloud.feign.FeignAutoConfigurationTest.Conf;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class, Conf.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignAutoConfigurationTest {

  @LocalServerPort
  private int port;

  @EnableFeignClients
  @RibbonClient(name = "someservice", configuration = Conf.class)
  public static class Conf {
    @Bean
    public ServerList<Server> ribbonServerList() {
      return new StaticServerList<>(new Server("example.com", 80));
    }
  }

  @FeignClient("example")
  public interface StoreClient {
    @RequestMapping(method = RequestMethod.GET, value = "/")
    String hello();
  }

  @Configuration
  public static class FooConfiguration {
  }


  @Autowired
  private MockTracer mockTracer;

  @Autowired
  private StoreClient storeClient;

  @Test
  public void testTracedRequest() throws InterruptedException {
    storeClient.hello();
    Thread.sleep(2000);
    assertEquals(1, mockTracer.finishedSpans().size());
  }
}
