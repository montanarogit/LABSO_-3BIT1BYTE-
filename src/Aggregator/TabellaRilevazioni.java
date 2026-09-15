package Aggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TabellaRilevazioni {
    // Mappa: Nome Rilevazione -> Lista di Nodi (es. "IP:Porta") che la possiedono
    private final Map<String, List<String>> mappaRilevazioni;
    
    // Lista che funge da registro per tenere traccia dei log dei download
    private final List<String> registroLogs;
    
    // Lock per garantire la mutua esclusione su mappa e registri
    private final Lock lock;

    public TabellaRilevazioni() {
        this.mappaRilevazioni = new HashMap<>();
        this.registroLogs = new ArrayList<>();
        // Il flag 'true' imposta il lock in modalità "fair" (equa)
        this.lock = new ReentrantLock(true); 
    }

    // Metodo per aggiornare le informazioni aggiungendo un nodo
    public void aggiungiRilevazione(String nomeRilevazione, String indirizzoNodo) {
        lock.lock();
        try {
            mappaRilevazioni.putIfAbsent(nomeRilevazione, new ArrayList<>());
            if (!mappaRilevazioni.get(nomeRilevazione).contains(indirizzoNodo)) {
                mappaRilevazioni.get(nomeRilevazione).add(indirizzoNodo);
            }
        } finally {
            lock.unlock(); 
        }
    }

    // Rimuove un nodo in caso di fallimento del download
    public void rimuoviNodoDaRilevazione(String nomeRilevazione, String indirizzoNodo) {
        lock.lock();
        try {
            if (mappaRilevazioni.containsKey(nomeRilevazione)) {
                mappaRilevazioni.get(nomeRilevazione).remove(indirizzoNodo);
                if (mappaRilevazioni.get(nomeRilevazione).isEmpty()) {
                    mappaRilevazioni.remove(nomeRilevazione);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Metodo per ottenere le informazioni dalla rete in modo sicuro
    public Map<String, List<String>> getCopiaRilevazioni() {
        lock.lock();
        try {
            // Restituiamo una copia profonda della mappa per evitare modifiche concorrenti
            Map<String, List<String>> copia = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : mappaRilevazioni.entrySet()) {
                copia.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return copia;
        } finally {
            lock.unlock();
        }
    }

    // --- GESTIONE DEI LOG ---

    // Restituisce la lista dei log alla console
    public List<String> getRegistroLog() {
        lock.lock();
        try {
            return new ArrayList<>(registroLogs);
        } finally {
            lock.unlock();
        }
    }

    // Registra un nuovo download in modo thread-safe
    public void aggiungiLog(String messaggio) {
        lock.lock();
        try {
            registroLogs.add(messaggio);
        } finally {
            lock.unlock();
        }
    }
}