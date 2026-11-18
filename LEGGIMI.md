# Io cucino! — versione 0.2

App Android (Kotlin + Jetpack Compose) per decidere cosa cucinare, pianificare il menù della settimana,
fare la lista della spesa e tenere traccia di cosa c'è in casa.

## Come ottenere l'APK (GitHub Actions)

1. Metti **tutto il contenuto di questa cartella** nella cartella del tuo repository (quella gestita
   da GitHub Desktop, sovrascrivendo i file della versione 0.1; comprese le cartelle nascoste `.github`
   e `gradle`, e il file `app/debug.keystore`). Poi in GitHub Desktop: **Commit to main** e **Push origin**.
2. Su GitHub apri **Actions → Build APK → Run workflow** (parte da solo anche a ogni caricamento).
3. A fine compilazione scarica dall'esecuzione l'artefatto **Io-cucino-apk** e installa `app-debug.apk`.

La firma dell'app è fissa (`app/debug.keystore`): gli aggiornamenti successivi si installano
**sopra** la versione già presente, senza perdere dispensa, spesa e menù.

## Ricette online

Le 50 ricette iniziali sono già dentro l'app (funziona anche senza internet). Per averne di nuove
senza aggiornare l'app:

1. Crea su GitHub un repository **pubblico** chiamato `io-cucino-ricette` (account `steratto-ux`) e
   carica nella sua radice il file `ricette-online/recipes.json` (lasciandolo con il nome `recipes.json`).
2. Fatto. L'app è già predisposta con questo indirizzo:
   `https://raw.githubusercontent.com/steratto-ux/io-cucino-ricette/main/recipes.json`
   (si vede e si può cambiare in **ingranaggio → Impostazioni → Ricette online**). Il tasto
   **Aggiorna ora** scarica subito; altrimenti l'app controlla da sola al massimo ogni 12 ore.

## Foto delle ricette

Ogni ricetta mostra una foto, scelta in quest'ordine:

1. **la tua foto**: nella ricetta tocca il tasto con la fotocamera → *Scegli dalla galleria*. Resta
   sul telefono e ha sempre la precedenza;
2. **la foto indicata nel catalogo** (campo `"image"` della ricetta, un indirizzo web);
3. **la foto trovata da sola su Wikipedia/Wikimedia Commons**, solo se c'è una pagina pertinente al
   piatto. Viene cercata quando sei online e poi resta in memoria.

Se non c'è nessuna foto si vede il colore del tipo di piatto con la sua emoji.

Per aggiungere ricette basta aggiungerle all'elenco `recipes` del file; una ricetta con lo stesso `id`
sostituisce quella già presente. Formato di una ricetta:

```json
{
  "id": "frittata-zucchine",
  "title": "Frittata di zucchine",
  "course": "secondo",          // primo | secondo | unico | contorno
  "difficulty": "semplice",     // semplice | media | impegnativa
  "minutes": 25,
  "servings": 4,
  "emoji": "🍳",
  "image": "",                  // facoltativo: indirizzo di una foto
  "tags": ["vegetariano", "veloce"],
  "ingredients": [
    { "name": "uova", "qty": 6, "unit": "pz", "aisle": "latticini" },
    { "name": "sale", "unit": "", "aisle": "condimenti", "staple": true }
  ],
  "steps": ["Prima fase...", "Seconda fase..."]
}
```

`aisle` (reparto): verdura, carne, pesce, latticini, pasta, dispensa, surgelati, condimenti.
`staple: true` = ingrediente che si ha sempre in casa (sale, olio, pepe, acqua): non va in lista della spesa.
Usa sempre lo stesso nome per lo stesso ingrediente (es. "uova", "passata di pomodoro"):
è così che l'app riconosce cosa hai già in dispensa.
