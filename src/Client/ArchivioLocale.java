package Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArchivioLocale {
    // Mappa che associa il Nome della Rilevazione al suo Contenuto (es. R1 -> "22.5 C")
    private final Map<String, String> rilevazioni;
    private final Lock lock;

    public ArchivioLocale() {
        this.rilevazioni = new HashMap<>();
        this.lock = new ReentrantLock(true);
    }

    // Usato dal comando 'add <nome risorsa> <contenuto>'
    public void aggiungiRilevazione(String nome, String contenuto) {
        lock.lock();
        try {
            rilevazioni.put(nome, contenuto);
        } finally {
            lock.unlock();
        }
    }

    // Ritorna la lista dei nomi, usato dal comando 'listdata local'
    public List<String> getListaNomiRilevazioni() {
        lock.lock();
        try {
            return new ArrayList<>(rilevazioni.keySet());
        } finally {
            lock.unlock();
        }
    }

    // Verifica se il nodo possiede una certa rilevazione
    public boolean possiedeRilevazione(String nome) {
        lock.lock();
        try {
            return rilevazioni.containsKey(nome);
        } finally {
            lock.unlock();
        }
    }

    // Usato quando un altro nodo (peer) richiede di scaricare questa rilevazione
    public String getContenuto(String nome) {
        lock.lock();
        try {
            return rilevazioni.getOrDefault(nome, null);
        } finally {
            lock.unlock();
        }
    }
}