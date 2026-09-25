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
        
        //qui modifica
        boolean giaPosseduta = archivio.possiedeRilevazione(nomeRisorsa);
        if (giaPosseduta) {
            System.out.println("Nota: Possiedi già la rilevazione '" + nomeRisorsa + "' in locale. Verrà sovrascritta al termine del download."); //permette di sovrascrivere risorse
        }

        boolean completato = false;

        
        while (!completato) {
            
            outAggregator.println("GET_NODE_FOR " + nomeRisorsa);
            String rispostaAggregator = inAggregator.readLine();

            if (rispostaAggregator == null || rispostaAggregator.equals("NOT_FOUND")) {
                
                System.out.println("Download fallito: La rilevazione non è disponibile sulla rete.");
                break;
            }

            
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

            
            try (
                Socket peerSocket = new Socket(ipPeer, portaPeer);
                PrintWriter outPeer = new PrintWriter(peerSocket.getOutputStream(), true);
                BufferedReader inPeer = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()))
            ) {
                outPeer.println("DOWNLOAD " + nomeRisorsa);
                String esito = inPeer.readLine();

                if ("OK".equals(esito)) {
                    
                    String contenuto = inPeer.readLine();

                    //modifica
                    System.out.println("Valore appena scaricato da sovrascrivere: [" + contenuto + "]"); //dice con quale risorsa viene sovrascritta

                    archivio.aggiungiRilevazione(nomeRisorsa, contenuto);
                    completato = true;
                    
                    System.out.println("Download completato con successo!");
                    
                   
                    outAggregator.println("REGISTER " + nomeRisorsa + " " + mioIndirizzoP2P);
                    inAggregator.readLine(); 
                    
                    // Notifichiamo l'aggregatore per aggiornare il file di log 
                    outAggregator.println("LOG_DOWNLOAD " + nomeRisorsa + " " + rispostaAggregator + " " + mioIndirizzoP2P);

                    //modifica
                    inAggregator.readLine();  //consuma i messaggi arretrati dell'aggregatore

                } else {
                    
                    System.out.println("Il nodo contattato non possiede più la risorsa.");
                    notificaFallimento(nomeRisorsa, rispostaAggregator);
                }

            } catch (IOException e) {
                
                System.out.println("Errore di rete con il peer " + rispostaAggregator + ".");
                notificaFallimento(nomeRisorsa, rispostaAggregator);
            }
        }
    }

    
    private void notificaFallimento(String nomeRisorsa, String peerFallito) {
        outAggregator.println("REMOVE_NODE_FOR " + nomeRisorsa + " " + peerFallito);
        System.out.println("Segnalazione inviata all'aggregatore. Ritento...");
    }
}