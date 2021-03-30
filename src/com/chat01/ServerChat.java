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
import java.util.ArrayList;
import java.util.Iterator;

public class ServerChat extends JFrame {
    private static final int PORT = 8888;//crée la fenêtre graphique
    JTextArea serverTa = new JTextArea();
    private JPanel btnTool = new JPanel();
    private JButton startBtn = new JButton("start");
    private JButton stopBtn = new JButton("stop");
    private JScrollPane sp = new JScrollPane(serverTa);

    private ServerSocket ss = null;
    private Socket s = null;
    private ArrayList<ClientConn> cclist = new ArrayList<ClientConn>();//pour pourvoir acceuillir plusieurs clients
    private boolean isStart = false;

    public ServerChat() {
        this.setTitle("fenêtre de serveur");
        this.add(sp, BorderLayout.CENTER);
        btnTool.add(startBtn);
        btnTool.add(stopBtn);
        this.add(btnTool, BorderLayout.SOUTH);
        this.setBounds(0, 0, 650, 500);

        if (isStart) {//renseigne sur le status de serveur
            serverTa.append("le serveur est allumé!\n");
        } else {
            serverTa.append("le serveur n'est pas encore activé!\nVeuillez appuyer sur le bouton start!\n");
        }

        this.addWindowListener(new WindowAdapter() {//associe les actions à la fermeture de fenêtre
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

        stopBtn.addActionListener(new ActionListener() {//associe les actions au bouton"stop",on quitte aussi(même opération comme dessus)
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

        startBtn.addActionListener(new ActionListener() {//associe les actions au bouton "start", on crée le serversocket,et on lance
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
        this.setVisible(true);//met en place la fenêtre graphique

        try {
            startServer();//on lance le serveur à la fin de création de fenêtre
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isStart) {//renseigne sur le status de serveur à nouveau
            serverTa.append("le serveur est allumé!\n");
        } else {
            serverTa.append("le serveur n'est pas encore activé!\nVeuillez appuyer sur le bouton start!\n");
        }

    }

    //  lance le serveur
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

    //    définit une classe de client connection pour y encapsuler des infos et des méthodes
    class ClientConn implements Runnable {
        Socket s = null;
        String pseudonyme = null;//encapsulation
        int port;
        boolean ifJoin = false;

        public ClientConn(Socket s) {
            this.s = s;
            (new Thread(this)).start();//fait appel à run()
        }

        public void setPseudonyme(String pseudonyme) {
            this.pseudonyme = pseudonyme;
            this.ifJoin = true;
        }

        public void setPort(int port) {
            this.port = port;
        }

        //reçoit d'infos et les renvoye à chacun des clients
        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(s.getInputStream());//initie et configue la conversation
                String str = dis.readUTF();
                setPseudonyme(str);
                setPort(s.getPort());
                String strSend = pseudonyme + " a rejoint la conversation\n";
                Iterator<ClientConn> it = cclist.iterator();//parcourit tous les clients
                while (it.hasNext()) {
                    ClientConn o = it.next();
                    if (o.port != port)//renvoye les msgs aux autres sauf soi-même
                        o.send(strSend);

                    while (isStart) {//pour pouvoir recevoir chaque phrase
                        str = dis.readUTF();
                        System.out.println(s.getInetAddress() + "|" + s.getPort() + "parle:" + str + "\n");
                        serverTa.append(this.pseudonyme + " a parle:" + str + "\n");
                        strSend = pseudonyme + " a parle:" + str + "\n";
                        //parcourir ccList, faire appel à send()
                        it = cclist.iterator();//parcourit tous les clients
                        while (it.hasNext()) {
                            o = it.next();
                            if (o.ifJoin == true)
                                o.send(strSend);
                        }
                    }
                }
            } catch (EOFException j) {//gére le cas d’une déconnexion de client sans que le serveur soit prévenu
                System.out.println(s.getInetAddress() + "|" + s.getPort() + "a quitté la chambre...\n");
                serverTa.append(s.getInetAddress() + "|" + s.getPort() + "a quitté la chambre...\n");
                String strSend = pseudonyme + " a quitté la chambre...\n";
                Iterator<ClientConn> it = cclist.iterator();//parcourit tous les clients
                while (it.hasNext()) {
                    ClientConn o = it.next();
                    o.send(strSend);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //    envoye le msgs au client
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
