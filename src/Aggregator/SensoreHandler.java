package Aggregator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class SensoreHandler implements Runnable {
    private final Socket clientSocket;
    private final TabellaRilevazioni tabella;

    public SensoreHandler(Socket clientSocket, TabellaRilevazioni tabella) {
        this.clientSocket = clientSocket;
        this.tabella = tabella;
    }

    @Override
    public void run() {
        // try-with-resources per chiudere automaticamente gli stream alla fine
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String richiesta;
            
            // Rimane in ascolto finché il client non chiude la connessione
            while ((richiesta = in.readLine()) != null) {
                String[] token = richiesta.split(" ");
                String comando = token[0].toUpperCase();

                switch (comando) {
                    case "REGISTER":
                        // Sintassi: REGISTER <NomeRilevazione> <IP:Porta>
                        if (token.length == 3) {
                            tabella.aggiungiRilevazione(token[1], token[2]);
                            out.println("OK Registrazione completata");
                        }
                        break;

                    case "GET_ALL":
                        // Risponde alla richiesta "listdata remote" del client
                        Map<String, List<String>> copiaTabella = tabella.getCopiaRilevazioni();
                        if (copiaTabella.isEmpty()) {
                            out.println("Nessuna rilevazione presente sulla rete.");
                        } else {
                            for (Map.Entry<String, List<String>> entry : copiaTabella.entrySet()) {
                                String nodi = String.join(", ", entry.getValue());
                                out.println(entry.getKey() + ": " + nodi);
                            }
                        }
                        // Segnale di fine trasmissione per il client
                        out.println("END_LIST");
                        break;

                    case "GET_NODE_FOR":
                        // Richiede un nodo da cui scaricare. Sintassi: GET_NODE_FOR <NomeRilevazione>
                        if (token.length == 2) {
                            String nomeRil = token[1];
                            Map<String, List<String>> statoAttuale = tabella.getCopiaRilevazioni();
                            
                            if (statoAttuale.containsKey(nomeRil) && !statoAttuale.get(nomeRil).isEmpty()) {
                                // Ritorna il primo nodo disponibile
                                out.println(statoAttuale.get(nomeRil).get(0));
                            } else {
                                // Come da specifiche: ritorna che la rilevazione non è disponibile
                                out.println("NOT_FOUND");
                            }
                        }
                        break;

                    case "REMOVE_NODE_FOR":
                        // Utilizzato dal protocollo di download robusto se un download fallisce
                        // Sintassi: REMOVE_NODE_FOR <NomeRilevazione> <IP:Porta>
                        if (token.length == 3) {
                            tabella.rimuoviNodoDaRilevazione(token[1], token[2]);
                            out.println("OK Rimozione completata");
                        }
                        break;

                    case "QUIT":
                        // Il client comunica che si sta disconnettendo
                        out.println("BYE");
                        return; // Esce dal ciclo e dal metodo run()
                        
                    default:
                        out.println("ERROR Comando sconosciuto");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore di comunicazione (nodo disconnesso inaspettatamente): " + e.getMessage());
        }
    }
}