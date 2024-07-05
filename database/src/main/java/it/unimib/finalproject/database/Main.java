package it.unimib.finalproject.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe principale in cui parte il database.
 */
public class Main {
    //porta di ascolto del database
    public static final int PORT = 3030;
    //il file DB.json contiene un database di partenza
    private static final String FILE_DB = "DB.json";
    // oggetto condiviso contentente i dati del DB
    private static Database DB;

    /**
     * Avvia il database e l'ascolto di nuove connessioni.
     *
     * @return Un server HTTP Grizzly.
     */
    public static void startServer() {
        try {
            var server = new ServerSocket(PORT);
            DB = new Database(FILE_DB);
            System.out.println("Database listening at localhost:" + PORT);
            while (true)
                new Handler(server.accept(), DB).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }    

    private static class Handler extends Thread {
        private Socket client;
        private Database DB;
        
        public Handler(Socket client, Database DB) {
            this.client = client;
            this.DB = DB;
        }

        public void run() {
            try {
                System.out.println("New Thread...");

                //inizializzazione degli stream di I/O della socket
                var outputStream = new PrintWriter(client.getOutputStream(), true);
                var inputStream = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String inputLine;
                //La connessione con il client rimane aperta fino a quando non viene ricevuto "+CLOSE"
                while ((inputLine = inputStream.readLine()) != null) {
                    //se ricevo +CLOSE, chiudo la connessione
                    if ("+CLOSE".equals(inputLine)) {
                        break;
                    }
                          
                    //divido l'input line
                    //requestType, secondo il protocollo, può essere +GET/+DEL/+PUT/+ADD/+NID
                    String requestType = inputLine.substring(0, 4);
                    //dato che tutti i comandi hanno la stessa lunghezza, viene estratta la chiave in forma key:id (dal carattere 5 in poi, evitando il requestType)
                    String key = inputLine.substring(5);
                    String param = "";
                    switch(requestType){
                        case "+GET":
                            outputStream.println(DB.getRequest(key));
                            break;
                        case "+PUT":
                            //viene letto il parametro passato come input
                            while (!(inputLine = inputStream.readLine()).equals("+END")) {
                                param += inputLine;
                            }
                            outputStream.println(DB.putRequest(key, param));
                            break;
                        case "+ADD":
                            //viene letto il parametro passato come input
                            while (!(inputLine = inputStream.readLine()).equals("+END")) {
                                param += inputLine;
                            }
                            outputStream.println(DB.addRequest(key, param));
                            break;
                        case "+DEL":
                            outputStream.println(DB.delRequest(key));
                            break;
                        case "+NID":
                            outputStream.println(DB.getNewId(key));
                            break;
                        default:
                            outputStream.println("+ERR Unknown command");
                    }
                    //in ogni caso, viene inviato +END per far capire che il payload è terminato
                    outputStream.println("+END");
                }
                System.out.println("Thread closed");
                inputStream.close();
                outputStream.close();
                client.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    
    /**
     * Metodo principale di avvio del database.
     *
     * @param args argomenti passati a riga di comando.
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        startServer();
    }

}