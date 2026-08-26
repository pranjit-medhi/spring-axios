# spring-axios

An **axios-like HTTP client starter for Spring Boot**. It brings axios ergonomics
(fluent API, instances with defaults, request/response interceptors, JSON
(de)serialization, retries) to the JVM, with first-class Spring Boot integration
(auto-configuration, `application.yml` properties, Micrometer metrics, and an
optional Resilience4j circuit breaker).

- Sync **and** async (`CompletableFuture`) calls
- Per-instance defaults (base URL, headers, timeouts, retries) — like axios instances
- Request/response interceptors
- Automatic JSON (Jackson) (de)serialization with generics support
- Query params, path variables, form/byte bodies
- Timeouts, retries with exponential backoff, retryable-status control
- Optional Micrometer metrics and Resilience4j circuit breaker (no hard dependency)

---

## 1. Installation

The library is published to your Maven repository. Add the dependency:

```xml
<dependency>
    <groupId>dev.springaxios</groupId>
    <artifactId>spring-axios</artifactId>
    <version>0.1.0</version>
</dependency>
```

For metrics, also depend on actuator (provides a `MeterRegistry` bean):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

For the circuit breaker, add Resilience4j:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.2.0</version>
</dependency>
```

> The core starter has **no required** dependency on Micrometer or Resilience4j.
> Those integrations activate automatically only when the libraries (and a
> `MeterRegistry` bean) are present.

---

## 2. Quick start

`AxiosClient` is auto-configured for you. Just inject it:

```java
@Service
public class UserService {

    @Autowired
    private AxiosClient axios;

    public User getUser(long id) {
        AxiosResponse<User> res = axios.get("/users/" + id, User.class);
        return res.getData();
    }
}
```

The base URL and other defaults come from `application.yml` (see §5).

---

## 3. Making requests

### GET

```java
// Untyped (String)
AxiosResponse<String> raw = axios.get("/status", String.class);

// Typed POJO
AxiosResponse<User> user = axios.get("/users/1", User.class);
User u = user.getData();

// Generics (lists, maps) via TypeReference
AxiosResponse<List<User>> users =
        axios.get("/users", new TypeReference<>() {});

// With query params + path vars
AxiosResponse<List<User>> active =
        axios.get("/users", new TypeReference<>() {})
             .getRequest() // ... or build via AxiosRequest (see below)
```

A more explicit way (recommended for generics, query params, path vars):

```java
AxiosResponse<List<User>> res = axios.request(
        AxiosRequest.builder()
            .method("GET")
            .url("/users")
            .queryParam("active", true)
            .queryParam("page", 2)
            .responseTypeRef(new TypeReference<>() {})
            .build());
```

### POST / PUT / PATCH

```java
User created = axios.post("/users", newUser, User.class).getData();
User updated = axios.put ("/users/1", newUser, User.class).getData();
User patched = axios.patch("/users/1", patch,   User.class).getData();
```

### DELETE

```java
axios.delete("/users/1");                       // no body
User deleted = axios.delete("/users/1", User.class).getData();
```

### Async

```java
CompletableFuture<AxiosResponse<User>> f = axios.getAsync("/users/1", User.class);
f.thenAccept(res -> System.out.println(res.getData().name()));

// or a fully custom request
CompletableFuture<AxiosResponse<User>> f2 = axios.requestAsync(
        AxiosRequest.builder().method("GET").url("/users/1").responseType(User.class).build());
```

### Per-request configuration overrides

```java
AxiosResponse<User> res = axios.request(
        AxiosRequest.builder().method("GET").url("/users/1").responseType(User.class).build(),
        AxiosConfig.builder().timeoutMs(2000).maxRetries(5).build());
```

---

## 4. Instances (per-service defaults)

Like axios `axios.create({ ... })`. Derive a client for a specific upstream so you
don't repeat base URL/headers/timeouts:

```java
AxiosClient github = axios.createInstance(
        AxiosConfig.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + token)
            .timeoutMs(3000)
            .maxRetries(3)
            .build());

github.get("/user", GithubUser.class);
```

You can also build a standalone client without Spring:

```java
AxiosClient client = AxiosClient.create(
        AxiosConfig.builder().baseUrl("https://api.example.com").build());
