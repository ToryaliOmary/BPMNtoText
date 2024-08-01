# BPMNtoText
Diese Anwednung dient der Überstzung von eingegebenen BPMN-Diagrammen in einen Fließtext. 

# Anwendung einrichten 
1. Repo lokal clonen und in IDE nach Wahl öffnen
2. notwendige packages und libraries installieren
3. zum initialisieren der Anwendung folgenden Befehl im Terminal ausführen:  **mvn clean install**

# Anwendung verwenden
1. Aeneis BPMN-Export in den on den Ordner **ressources** einfügen
2. Dateipfad in Klasse **BPMNLoader** aktualisieren
3. zum Starten der Anwendung folgenden Befehl im Terminal ausführen: **mvn exec:java -D"exec.mainClass=com.example.App"**
4. erstellte Prozessbeschreibung wird automatsich erstellt

# Fehlerbehandlung 
- sämtliche Methoden und Funktionen sind mit einer Fehlermeldung ausgestattet (bessere Nachverfolgung der Fehler)
- Klasse DebugUtil dient der erweiterten Fehleranalyse
