package ru.scaytrase;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(8080), 0);
        server.createContext("/", new Handler(args));
        server.start();
    }

    private static class Handler implements HttpHandler {

        Map<String, AtomicLong> counters = new HashMap<>();

        String[] args;

        Handler(String[] args) {
            this.args = args;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Logger logger = Logger.getLogger("Handler");

            logger.info(exchange.getRequestURI().toString());

            if (!"/".equalsIgnoreCase(exchange.getRequestURI().toString())) {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
                return;
            }

            byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();
            var requestBody = new String(requestBodyBytes);
            var inputJson = new JSONObject(requestBody);
            var outputJson = new JSONObject();

            if (inputJson.has("counter")) {
                String name = inputJson.getString("counter");

                if (this.counters.containsKey(name)) {
                    this.counters.get(name).incrementAndGet();
                } else {
                    this.counters.put(name, new AtomicLong(1));
                }
            }

            JSONArray jsonArgs = new JSONArray();
            outputJson.put("args", jsonArgs);
            for (String arg : this.args) {
                jsonArgs.put(arg);
            }

            JSONObject counters = new JSONObject();
            outputJson.put("counters", counters);
            for (Map.Entry<String, AtomicLong> entry : this.counters.entrySet()) {
                counters.put(entry.getKey(), entry.getValue());
            }

            String output = outputJson.toString();
            byte[] bytes = output.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }
}
