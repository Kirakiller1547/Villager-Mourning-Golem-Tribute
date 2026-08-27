# Villager Mourning – Fabric Mod (Minecraft 1.21.4)

Wenn irgendwo in der Welt ein **Iron Golem stirbt** (egal ob durch den Spieler, einen
anderen Mob, Fallschaden usw.) und **Villager in der Nähe** sind (Standard: 32 Blöcke
Radius, also praktisch "irgendein Dorf"):

1. An der Sterbestelle entsteht ein **Grabstein**: ein Steinblock mit einem Schild
   darauf, auf dem "IRON GOLEM" steht.
2. Alle Villager in der Nähe laufen zu diesem Grabstein (auch gegen die normale
   Dorf-KI, die wird jede Sekunde überschrieben).
3. Sobald alle da sind (oder spätestens nach 15 Sekunden) wird ein Trauer-Sound
   abgespielt.

## Wichtiger Hinweis zum Song

Der Song "Kamin" von EMIN & JONY kann hier nicht als Audiodatei mitgeliefert werden,
da das eine Urheberrechtsverletzung wäre. Der Mod ist aber so gebaut, dass jede
beliebige eigene Audiodatei eingebunden werden kann, für die ihr die Rechte habt bzw.
die ihr privat und legal nutzen dürft:

1. Datei umbenennen in `mourning_song.ogg`.
2. Ablegen unter:
   ```
   src/main/resources/assets/villagermourning/sounds/mourning_song.ogg
   ```
   (laut Screenshot bereits erledigt.)
3. Fertig – `sounds.json` referenziert diese Datei bereits.

Ohne diese Datei kompiliert der Mod trotzdem, es gibt beim Abspielen dann nur einen
"missing sound"-Log-Eintrag (kein Crash).

## Jar bauen (Schritt für Schritt)

Voraussetzungen: **Java Development Kit (JDK) 21** installiert
(z.B. von https://adoptium.net – "Temurin 21"), Internetzugang beim ersten Build.

### Windows

1. Ordner `villagermourning-final` irgendwohin entpacken (z.B. `C:\Users\Main\Downloads\villagermourning-final`).
2. Eure `mourning_song.ogg` an den oben genannten Pfad legen, falls noch nicht geschehen.
3. In diesem Ordner mit Rechtsklick → "In Terminal öffnen" (oder `cmd`/PowerShell dort
   öffnen).
4. Eingeben:
   ```
   gradlew.bat build
   ```
5. Der erste Build dauert einige Minuten (lädt Minecraft, Mappings, Fabric API
   herunter). Am Ende steht `BUILD SUCCESSFUL`.
6. Die fertige Datei liegt unter:
   ```
   build\libs\villagermourning-1.0.0.jar
   ```

### macOS / Linux

```bash
cd villagermourning-final
chmod +x gradlew
./gradlew build
```

## Mod installieren

1. `villagermourning-1.0.0.jar` (NICHT die `-sources.jar` oder `-dev.jar`) in den
   `mods`-Ordner eurer Fabric-Instanz kopieren
   (z.B. `%appdata%\.minecraft\mods` unter Windows).
2. Zusätzlich **Fabric API** für Minecraft 1.21.4 installieren (von
   https://modrinth.com/mod/fabric-api oder CurseForge) – ohne die läuft der Mod nicht.
3. Fabric Loader für 1.21.4 im Minecraft Launcher als Profil auswählen und starten.

## Einstellungen anpassen

Alle wichtigen Werte stehen als Konstanten ganz oben in
`src/main/java/com/villagermourning/MourningManager.java`:

- `SEARCH_RADIUS` – wie weit nach Villagern um den toten Golem gesucht wird
- `ARRIVAL_DISTANCE` – ab wann ein Villager als "angekommen" gilt
- `WALK_SPEED` – wie schnell die Villager laufen
- `MAX_WAIT_TICKS` – nach wie vielen Ticks der Sound auch ohne Ankunft aller startet
- `EVENT_LIFETIME_AFTER_SONG` – wie lange das Event nach Songstart noch als aktiv gilt

Der Grabstein selbst (Blocktyp, Schildtext) steht in der Methode `placeGravestone(...)`
in derselben Datei – z.B. `Blocks.COBBLESTONE` gegen einen anderen Block austauschen.

## Falls die Minecraft-Version veraltet ist

Minecraft-Versionen (und mittlerweile auch das Versionsschema selbst) ändern sich
laufend. Falls `1.21.4` nicht mehr die von euch gewünschte Version ist:

1. Geht auf https://fabricmc.net/develop/ (offizieller Fabric-Versionsgenerator).
2. Wählt dort die gewünschte Minecraft-Version aus.
3. Übertragt die angezeigten Werte für `minecraft_version`, `yarn_mappings`,
   `loader_version` und `fabric_api_version` in die Datei `gradle.properties`.
4. Nochmal `gradlew.bat build` (bzw. `./gradlew build`) ausführen.

Bei sehr neuen Versionen kann es sein, dass sich interne Klassennamen geändert haben
und der Code leicht angepasst werden muss (das zeigt euch dann ein roter
Compiler-Fehler mit Zeilennummer an).
