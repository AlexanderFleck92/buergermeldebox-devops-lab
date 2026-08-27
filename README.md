# 🏛️ Cloud-Transformation Lab: Bürger-Melde-Box

Dieses Repository dokumentiert ein praxisnahes DevOps- und Infrastruktur-Lab zur schrittweisen Modernisierung einer kommunalen Fachanwendung.

Im Mittelpunkt steht eine Spring-Boot-REST-API für Mängelmeldungen, die ausgehend von einer simulierten Legacy-Umgebung schrittweise containerisiert, automatisiert und perspektivisch auf eine orchestrierte Plattform überführt wird.

Das Projekt verfolgt dabei einen realistischen Migrationsansatz: Jede Phase baut auf der vorherigen auf und führt gezielt neue Automatisierungs-, Sicherheits- und Betriebsaspekte ein.

> **Status des Labs:** 🟡 Phase 2 – Ansible-Automatisierung auf einer Legacy-VM in Arbeit

---

## 🗺️ Projektfokus & Roadmap

Das Projekt beginnt bewusst mit einer einfachen, reproduzierbaren Container-Basis. In Phase 1 steht noch keine verteilte Hochverfügbarkeitsarchitektur im Mittelpunkt.

Die weiteren Phasen erweitern diese Grundlage schrittweise um Server-Automatisierung, Container-Orchestrierung, Health-Checks, Replikation, Monitoring sowie Ausfall- und Rollback-Tests.

* [x] **Phase 1:** Anwendungsbasis & Multi-Stage-Containerisierung
* [ ] **Phase 2:** Simulation einer Legacy-VM & Server-Automatisierung mit Ansible
* [ ] **Phase 3:** Kubernetes-Orchestrierung, Health-Checks & Replikation
* [ ] **Phase 4:** Chaos Engineering, Rollback-Tests & Monitoring mit Prometheus/Grafana

---

# 🏗️ Architektur – Phase 1

Phase 1 bildet die technische Grundlage für die späteren Migrationsschritte.

Die Anwendung läuft zunächst lokal auf einem Windows-11-Arbeitsplatz. Docker Desktop stellt dabei die Container-Laufzeit über das WSL2-Backend bereit.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           Windows 11 Host                               │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    Docker Desktop (WSL2 Backend)                  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────┐        ┌─────────────────────────┐   │  │
│  │  │ Container:              │        │ Container:              │   │  │
│  │  │ buergermeldebox-app     │        │ buergermeldebox-db      │   │  │
│  │  │                         │        │                         │   │  │
│  │  │ Spring Boot             │        │ PostgreSQL              │   │  │
│  │  │ Java 21                 │        │ Port 5432               │   │  │
│  │  │ REST API                │        │                         │   │  │
│  │  │ Actuator Health         │        │                         │   │  │
│  │  │                         │        │                         │   │  │
│  │  │ appuser (non-root)      │        │                         │   │  │
│  │  └───────────┬─────────────┘        └───────────┬─────────────┘   │  │
│  │              │                                  │                 │  │
│  │              │        Docker Network            │                 │  │
│  │              └──────────────────────────────────┘                 │  │
│  │                                                                   │  │
│  │                                      ┌─────────────────────────┐  │  │
│  │                                      │ postgres-data           │  │  │
│  │                                      │ Named Volume            │  │  │
│  │                                      └─────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                         │                                               │
└─────────────────────────┼───────────────────────────────────────────────┘
                          │
                       HTTP :8080
                          │
                          ▼
                 Browser / Postman / curl
