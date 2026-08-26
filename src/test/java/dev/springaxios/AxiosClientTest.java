package dev.springaxios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AxiosClientTest {

    private HttpServer server;
    private int port;

    public static class User {
        public int id;
        public String name;

        @JsonCreator
        public User(@JsonProperty("id") int id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/users/1", exchange -> {
            byte[] body = "{\"id\":1,\"name\":\"neo\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/api/echo", exchange -> {
            byte[] req = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, req.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(req);
            }
        });
        server.createContext("/api/error", exchange -> {
            exchange.sendResponseHeaders(500, -1);
        });
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private AxiosClient client() {
        return AxiosClient.create(AxiosConfig.builder().baseUrl("http://localhost:" + port + "/api").build());
    }

    @Test
    void getDeserializesJson() {
        AxiosResponse<User> res = client().get("/users/1", User.class);
        Assertions.assertEquals(200, res.getStatus());
        Assertions.assertNotNull(res.getData());
        Assertions.assertEquals(1, res.getData().id);
        Assertions.assertEquals("neo", res.getData().name);
    }

    @Test
    void postEchoesBody() {
        AxiosResponse<User> res = client().post("/echo", new User(2, "trinity"), User.class);
        Assertions.assertEquals(200, res.getStatus());
        Assertions.assertEquals("trinity", res.getData().name);
    }

    @Test
    void errorStatusReturnsResponseNotException() {
        AxiosResponse<Void> res = client().get("/error", Void.class);
        Assertions.assertEquals(500, res.getStatus());
    }

    @Test
    void retriesOnConnectionErrorThenSucceeds() throws Exception {
        // point at a closed port; with no server it should fail fast (maxRetries=0 default)
        AxiosClient failing = AxiosClient.create(AxiosConfig.builder().baseUrl("http://localhost:1").timeoutMs(500).build());
        Assertions.assertThrows(AxiosException.class, () -> failing.get("/x", String.class));
    }
}
