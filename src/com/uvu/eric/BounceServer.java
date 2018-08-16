package com.uvu.eric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.net.InetSocketAddress;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BounceServer implements Runnable{
    private volatile static Hashtable<String, SocketChannel> clients = new Hashtable<>();
    private final static int DEFAULT_BUFFER_SIZE = 1024;
    private final static String BALL = "ball";
    private final static String DISCONNECT = "disconnect";
    private String Name;
    private SocketChannel Client;

    public BounceServer(SocketChannel clientSocket, String clientName) {
        Client = clientSocket;
        Name = clientName;
    }

    public static void main(String[] args) {
        System.out.println("Threaded Echo Server");
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress(5000));
            while (true) {
                System.out.println("Waiting for connection.....");
                SocketChannel toBeClient = serverSocket.accept();
                String name = new String();
                ByteBuffer temp = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                toBeClient.read(temp);
                temp.flip();
                while (temp.hasRemaining())
                {
                    name += (char)temp.get();
                }
                if(clients.containsKey(name)){
                    toBeClient.close();
                    //If i want i could add some error handling here and send a msg back to client
                    continue;
                }
                clients.put(name, toBeClient);
                new Thread(new BounceServer(toBeClient, name)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Threaded Echo Server Terminating");
}

    @Override
    public void run() {
        System.out.println("Connected to client using [" + Thread.currentThread() + "]");
        boolean quit = false;
        try {
            while(!quit) {
                ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                while (Client.isOpen() && Client.read(buffer) > 0) {
                    buffer.flip();
                    String input = new String(buffer.array());
                    switch (input.split("\\s")[0].trim()) {
                        case BALL:
                            if (clients.size() == 1) {
                                break;
                            }
                            Random rand = new Random();
                            int indexChoice = rand.nextInt(clients.size());
                            if (clients.keySet().toArray()[indexChoice] == Name)
                                indexChoice = ++indexChoice % clients.size();
                            buffer.put(BALL.getBytes());
                            buffer.flip();
                            clients.get(clients.keySet().toArray()[indexChoice]).write(buffer);
                            break;
                        case DISCONNECT:
                            //Note: This will disconnect any client from the server if
                            quit = true;
                            String name = input.split("\\s").length > 1 ? input.split("\\s")[1].trim() : "";
                            SocketChannel result = clients.remove(name);
                            if (result != null)
                                result.close();
                            break;
                        default:
                            break;
                    }
                }
            }
            System.out.println("Client [" + Thread.currentThread() + " connection terminated");
        }
            catch (IOException ex) {
            //ex.printStackTrace();
            System.out.println("Exiting thread.");
        }
    }
}
