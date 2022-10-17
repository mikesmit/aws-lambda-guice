package com.coderberry.aws.guice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

public class GuiceRequestHandler implements RequestStreamHandler {
    private final Gson gson = new GsonBuilder().create();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            final String moduleName = getenv("CB_GUICE_MODULE");
            final String handlerName = getenv("CB_HANDLER_CLASS");
            final Class<? extends Module> moduleClass = loadClass(moduleName);
            Constructor<? extends Module> constructor = getConstructor(moduleClass);
            final Module module = createInstance(constructor);
            final Injector injector = Guice.createInjector(module);
            final Class<? extends RequestHandler> handlerClass = loadClass(handlerName);
            RequestHandler handler = injector.getInstance(handlerClass);

            Method[] methods = handlerClass.getMethods();
            Method handleRequest = Arrays.asList(methods).stream()
                    .filter(m -> m.getName().equals("handleRequest")
                            && m.getParameterTypes().length == 2
                            && m.getParameterTypes()[1].equals(Context.class))
                    .findFirst().orElseThrow(() -> new RuntimeException("Couldn't find the 'handleRequest' method"));

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("US-ASCII")));
            Object request = gson.fromJson(reader, handleRequest.getParameterTypes()[0]);
            Object result = handler.handleRequest(request, context);
            try(PrintWriter writer = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(outputStream, Charset.forName("US-ASCII"))))) {
                gson.toJson(result, writer);
            }
        } finally {
            inputStream.close();
        }
    }

    private static <T> T createInstance(Constructor<T> constructor){
        try {
            return  constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Constructor<T> getConstructor(Class<T> moduleClass) {
        try {
            Constructor<T> constructor = moduleClass.getConstructor();
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find no-arg constructor for " + moduleClass.getCanonicalName(), e);
        }
    }

    private static <T> Class<T> loadClass(String name) {
        try {
            final Class<T> moduleClass = (Class<T>) Class.forName(name);
            return moduleClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load the class : " + name, e);
        }
    }

    protected String getenv(String name) {
        return System.getenv(name);
    }
}
