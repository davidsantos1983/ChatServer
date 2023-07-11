package org.academiadecodigo.nanderthals;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private int port;
    private ExecutorService executor;
    //private List<PrintWriter> connectedClients;
    private Map<String, PrintWriter> connectedClients;

    public Server(int port) {

        this.port = port;
        this.executor = Executors.newCachedThreadPool();
        //this.connectedClients = new CopyOnWriteArrayList<>();
        this.connectedClients = new HashMap<>();
    }

    public void start() {

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port: " + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());

                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                //connectedClients.add(writer);
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                ServerWorker worker = new ServerWorker(clientSocket, writer, reader, this);
                executor.submit(worker);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {

        for (PrintWriter writer : connectedClients.values()) {
            writer.println(message);
        }
    }

    public synchronized String listClients() {

        StringBuilder clientlist = new StringBuilder();
        clientlist.append("Connected clients:\n");

        for (String client : connectedClients.keySet()) {

            clientlist.append("- ").append(client).append("\n");
        }
        return clientlist.toString();

    }

    public synchronized void whisper(String sender, String recipient, String message) {

        PrintWriter recipientWriter = connectedClients.get(recipient);
        PrintWriter senderWriter = connectedClients.get(sender);

        if (recipientWriter != null && senderWriter != null) {
            recipientWriter.println("[Whisper from " + sender + " ]: " + message);
            senderWriter.println("[You whispered to " + recipient + "]: " + message);
        } else {
            senderWriter.println("User " + recipient + " is not connected.");
        }
    }

    public synchronized void addClient(String username, PrintWriter writer) {

        connectedClients.put(username, writer);
    }

    public synchronized void removeClient(String username) {

        connectedClients.remove(username);
    }

    public void stop() {

        executor.shutdown();
    }

    private class ServerWorker implements Runnable {

        private Socket clientSocket;
        private PrintWriter writer;

        private BufferedReader reader;

        private String username;

        private Server server;

        public ServerWorker(Socket clientSocket, PrintWriter writer, BufferedReader reader, Server server) {

            this.clientSocket = clientSocket;
            this.writer = writer;
            this.reader = reader;
            this.server = server;
        }

        @Override
        public void run() {

            try {

                writer.println("Welcome to the chat server, please make yourself at home!");
                //BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer.println("Please enter your username: ");
                username = reader.readLine();
                writer.println("Welcome " + username + "!");

                server.addClient(username, writer);
                server.broadcast(username + " has joined the chat.");

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("/w")) {

                        handleWhisper(line);

                    }else if (line.startsWith("/l")) {

                        handleListClients();

                    } else {

                        server.broadcast(username + ": " + line);
                    }

                    //System.out.println("Received: " + line);
                    //broadcast(line);
                }

                server.removeClient(username);
                server.broadcast(username + " has left the chat.");

                reader.close();
                writer.close();
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleWhisper(String line) {


            if (line.startsWith("/w")) {

                String[] parts = line.split(" ", 3);

                if (parts.length == 3) {
                    String recipient = parts[1];
                    String message = parts[2];
                    server.whisper(username, recipient, message);

                }else{

                    writer.println("Invalid command!");

                }
            }else if (line.startsWith("/l")) {

              handleListClients();

            } else {

                server.broadcast(username + ": " + line);
            }
        }

        private void handleListClients(){

            String clientsList = server.listClients();
            writer.println(clientsList);
        }
    }
}