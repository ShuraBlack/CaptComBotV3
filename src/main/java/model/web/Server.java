package model.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    private HttpServer server;

    public Server () {
        try {
            int port = 8000;
            String ip = "0.0.0.0";
            server = HttpServer.create(new InetSocketAddress(ip,port), 0);

            server.createContext("/player/", new PlayerPage());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HttpServer getServer() {
        return server;
    }
}
