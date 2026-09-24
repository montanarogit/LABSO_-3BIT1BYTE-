package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DownloadManager {
    private final PrintWriter outAggregator;
    private final BufferedReader inAggregator;
    private final ArchivioLocale archivio;
    private final String mioIndirizzoP2P;

    public DownloadManager(PrintWriter outAggregator, BufferedReader inAggregator, 
                           ArchivioLocale archivio, String mioIndirizzoP2P) {
        this.outAggregator = outAggregator;
        this.inAggregator = inAggregator;
        this.archivio = archivio;
        this.mioIndirizzoP2P = mioIndirizzoP2P;
    }

    public void eseguiDownload(String nomeRisorsa) throws IOException {
        // Controllo preliminare: non scarichiamo se lo abbiamo già
        if (archivio.possiedeRilevazione(nomeRisorsa)) {
            System.out.println("Possiedi già la rilevazione in locale.");
            return;
        }

        boolean completato = false;

        // Ciclo while che implementa il protocollo robusto di download
        while (!completato) {
            // 1. Chiediamo all'aggregatore a chi connetterci
            outAggregator.println("GET_NODE_FOR " + nomeRisorsa);
            String rispostaAggregator = inAggregator.readLine();

            if (rispostaAggregator == null || rispostaAggregator.equals("NOT_FOUND")) {
                // L'aggregatore non ha altri nodi da proporre
                System.out.println("Download fallito: La rilevazione non è disponibile sulla rete.");
                break;
            }

            // Controllo difensivo per evitare ArrayIndexOutOfBoundsException
            String[] parts = rispostaAggregator.split(":");
            if (parts.length < 2) {
                System.out.println("Download fallito: La rilevazione non è disponibile sulla rete.");
                break;
            }

            String ipPeer = parts[0];
            int portaPeer;
            try {
                portaPeer = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("Errore nel formato dell'indirizzo del peer ricevuto dall'aggregatore.");
                break;
            }

            System.out.println("Tentativo di download da " + rispostaAggregator + "...");

            // 2. Proviamo a connetterci al peer suggerito
            try (
                Socket peerSocket = new Socket(ipPeer, portaPeer);
                PrintWriter outPeer = new PrintWriter(peerSocket.getOutputStream(), true);
                BufferedReader inPeer = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()))
            ) {
                outPeer.println("DOWNLOAD " + nomeRisorsa);
                String esito = inPeer.readLine();

                if ("OK".equals(esito)) {
                    // 3. Successo! Leggiamo il contenuto e lo salviamo in archivio
                    String contenuto = inPeer.readLine();
                    archivio.aggiungiRilevazione(nomeRisorsa, contenuto);
                    completato = true;
                    
                    System.out.println("Download completato con successo!");
                    
                    // Notifichiamo l'aggregatore che ora anche noi possediamo la risorsa
                    outAggregator.println("REGISTER " + nomeRisorsa + " " + mioIndirizzoP2P);
                    inAggregator.readLine(); // Consuma l'OK dell'aggregatore
                    
                    // Notifichiamo l'aggregatore per aggiornare il file di log 
                    outAggregator.println("LOG_DOWNLOAD " + nomeRisorsa + " " + rispostaAggregator + " " + mioIndirizzoP2P);
                    inAggregator.readLine(); // Consuma l'OK dell'aggregatore
                    
                } else {
                    // Il peer ha risposto ERROR_NOT_FOUND
                    System.out.println("Il nodo contattato non possiede più la risorsa.");
                    notificaFallimento(nomeRisorsa, rispostaAggregator);
                }

            } catch (IOException e) {
                // Il peer è offline o irraggiungibile
                System.out.println("Errore di rete con il peer " + rispostaAggregator + ".");
                notificaFallimento(nomeRisorsa, rispostaAggregator);
            }
        }
    }

    // Metodo helper per dire all'aggregatore di eliminare l'entry errata
    private void notificaFallimento(String nomeRisorsa, String peerFallito) {
        outAggregator.println("REMOVE_NODE_FOR " + nomeRisorsa + " " + peerFallito);
        try {
            inAggregator.readLine(); // <-- INSERIRE QUI
        } catch (IOException e) {
            System.err.println("Errore di lettura durante la notifica di fallimento: " + e.getMessage());
        }
        System.out.println("Segnalazione inviata all'aggregatore. Ritento...");
    }
}