package Aggregator;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AggregatorConsole implements Runnable {
    private final TabellaRilevazioni tabella;

    public AggregatorConsole(TabellaRilevazioni tabella) {
        this.tabella = tabella;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Console Aggregator attiva. Comandi: listdata, log, quit");

        while (true) {
            // Un semplice prompt per la console
            System.out.print("> ");
            
            // Attende l'input dell'utente. Questa è un'operazione bloccante, 
            // per questo motivo DEVE stare in un thread separato!
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String comando = input.toLowerCase();

            switch (comando) {
                case "listdata":
                    // Ritorna l'elenco di tutte le rilevazioni disponibili sulla rete
                    Map<String, List<String>> dati = tabella.getCopiaRilevazioni();
                    if (dati.isEmpty()) {
                        System.out.println("Nessuna rilevazione attualmente presente sulla rete.");
                    } else {
                        System.out.println("Risorse:");
                        for (Map.Entry<String, List<String>> entry : dati.entrySet()) {
                            System.out.println(entry.getKey() + ": " + String.join(", ", entry.getValue()));
                        }
                    }
                    break;

                case "log":
                    // Ritorna la lista di tutte le richieste di download, i nodi coinvolti e l'esito
                    System.out.println("Risorse scaricate:");
                    List<String> registro = tabella.getRegistroLog();
                    if (registro.isEmpty()) {
                        System.out.println("Nessun download registrato finora.");
                    } else {
                        for (String voce : registro) {
                            System.out.println(voce);
                        }
                    }
                    break;

                case "quit":
                    // Arresta l'aggregatore in modo definitivo
                    System.out.println("Chiusura dell'Aggregator in corso...");
                    scanner.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Comando non valido. Usa: listdata, log, quit");
                    break;
            }
        }
    }
}