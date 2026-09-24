package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        // Modificato per accettare 3 parametri: IP, Porta Aggregator, Porta P2P
        if (args.length != 3) {
            System.err.println("Errore. Uso corretto: java Client.Client <Indirizzo_IP> <Porta_Aggregator> <Porta_P2P>");
            System.exit(1);
        }

        String ipAggregator = args[0];
        int portaAggregator = 0;
        int portaP2PLocale = 0;

        try {
            portaAggregator = Integer.parseInt(args[1]);
            portaP2PLocale = Integer.parseInt(args[2]); // Legge il terzo parametro
        } catch (NumberFormatException e) {
            System.err.println("Errore: Le porte devono essere numeri interi.");
            System.exit(1);
        }

        ArchivioLocale archivio = new ArchivioLocale();

        try (
            Socket socket = new Socket(ipAggregator, portaAggregator);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connessione stabilita con l'Aggregator a " + ipAggregator + ":" + portaAggregator);
            
            // Calcola il proprio indirizzo dinamico usando la porta passata da terminale
            String mioIndirizzoP2P = "127.0.0.1:" + portaP2PLocale;
            
            // Avvia il server P2P interno per accettare i download dagli altri nodi
            Thread listenerThread = new Thread(new PeerListener(portaP2PLocale, archivio));
            listenerThread.start();

            // Inizializza il gestore per il download robusto
            DownloadManager downloader = new DownloadManager(out, in, archivio, mioIndirizzoP2P);

            System.out.println("Nodo Sensore in ascolto su porta " + portaP2PLocale + ". Comandi disponibili:");
            System.out.println("- listdata local");
            System.out.println("- listdata remote");
            System.out.println("- add <nome> <contenuto>");
            System.out.println("- download <nome>");
            System.out.println("- quit");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                String[] token = input.split(" ", 3);
                String comando = token[0].toLowerCase();
                String sottoComando = token.length > 1 ? token[1].toLowerCase() : "";

                if (comando.equals("listdata") && sottoComando.equals("local")) {
                    List<String> locali = archivio.getListaNomiRilevazioni();
                    System.out.println("Risorse locali:");
                    if (locali.isEmpty()) System.out.println("Nessuna.");
                    else locali.forEach(System.out::println);

                } else if (comando.equals("listdata") && sottoComando.equals("remote")) {
                    out.println("GET_ALL");
                    String risposta;
                    while ((risposta = in.readLine()) != null && !risposta.equals("END_LIST")) {
                        System.out.println(risposta);
                    }

                } else if (comando.equals("add") && token.length >= 3) {
                    String nomeRisorsa = token[1];
                    String contenuto = token[2];
                    archivio.aggiungiRilevazione(nomeRisorsa, contenuto);
                    out.println("REGISTER " + nomeRisorsa + " " + mioIndirizzoP2P);
                    System.out.println("Rilevazione aggiunta: " + in.readLine());

                } else if (comando.equals("download") && token.length >= 2) {
                    String nomeRisorsa = token[1];
                    downloader.eseguiDownload(nomeRisorsa);

                } else if (comando.equals("quit")) {
                    System.out.println("Chiusura del Nodo Sensore...");
                    out.println("QUIT");
                    break; 

                } else {
                    System.out.println("Comando non riconosciuto.");
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Errore: Aggregatore non trovato.");
        } catch (IOException e) {
            System.err.println("Errore di connessione all'Aggregatore.");
        }
    }
}