```

### Architekturprinzipien

**Containerisierung:**
Die Anwendung wird als Docker-Image gebaut und unabhängig von der lokalen Java-Installation ausgeführt.

**Multi-Stage-Build:**
Der Build der Anwendung und die spätere Laufzeit werden voneinander getrennt. Maven und weitere Build-Werkzeuge verbleiben in der Build-Stage und werden nicht in das finale Runtime-Image übernommen.

**Least Privilege:**
Der Java-Prozess läuft innerhalb des Containers nicht als `root`, sondern unter dem dedizierten Benutzer `appuser` mit UID 999.

**Health Monitoring:**
Spring Boot Actuator stellt Health-Informationen zur Verfügung. Diese bilden die Grundlage für die später geplanten Kubernetes-Liveness- und Readiness-Probes.

**Lokale Datenhaltung:**
Die persistente Datenhaltung wird durch die PostgreSQL Datenbank ermöglicht.

---

# 📋 Anforderungen – Phase 1

## Funktionale Anforderungen

* Die Webanwendung muss über HTTP erreichbar sein.
* Die Anwendung muss lokal als Docker-Container ausgeführt werden können.
* Die Spring-Boot-Anwendung muss innerhalb des Containers auf Port `8080` lauschen.
* Der Health-Endpunkt muss über `/actuator/health` erreichbar sein.

## Technische Anforderungen

* **Java:** 21
* **Spring Boot:** 4.1.0
* **Build-System:** Maven
* **Containerisierung:** Docker
* **Build-Verfahren:** Multi-Stage Docker Build
* **Datenbank:** PostgreSQL
* **Monitoring/Health:** Spring Boot Actuator

Das finale Runtime-Image soll ausschließlich die für den Betrieb erforderliche Java-Laufzeit und die gebaute Anwendung enthalten.

---

# 🛡️ Sicherheitskonzept

Die Containerisierung folgt dem Least-Privilege-Prinzip.

Der Anwendungsprozess wird innerhalb des Containers nicht mit Root-Rechten ausgeführt. Stattdessen wird ein dedizierter Systembenutzer verwendet:

```text
User:  appuser
UID:   999
Group: appgroup
GID:   999
```

Darüber hinaus gelten für Phase 1 folgende Sicherheitsanforderungen:

* Keine Passwörter oder Zugangsdaten im Dockerfile
* Keine Secrets im Quellcode
* Kein unnötiger Einsatz von Build-Werkzeugen im finalen Image
* Kein Betrieb der Anwendung als `root`
* Trennung von Build- und Runtime-Umgebung

> **Hinweis:** Der Non-Root-Ansatz bezieht sich auf die Ausführung des Anwendungsprozesses innerhalb des Containers. Er ist ein Bestandteil der Container-Sicherheitsstrategie und ersetzt keine weiteren Host- bzw. Docker-Sicherheitsmaßnahmen.

---

# 🚀 Installation und Ausführung

## Voraussetzungen

Für die lokale Ausführung werden benötigt:

* Windows 11
* Docker Desktop
* aktiviertes WSL2-Backend
* Git Bash
* Git

Die Java- und Maven-Installation auf dem Host ist für den Container-Build nicht erforderlich, sofern der Maven-Build vollständig innerhalb der Build-Stage des Dockerfiles durchgeführt wird.

---

## 1. In den Anwendungsordner wechseln

```bash
cd app
```

---

## 2. Anwendung und Datenbank mit Docker Compose starten

Das Backend sowie die PostgreSQL-Datenbank werden gemeinsam mittels Docker Compose hochgefahren. Der Parameter `-d` sorgt dafür, dass die Container im Hintergrund laufen und dein Terminal frei bleibt.

```bash
docker compose up --build -d
```

**Was passiert hier im Hintergrund?**
* **`--build`:** Baut das Dockerfile deiner App frisch (ersetzt das manuelle `docker build`). Das Image nutzt im Hintergrund weiterhin den sicheren Multi-Stage-Build.
* **Healthcheck & Abhängigkeit:** Docker Compose wartet automatisch, bis die PostgreSQL-Datenbank bereit ist (`healthy`), und startet erst dann die Spring-Boot-Anwendung.

Die Portweiterleitung verbindet deinen Computer mit dem App-Container:
```text
Host:8080  →  Container:8080
```


---

## 🔍 Verifikation

### 1. Laufende Container überprüfen
```bash
docker ps
```
Die Container **`buergermeldebox-app`** und **`buergermeldebox-db`** müssen als laufend angezeigt werden. Die Datenbank benötigt in der Spalte `STATUS` zudem das Attribut **`(healthy)`**.

### 2. Health-Endpunkt überprüfen
Im Browser, in Postman oder über Git Bash:

```bash
curl http://localhost:8080/actuator/health
```

Erwartet wird ein HTTP-Status **200 OK** sowie ein detaillierter Health-Status **UP**.

**Beispiel für die JSON-Antwort:**
```json
{
  "components": {
    "db": {
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      },
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "livenessState": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  },
  "status": "UP"
}
```

**Auswertung:**
Der Health-Endpunkt bestätigt, dass die Anwendung betriebsbereit ist. Insbesondere wird die neue **PostgreSQL-Datenbankverbindung erfolgreich als `UP`** gemeldet. Die vorhandenen Liveness- und Readiness-Informationen können in Phase 3 als Grundlage für Kubernetes-Probes verwendet werden.

### 👤 Testprotokoll: Non-Root-Ausführung
Mit folgendem Befehl wird die Benutzeridentität innerhalb des laufenden App-Containers überprüft:

```bash
docker compose exec app id
```
*Erwartetes Ergebnis:*
```text
uid=999(appuser) gid=999(appgroup) groups=999(appgroup)
```

**Auswertung:**
Die UID 999 ist ungleich 0. Der Java-Prozess läuft innerhalb des Containers somit sicherheitskonform nicht als root.

Optional kann der Benutzername zusätzlich überprüft werden:
```bash
docker compose exec app whoami
```
*Erwartetes Ergebnis:* `appuser`

---

## 🧹 Container stoppen

Um die gesamte Umgebung (Anwendung und Datenbank) sauber anzuhalten, nutze folgenden Befehl. Die gemeldeten Daten in der Datenbank bleiben dank des konfigurierten Docker-Volumes (`postgres-data`) auf der Festplatte erhalten:

```bash
docker compose down
```

---

## 🧪 Reproduzierbarer Testablauf

Ein vollständiger, automatisierter Durchlauf von der Code-Änderung bis zur Verifikation in einem Rutsch:

```bash
# 1. In den Ordner wechseln
cd app