```

---

## 5. Configuration (`application.yml`)

```yaml
axios:
  base-url: https://api.example.com      # default base URL for all calls
  timeout-ms: 5000                        # socket connect/read timeout
  max-retries: 3                          # retry attempts on I/O error or retryable status
  retry-delay-ms: 500                     # base delay between retries
  retry-backoff-multiplier: 2.0           # delay *= multiplier each attempt
  retryable-statuses: [429, 500, 502, 503, 504]
  default-headers:                        # headers sent on every request
    Accept: application/json
  metrics-enabled: true                   # publish Micrometer timers (if actuator present)
  circuit-breaker:
    enabled: false                        # set true to wrap calls in a circuit breaker
    name: axios
    failure-rate-threshold: 50
    slow-call-duration-threshold-ms: 10000
    wait-duration-in-open-state-ms: 5000
    sliding-window-size: 100
    permitted-number-of-calls-in-half-open-state: 10
```

---

## 6. Interceptors

Analogous to axios interceptors. They are applied in order on every request/response.

```java
@Configuration
public class AxiosConfigExt {

    @Bean
    public AxiosClient axiosClientWithInterceptors(AxiosClient base) {
        return base.createInstance(
            AxiosConfig.builder()
                .requestInterceptor(req -> {
                    // e.g. add a trace id header
                    return AxiosRequest.builder()
                        .method(req.getMethod()).url(req.getUrl()).baseUrl(req.getBaseUrl())
                        .body(req.getBody())
                        .headers(new HashMap<>(req.getHeaders()) {{
                            put("X-Trace-Id", UUID.randomUUID().toString());
                        }})
                        .queryParams(req.getQueryParams()).pathVars(req.getPathVars())
                        .responseType(req.getResponseType())
                        .responseTypeRef(req.getResponseTypeRef())
                        .build();
                })
                .responseInterceptor(res -> {
                    // e.g. log or transform
                    return res;
                })
                .build());
    }
}
```

---

## 7. Error handling

- **Connection errors / timeouts** throw `AxiosException` (`getStatus() == 0`).
- **HTTP error statuses (4xx/5xx) do NOT throw** — the `AxiosResponse` is returned
  with the status populated, matching axios behaviour. Check `res.getStatus()` or
  `res.isOk()`.
- Deserialization failures throw `AxiosException`.

```java
try {
    AxiosResponse<User> res = axios.get("/users/1", User.class);
    if (res.isOk()) { /* ... */ }
} catch (AxiosException e) {
    int status = e.getStatus(); // 0 for network errors
}
```

---

## 8. Metrics

When `spring-boot-starter-actuator` is on the classpath and `axios.metrics-enabled`
is `true` (the default), a timer `http.client.requests` is recorded with tags
`client=axios`, `method`, and `host`. Inspect it via `/actuator/metrics/http.client.requests`.

---

## 9. Circuit breaker

When `resilience4j-circuitbreaker` is present and `axios.circuit-breaker.enabled=true`,
calls are wrapped in a Resilience4j `CircuitBreaker`. Tune via the `axios.circuit-breaker.*`
properties. The integration is pluggable — to use a different breaker, supply your own
`CallWrapper` bean:

```java
@Bean
public CallWrapper myBreaker() {
    return callable -> myCustomBreaker.executeCallable((Callable<Object>) callable);
}
```

---

## 10. Building from source

```bash
mvn -B clean install
```

This compiles the library and runs the smoke tests in `AxiosClientTest`.

---

## API cheat-sheet

| axios (JS)                     | spring-axios                              |
|--------------------------------|-------------------------------------------|
| `axios.get(url)`               | `axios.get(url, String.class)`            |
| `axios.get(url).then(cb)`      | `axios.getAsync(url, Type).thenApply(...)`|
| `axios.post(url, data)`        | `axios.post(url, data, Type.class)`       |
| `axios.create(cfg)`            | `axios.createInstance(AxiosConfig...)`    |
| `axios.interceptors.request`   | `AxiosConfig.requestInterceptor(...)`     |
| `response.data`                | `response.getData()`                      |
| `response.status`              | `response.getStatus()`                    |
