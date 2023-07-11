package org.academiadecodigo.nanderthals;

public class Main {

    public static void main(String[] args) {

        int port = 8080;
        Server server = new Server(port);
        server.start();
    }
}
