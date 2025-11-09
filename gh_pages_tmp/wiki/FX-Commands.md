# FX Test Commands

Die folgenden Befehle dienen zum schnellen Testen und Vorführen neuer Effekt-Visuals. Nur Admin/OP (Permission `minecraftsurvivors.admin`).

## /fx testgenkidama
Löst einen Meteor/Genkidama-Effekt an deinem Blickziel (bis ca. 25 Blöcke) aus.

Verwendet Config-Werte:
- `visuals.genkidama.height` (Default 10.0)
- `visuals.genkidama.radius` (Default 3.5)
- `visuals.genkidama.damage` (Default 0.0; nur Visual, kein Schaden)
- `visuals.meteor.damage-enabled` (Default false; falls true, wirkt Schaden in Radius)

## /fx meteor <height> <speed> <radius> <damage>
Erzeugt einen Meteor mit Parametern:
- `height`: Start-Höhe (z. B. 10.0)
- `speed`: Fallgeschwindigkeit in Blöcken pro Tick (z. B. 0.5)
- `radius`: Wirkungsradius bei Aufschlag (z. B. 3.0)
- `damage`: Schaden auf Treffer (benötigt `visuals.meteor.damage-enabled: true`)

## Fancy Visuals (Konfiguration)
Allgemeiner Schalter:
- `visuals.fancy-enabled` (Default true)

FrostNova-spezifisch:
- `visuals.frostnova.fancy` (Default true)
- `visuals.frostnova.ring-points` (Default 36)
- `visuals.frostnova.shards` (Default 12)

Diese Flags steuern zusätzliche Ringe/Partikel bei FrostNova und können zur Performance-Optimierung reduziert oder deaktiviert werden.

