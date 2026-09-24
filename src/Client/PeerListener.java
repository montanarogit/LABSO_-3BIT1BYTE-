package Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PeerListener implements Runnable {
    private final int portaLocale;
    private final ArchivioLocale archivio;
    // Questo lock condiviso è la chiave per la mutua esclusione sui download
    private final Lock downloadLock; 

    public PeerListener(int portaLocale, ArchivioLocale archivio) {
        this.portaLocale = portaLocale;
        this.archivio = archivio;
        // Il flag 'true' (fairness) garantisce che le richieste vengano 
        // servite nell'ordine esatto in cui si sono accodate (FIFO).
        this.downloadLock = new ReentrantLock(true);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(portaLocale)) {
            // Ciclo infinito per accettare le richieste P2P
            while (true) {
                Socket peerSocket = serverSocket.accept();
                
                // Deleghiamo la comunicazione al worker, passandogli l'archivio E il lock
                Thread handler = new Thread(new RichiestaDownloadHandler(peerSocket, archivio, downloadLock));
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel listener P2P: " + e.getMessage());
        }
    }
}