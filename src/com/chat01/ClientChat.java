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
    private JScrollPane sp=new JScrollPane(ta);//glissant rouleau
    private JTextField tf=new JTextField(20);
    private static final String CONNSTR="127.0.0.1";
    private static final int CONNPORT=8888;
    private Socket s=null;
    private DataOutputStream dos=null;
    private boolean isConn=false;
    boolean isInitial=true;
//public ClientChat() throws HeadlessException{
//    super();
//}

    public void init(){
        this.setTitle("fenêtre de client");
        this.add(sp, BorderLayout.CENTER);
        this.add(tf, BorderLayout.SOUTH);
        ta.append("Entrer votre pesudo:\n");
        this.setBounds(400,400,450,400);
        tf.addActionListener(new ActionListener() {//la barre de saisie
            @Override
            public void actionPerformed(ActionEvent e) {
                String strSend=tf.getText();
                if(strSend.trim().length()==0) {//saisie de l'espace n'est pas possible!
                    return;
                }
                if (isInitial==true){
                    ta.append(tf.getText()+"\n");//第一次是昵称，要发给对方知道，但对方不能显示
                    ta.append(strSend+" a rejoint la conversation\n");
                    ta.append("----------------------------\n");
                }//没输入名字前不能接消息
//                ta.append(strSend+"\n");//多行显示，自己不显示昵称？？
                send(strSend);
                isInitial=false;
                tf.setText("");//nettoye la barre de saisie
            }
        });

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ta.setEditable(false);
        tf.requestFocus();//光标聚焦focalisation de souris

        try {
            s=new Socket(CONNSTR,CONNPORT);//lance le client
            isConn=true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setVisible(true);

        new Thread(new Receive()).start();//lancer le multiple一直收很多人的信息
    }
//    envoyer
    public void send(String str){
        try {
            dos=new DataOutputStream(s.getOutputStream());
            dos.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //multiple recevoir
    class Receive implements Runnable{
        @Override
        public void run() {

            try {
                while (isConn) {//continue à intercepter plusieurs messages de chaque client
                DataInputStream dis = new DataInputStream(s.getInputStream());
                String str=dis.readUTF();
                ta.append(str);//显示到窗口
            }
            } catch (EOFException m){
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
        cc.init();//main里调用

    }
}
