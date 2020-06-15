package com;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.WebClientSession;

@RunWith(VertxUnitRunner.class)
public class CookieTest {

    private static final String HOST = "0.0.0.0";
    private static final int PORT = 8085;
    private static final String ABS_URL = "http://" + HOST + ":" + PORT;

    public static Vertx vertx;
    public static HttpServer server;

    @BeforeClass
    public static void start(TestContext context) {
        Async completion = context.async();
        vertx = Vertx.vertx();
        server = vertx.createHttpServer(new HttpServerOptions().setHost(HOST).setPort(PORT));
        Router router = Router.router(vertx);
        // cookie
        router.get("/cookie").handler(e ->
                e.response()
                        .addCookie(Cookie.cookie("testName", "testValue"))
                        .addCookie(Cookie.cookie("testName2", "testValue2"))
                        .putHeader(HttpHeaders.LOCATION, ABS_URL + "/cookieCheck")
                        .setStatusCode(301)
                        .end());
        router.get("/cookieCheck").handler(e -> {
            if (e.cookieMap().get("testName") != null && e.cookieMap().get("testName2") != null) {
                e.response().setStatusCode(200).end("cookieCheck");
            } else {
                e.response().setStatusCode(400).end("no cookies");
            }
        });
        server.requestHandler(router).listen(e -> {
            completion.complete();
        });
    }

    @AfterClass
    public static void stop(TestContext context) {
        Async completion = context.async();
        vertx.close(e -> {
            if (e.succeeded()) {
                completion.complete();
            } else {
                context.fail(e.cause());
            }
        });
    }

    // test fails as server received from client only one cookie ("testName", "testValue")
    @Test
    public void cookieTest(TestContext context) {
        WebClientOptions options =
                new WebClientOptions().setDefaultHost(HOST).setDefaultPort(PORT).setProtocolVersion(HttpVersion.HTTP_1_1);
        WebClientSession client = WebClientSession.create(WebClient.create(vertx, options));
        Async completion = context.async();
        client.get(PORT, HOST, "/cookie").send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                System.out.println("Received response with status code" + response.statusCode());
                String resp = response.bodyAsString();
                System.out.println(resp);
                context.assertEquals("cookieCheck", resp);
            } else {
                System.out.println("Something went wrong " + ar.cause().getMessage());
                context.fail(ar.cause());
            }
            completion.complete();
        });

    }

}
