package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

/**
 * @author kameshs
 */
@SpringBootTest(classes = {MockTracingConfiguration.class,  AsyncAnnotationTest.Configuration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncAnnotationTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration {
        @Bean
        public AsyncService delayAsyncService() {
            return new AsyncService();
        }
    }

    static class AsyncService {
        @Autowired
        private MockTracer tracer;

        @Async
        public Future<String> fooAsync() {
            tracer.buildSpan("foo").start().finish();
            return new AsyncResult<>("whatever");
        }

        @Async
        public Future<String> fooException() {
            throw new RuntimeException();
        }

    }

    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private AsyncService asyncService;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testAsyncTraceAndSpans() throws Exception {
        try (ActiveSpan span = mockTracer.buildSpan("bar")
                .startActive()) {
            Future<String> fut = asyncService.fooAsync();
            await().until(() -> fut.isDone());
            assertThat(fut.get()).isNotNull();
        }
        await().until(() -> mockTracer.finishedSpans().size() == 3);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        // parent span from test, span modelling @Async, span inside @Async
        assertEquals(3, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
        MockSpan asyncSpan = mockSpans.get(1);
        assertEquals(3, asyncSpan.tags().size());
        assertEquals(TraceAsyncAspect.TAG_COMPONENT, asyncSpan.tags().get(Tags.COMPONENT.getKey()));
        assertEquals("fooAsync", asyncSpan.tags().get(TraceAsyncAspect.TAG_METHOD));
        assertEquals(AsyncService.class.getSimpleName(), asyncSpan.tags().get(TraceAsyncAspect.TAG_CLASS));
    }

    @Test
    public void testExceptionThrown() {
        try {
            asyncService.fooException();
        } catch (Exception ex) {}
        await().until(() -> mockTracer.finishedSpans().size() == 1);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(1, mockSpans.size());
        assertEquals(4, mockSpans.get(0).tags().size());
        assertEquals(Boolean.TRUE, mockSpans.get(0).tags().get(Tags.ERROR.getKey()));
        assertEquals(1, mockSpans.get(0).logEntries().size());
        assertEquals("error", mockSpans.get(0).logEntries().get(0).fields().get("event"));
        assertTrue(mockSpans.get(0).logEntries().get(0).fields().get("error.object") instanceof RuntimeException);
    }
}