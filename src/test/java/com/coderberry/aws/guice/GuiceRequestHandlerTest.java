package com.coderberry.aws.guice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class GuiceRequestHandlerTest {
    public static class Request{
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
    public static class Result{
        private int code;

        public void setCode(int value) {
            this.code = value;
        }

        public int getCode() {
            return this.code;
        }

    }

    public static class TestHandler implements RequestHandler<Request, Result> {

        @Override
        public Result handleRequest(Request request, Context context) {
            Result r = new Result();
            r.setCode(1234);
            return r;
        }
    }

    public static class TestModule extends AbstractModule {
        @Provides
        @Singleton
        public TestHandler provideTestHandler() {
            return new TestHandler();
        }
    }

    public static class TestGuiceRequestHandler extends GuiceRequestHandler {
        private final Map<String, String> environment;

        public TestGuiceRequestHandler(final Map<String, String> environment) {
            this.environment = environment;
        }
        @Override
        protected String getenv(String name) {
            return environment.get(name);
        }
    }

    @Test
    public void test() throws Exception {
        final String input = "{\"id\":\"id_value\"}";
        Map<String, String> environment = new HashMap<>();
        environment.put("CB_GUICE_MODULE", TestModule.class.getTypeName());
        environment.put("CB_HANDLER_CLASS", TestHandler.class.getTypeName());
        final GuiceRequestHandler handler = new TestGuiceRequestHandler(environment);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Context context = Mockito.mock(Context.class);
        handler.handleRequest(inputStream, outputStream, context);
        final String result = outputStream.toString("US-ASCII");

        Assertions.assertEquals("{\"code\":1234}", result);
    }
}
