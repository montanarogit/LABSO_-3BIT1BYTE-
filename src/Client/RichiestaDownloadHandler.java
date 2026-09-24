package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

public class RichiestaDownloadHandler implements Runnable {
    private final Socket peerSocket;
    private final ArchivioLocale archivio;
    private final Lock downloadLock;

    public RichiestaDownloadHandler(Socket peerSocket, ArchivioLocale archivio, Lock downloadLock) {
        this.peerSocket = peerSocket;
        this.archivio = archivio;
        this.downloadLock = downloadLock;
    }

    @Override
    public void run() {
        // ACQUISIZIONE DEL LOCK: Soddisfa le specifiche. 
        // Se un altro thread (peer) sta già scaricando, l'esecuzione si sospende qui.
        // La richiesta rimane in attesa senza generare errori o crash.
        downloadLock.lock();
        
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
            PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true)
        ) {
            String richiesta = in.readLine();
            
            if (richiesta != null && richiesta.startsWith("DOWNLOAD ")) {
                // Estrae il nome della rilevazione (es: "DOWNLOAD R1" -> "R1")
                String nomeRisorsa = richiesta.substring(9).trim();
                
                // Verifica in mutua esclusione (gestita dentro ArchivioLocale) se possiede il dato
                if (archivio.possiedeRilevazione(nomeRisorsa)) {
                    out.println("OK");
                    out.println(archivio.getContenuto(nomeRisorsa));
                } else {
                    out.println("ERROR_NOT_FOUND");
                }
            }
        } catch (IOException e) {
            System.err.println("Errore durante l'invio della risorsa al peer: " + e.getMessage());
        } finally {
            // RILASCIO DEL LOCK: Finito il trasferimento (o in caso di errore di rete), 
            // si sblocca la coda per il prossimo peer in attesa.
            downloadLock.unlock();
        }
    }
}