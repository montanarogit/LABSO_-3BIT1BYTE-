package Aggregator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Aggregator {

    public static void main(String[] args) {
        // L'aggregatore accetta come unico parametro la porta su cui restare in ascolto
        if (args.length != 1) {
            System.err.println("Errore. Uso corretto: java Aggregator <porta>");
            System.exit(1);
        }

        int porta = 0;
        try {
            porta = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Errore: La porta deve essere un numero intero valido.");
            System.exit(1);
        }

        // Inizializziamo la risorsa condivisa (la tabella)
        TabellaRilevazioni tabella = new TabellaRilevazioni();

        // 1. Avvio del thread per la sessione interattiva (comandi: listdata, log, quit)
        // Questo garantisce che l'attesa dell'input utente non blocchi i socket
        Thread consoleThread = new Thread(new AggregatorConsole(tabella));
        consoleThread.start();

        // 2. Avvio del ServerSocket per la gestione delle connessioni di rete
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("Aggregator avviato e in ascolto sulla porta " + porta + "...");
            
            // Ciclo infinito per accettare le connessioni in entrata dai Client
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                // Per ogni nuovo client connesso, avviamo un thread dedicato
                // passando sia il socket che il riferimento alla risorsa condivisa
                Thread handlerThread = new Thread(new SensoreHandler(clientSocket, tabella));
                handlerThread.start();
            }
        } catch (IOException e) {
            System.err.println("Errore di rete nell'Aggregator: " + e.getMessage());
        }
    }
}