# 2. Bestehende Container stoppen, frisch bauen und im Hintergrund starten
docker compose down
docker compose up --build -d

# 3. Status und Non-Root-User prüfen
docker ps
docker compose exec app id

# 4. API-Health abfragen
curl http://localhost:8080/actuator/health
```

---

# 📊 Aktueller Stand

| Bereich                  | Status     |
| ------------------------ | ---------- |
| Spring-Boot-Anwendung    | ✅          |
| Java 21                  | ✅          |
| Maven Build              | ✅          |
| Multi-Stage Docker Build | ✅          |
| Docker-Ausführung        | ✅          |
| Port 8080                | ✅          |
| Spring Boot Actuator     | ✅          |
| H2 Health Check          | ✅          |
| Liveness State           | ✅          |
| Readiness State          | ✅          |
| Non-Root User            | ✅          |
| Ansible-Automatisierung  | 🚧 Phase 2 |
| Kubernetes               | ⏳ Phase 3  |
| Replikation              | ⏳ Phase 3  |
| Prometheus/Grafana       | ⏳ Phase 4  |
| Chaos Engineering        | ⏳ Phase 4  |

---

# 🛣️ Nächste Schritte

## Phase 2 – Legacy-VM & Ansible

Die nächste Phase simuliert eine klassische Legacy-Serverumgebung.

Geplant sind:

* Bereitstellung einer Ubuntu-VM
* Installation und Konfiguration der benötigten Laufzeitumgebung
* Automatisierung mit Ansible
* reproduzierbare Serverkonfiguration
* Trennung von Konfiguration und manueller Administration
* automatisiertes Deployment der Anwendung

Ziel ist es, den Übergang von einer manuell administrierten Umgebung zu einer reproduzierbaren Infrastructure-as-Code-/Configuration-as-Code-Arbeitsweise zu demonstrieren.

## Phase 3 – Kubernetes

Anschließend wird die Container-Anwendung auf eine Kubernetes-basierte Umgebung übertragen.

Geplant sind:

* Kubernetes Deployment
* Services
* Liveness-Probes
* Readiness-Probes
* mehrere Replikate
* Rolling Updates
* grundlegende Ausfallsimulation

## Phase 4 – Resilienz & Monitoring

In der letzten Phase wird der Betrieb stärker in Richtung produktionsnaher Cloud-Architektur erweitert.

Geplant sind:

* Prometheus
* Grafana
* Metriken und Dashboards
* Container-/Pod-Ausfälle
* Rollback-Tests
* Chaos-Engineering-Szenarien
* Dokumentation der beobachteten Ausfall- und Wiederherstellungsverfahren

---

# ⚠️ Grenzen des lokalen Labs

Dieses Projekt dient ausschließlich Trainings-, Demonstrations- und Bewerbungszwecken.

Die aktuelle Umgebung simuliert Cloud- und DevOps-Konzepte lokal auf einem einzelnen Arbeitsplatz:

```text
Windows 11
   │
   └── Docker Desktop
          │
          └── WSL2
                 │
                 └── Docker Container
```

Die Lösung stellt **keine produktive Multi-Cloud-, Multi-Region- oder geo-redundante Hochverfügbarkeitsarchitektur** dar.

Insbesondere sind in Phase 1 noch keine folgenden Komponenten vorhanden:

* Kubernetes
* mehrere Application Replicas
* externe hochverfügbare Datenbank
* Load Balancer
* Multi-Node-Cluster
* Multi-Region-Betrieb
* zentrale Observability-Plattform
* automatisierte Disaster-Recovery-Prozesse

Diese Aspekte werden bewusst erst in den folgenden Phasen betrachtet.

---

# 🎯 Ziel des Labs

Das Projekt soll nicht lediglich zeigen, wie eine Spring-Boot-Anwendung in einem Docker-Container ausgeführt wird.

Vielmehr soll der schrittweise Weg von einer klassischen Legacy-Anwendung hin zu einer automatisierten und containerisierten Betriebsplattform nachvollziehbar dargestellt werden:

```text
Legacy-Anwendung
       │
       ▼
   Container
       │
       ▼
 Ansible-Automatisierung
       │
       ▼
    Kubernetes
       │
       ▼
 Replikation & Health Checks
       │
       ▼
Monitoring & Resilienz
       │
       ▼
Produktionsnahe Plattform
```

Jede Phase erweitert dabei die vorherige um einen konkreten technischen Aspekt von DevOps, Containerisierung, Infrastructure as Code, Orchestrierung oder Resilienz.

---
