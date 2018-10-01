package com.smile.socketserverdemo;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    // private properties
    private static final int SERVER_PORT = 6000;
    private ServerSocket serverSocket;

    private Handler updateConversationHandler;
    private ServerThread serverThread = null;
    private CommunicationThread communicationThread = null;

    private TextView messageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageTextView = findViewById(R.id.messageTextView);
        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        updateConversationHandler = new Handler();
        serverThread = new ServerThread();
        serverThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (serverThread != null) {
            serverThread.setKeepRunning(false);
            try {
                serverThread.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (communicationThread != null) {
            communicationThread.setKeepRunning(false);
            try {
                communicationThread.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /***************************************************/
    private class ServerThread extends Thread {

        private boolean keepRunning = true;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            Socket socket = null;
            if (serverSocket != null) {
                while (keepRunning) {
                    try {
                        socket = serverSocket.accept();
                        communicationThread = new CommunicationThread(socket);
                        communicationThread.start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        public void setKeepRunning(boolean keepRunning) {
            this.keepRunning = keepRunning;
        }
    }

    private class CommunicationThread extends Thread {

        private boolean keepRunning = true;
        private Socket clientSocket;
        private BufferedReader bufferedReader = null;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(this.clientSocket.getInputStream());
                this.bufferedReader = new BufferedReader(inputStreamReader);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            if ( (bufferedReader != null) && (clientSocket != null ) ) {
                while (keepRunning) {
                    try {

                        String msg = bufferedReader.readLine();
                        updateConversationHandler.post(new UpdateUiRunning(msg));

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        public void setKeepRunning(boolean keepRunning) {
            this.keepRunning = keepRunning;
        }
    }

    private class UpdateUiRunning implements Runnable {

        private String msg;

        public UpdateUiRunning(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            messageTextView.setText("To " + messageTextView.getText().toString() + "\nClient says: " + msg + "\n");
        }
    }
}
