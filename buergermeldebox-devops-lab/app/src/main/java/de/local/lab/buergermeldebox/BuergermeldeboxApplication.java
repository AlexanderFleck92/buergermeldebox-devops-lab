package de.local.lab.buergermeldebox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.*;
import java.util.List;

@SpringBootApplication
public class BuergermeldeboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuergermeldeboxApplication.class, args);
    }
}

// Das Datenmodell für eine Bürgermeldung
@Entity
class MaengelMeldung {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titel;
    private String beschreibung;
    private String status = "OFFEN";

    // Getter und Setter
    public Long getId() { return id; }
    public String getTitel() { return titel; }
    public void setTitel(String t) { this.titel = t; }
    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String b) { this.beschreibung = b; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}

interface MeldungRepository extends org.springframework.data.jpa.repository.JpaRepository<MaengelMeldung, Long> {}

// Die REST-Schnittstelle
@RestController
@RequestMapping("/api/meldungen")
class MeldeController {
    private final MeldungRepository repo;
    public MeldeController(MeldungRepository repo) { this.repo = repo; }

    @GetMapping
    public List<MaengelMeldung> getAll() { return repo.findAll(); }

    @PostMapping
    public MaengelMeldung create(@RequestBody MaengelMeldung meldung) { return repo.save(meldung); }
}
