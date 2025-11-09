# GlyphCatalog

Package: `org.bysenom.minecraftSurvivors.glyph`

Registry für Glyphen: je Ability >= 3 Vorschläge.
Glyph-Key Schema: abilityKey + ":" + glyphId, z.B. "ab_lightning:genkidama".

## Public Fields

- `String key`
- `String abilityKey`
- `String name`
- `Material icon`
- `String desc`

## Public Methods

- `forAbility(String abilityKey)`
- `get(String glyphKey)`
