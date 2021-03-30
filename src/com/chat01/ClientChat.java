package com.chat01;
import java.io.EOFException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientChat extends JFrame {
    private JTextArea ta=new JTextArea(10,20);
    private JScrollPane sp=new JScrollPane(ta);
    private JTextField tf=new JTextField(20);

    private static final String CONNSTR="127.0.0.1";
    private static final int CONNPORT=8888;
    private Socket s=null;
    private DataOutputStream dos=null;
    private boolean isConn=false;
    boolean isInitial=true;

    public void init(){
        this.setTitle("fenêtre de client");
        this.add(sp, BorderLayout.CENTER);
        this.add(tf, BorderLayout.SOUTH);
        ta.append("Entrer votre pesudo:\n");
        this.setBounds(400,400,450,400);
        tf.addActionListener(new ActionListener() {//associe des actions à la barre de saisie
            @Override
            public void actionPerformed(ActionEvent e) {
                String strSend=tf.getText();
                if(strSend.trim().length()==0) {//saisie de l'espace nul n'est pas possible!
                    return;
                }
                if (isInitial==true){
                    ta.append(tf.getText()+"\n");
                    ta.append(strSend+" a rejoint la conversation\n");
                    ta.append("----------------------------\n");
                }
                send(strSend);
                isInitial=false;
                tf.setText("");//nettoye la barre de saisie
            }
        });
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ta.setEditable(false);
        tf.requestFocus();
        this.setVisible(true);//met en place la fenêtre graphique

        try {
            s=new Socket(CONNSTR,CONNPORT);//lance le participant
            isConn=true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Receive()).start();//lancer la recption de msg consécutive
    }


    //    envoye le msgs au serveur
    public void send(String str){
        try {
            dos=new DataOutputStream(s.getOutputStream());
            dos.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //    reçoit les msgs de la part d'autres participants
    class Receive implements Runnable{
        @Override
        public void run() {
            try {
                while (isConn) {//intercepte msgs de chaque participant consécutivement
                DataInputStream dis = new DataInputStream(s.getInputStream());
                String str=dis.readUTF();
                ta.append(str);
            }
            } catch (EOFException m){//gére le cas d’une déconnexion de client sans que le client soit prévenu
                System.out.println("le serveur est coupé accidentalement\n");
                ta.append("le serveur est coupé accidentalement\n");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args) {
        ClientChat cc=new ClientChat();
        cc.init();//fait appel à clientchat

    }
}
