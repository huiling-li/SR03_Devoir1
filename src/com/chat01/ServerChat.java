//recevoir tous les msgs d'abord
//puis les renvoyer à chacun
package com.chat01;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerChat extends JFrame {
    private static final int PORT = 8888;
    JTextArea serverTa = new JTextArea();
    private JPanel btnTool = new JPanel();
    private JButton startBtn = new JButton("start");
    private JButton stopBtn = new JButton("stop");
    private JScrollPane sp = new JScrollPane(serverTa);
    boolean isInitial = true;


    private ServerSocket ss = null;//接受socket的容器 本身不是socket
    private Socket s = null;

    private ArrayList<ClientConn> cclist = new ArrayList<ClientConn>();//plusieurs clients
    //另写一个类
    private boolean isStart = false;

    public ServerChat() {
        this.setTitle("fenêtre de serveur");
        this.add(sp, BorderLayout.CENTER);
        btnTool.add(startBtn);
        btnTool.add(stopBtn);//把两个按钮放这个条里
        this.add(btnTool, BorderLayout.SOUTH);
        this.setBounds(0, 0, 650, 500);

        if (isStart) {
            serverTa.append("le serveur est allumé!\n");
        } else {
            serverTa.append("le serveur n'est pas encore activé!\nVeuillez appuyer sur le bouton start!\n");
        }//告诉你状态

        this.addWindowListener(new WindowAdapter() {//si on ferme la fenêtre,on exit
            @Override
            public void windowClosing(WindowEvent e) {
                isStart = false;
                try {//libére serversocket et socket, puis termine le programme
                    if (s != null) {
                        s.close();
                    }
                    if (ss != null) {
                        ss.close();
                    }
                    serverTa.append("le serveur est déactivé!");
                    System.exit(0);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        stopBtn.addActionListener(new ActionListener() {//appuyer sur le bouton"stop",on exit(même opération comme dessus)
            @Override
            public void actionPerformed(ActionEvent e) {
                isStart = false;
                try {
                    if (s != null) {
                        s.close();
                    }
                    if (ss != null) {
                        ss.close();
                    }
                    serverTa.append("le serveur est déactivé!");
                    System.exit(0);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        startBtn.addActionListener(new ActionListener() {//on appuye sur le bouton "start", on crée le serversocket,et on lance
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (ss == null) {
                        ss = new ServerSocket(PORT);//si y a pas de serversocket, on en crée un(startServer)
                    }
                    isStart = true;//met à jour le status de serversocket
                    serverTa.append("le serveur est activé!\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        //la suspension de fenetre de serveur est censé conduire à
//        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        serverTa.setEditable(false);
        this.setVisible(true);
        try {
            startServer();//on lance le serveur à la fin de création de fenêtre
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isStart) {
            serverTa.append("le serveur est allumé!\n");
        } else {
            serverTa.append("le serveur n'est pas encore activé!\nVeuillez appuyer sur le bouton start!\n");
        }//告诉你状态

    }


    public void startServer() throws IOException {
        ss = new ServerSocket(PORT);
        isStart = true;
        while (isStart) {
            s = ss.accept();
            cclist.add(new ClientConn(s));
            System.out.println("un client s'est connecté:" + s.getInetAddress() + "/" + s.getPort() + "\n");
            serverTa.append("un client s'est connecté:" + s.getInetAddress() + "/" + s.getPort() + "\n");
        }
    }

    //stop
//    public void stopServer(){
//    }
//    public void receiveStr(){
//        try {
//            dis=new DataInputStream(s.getInputStream());
//            String str=dis.readUTF();
//            System.out.println(str);
//            serverTa.append(str);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //objet de connection
    class ClientConn implements Runnable {
        Socket s = null;
        String pseudonyme = null;//面向对象，封装的好处，可以把信息捆绑
        int port;
        boolean ifJoin = false;

        public void setPseudonyme(String pseudonyme) {
            this.pseudonyme = pseudonyme;
            this.ifJoin = true;
        }

        public ClientConn(Socket s) {
            this.s = s;
            (new Thread(this)).start();//fait appel à run()
        }

        public void setPort(int port) {
            this.port = port;
        }

        //reception d'infos
        @Override
        public void run() {
            try {//要初始化 在循环外加一次就行
                DataInputStream dis = new DataInputStream(s.getInputStream());
                String str = dis.readUTF();
                setPseudonyme(str);
                setPort(s.getPort());
                String strSend = pseudonyme + " a rejoint la conversation\n";//封装成函数
                Iterator<ClientConn> it = cclist.iterator();//parcourir tous les clients
                while (it.hasNext()) {
                    ClientConn o = it.next();
                    if (o.port != port)
                        o.send(strSend);

                    while (isStart) {//pour pouvoir recevoir chaque phrase
                        str = dis.readUTF();
                        System.out.println(s.getInetAddress() + "|" + s.getPort() + "parle:" + str + "\n");
                        serverTa.append(this.pseudonyme + " a parle:" + str + "\n");
                        strSend = pseudonyme + " a parle:" + str + "\n";
                        //parcourir ccList, faire appel à send()
                        it = cclist.iterator();//parcourir tous les clients
                        while (it.hasNext()) {
                            o = it.next();
                            if (o.ifJoin == true)
                                o.send(strSend);
                        }
//                    send(strSend);
                    }
                }
            } catch (EOFException j) {
                System.out.println(s.getInetAddress() + "|" + s.getPort() + "a quitté la chambre...\n");
                serverTa.append(s.getInetAddress() + "|" + s.getPort() + "a quitté la chambre...\n");
                String strSend = pseudonyme + " a quitté la chambre...\n";
                Iterator<ClientConn> it = cclist.iterator();//parcourir tous les clients
                while (it.hasNext()) {
                    ClientConn o = it.next();
                    o.send(strSend);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //renvoyer les msgs à chacun des interlocuteurs

        }

        public void send(String str) {
            try {
                DataOutputStream dos = new DataOutputStream(this.s.getOutputStream());
                dos.writeUTF(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ServerChat sc = new ServerChat();
    }
}
