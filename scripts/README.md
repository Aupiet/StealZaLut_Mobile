# Scripts & Outils Internes

Ce répertoire contient les outils utilitaires de maintenance et de vérification statique du projet StealZaLut Mobile.

## `check_guard_violations.py`

Script d'audit autonome vérifiant le respect strict du Design System dans `ui/screens` et `ui/components` :
- Absence de couleurs brutes hardcodées (`Color(0x...)`)
- Absence de dimensions brutes (`.dp`)
- Absence de typographies brutes (`.sp`, `fontSize`)
- Absence de chaînes en dur (`Text("...")`, `contentDescription`, `userMessage`)
- Absence d'icônes génériques hors du catalogue `SzlIconSet`
- Absence de déclarations directes de polices (`FontFamily(...)`)

### Utilisation :
```bash
python scripts/check_guard_violations.py
```
*(Correspondance directe avec le test unitaire JVM `DesignSystemGuardTest.kt`).*
