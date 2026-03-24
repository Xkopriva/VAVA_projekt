# Vava 

## 🚀 Workflow ku projektu

Aby sme udržali náš kód stabilný a funkčný, hlavná vetva (main) je chránená. To znamená, že do nej nemôžeš priamo „pushnúť“ zmeny

## Návod na pushovanie:

### 1. Vytvor si vlastnú branch

Nikdy nepracuj priamo v `main`. Vytvor si novú branch, ktorú si pomenuj podľa toho, na čom pracuješ alebo ako chceš (napr. `feat-prihlasovanie` alebo `fix-bug-tlacidlo`)

```bash
# Najprv sa uisti, že máš aktuálny main
git checkout main
git pull origin main

# Vytvor novú vetvu a prepni sa do nej
git checkout -b nazov-tvojej-vetvy
```

### 2. Commitni svoje zmeny

Keď máš kód napísaný a funkčný, ulož zmeny

```bash
git add .
git commit -m "feat: krátky popis toho, čo si pridal"
```

### 3. Pošli zmeny na GitHub (Push)

Teraz musíš svoju branch nahrať na GitHub. Priamo do `main` ťa to nepustí (je chránený), musíš pushnúť svoju branch

```bash
git push origin nazov-tvojej-vetvy
```

### 4. Vytvor Pull Request (PR) a požiadaj o review

Toto je krok, kde sa tvoj kód skontroluje, aby sa nepokazil hlavný program

1. Otvor si náš repozitár na GitHube
2. Hneď hore uvidíš tlačidlo **"Compare & pull request"** (ak nie, choď do záložky "Pull requests" a klikni na "New pull request")
3. Skontroluj, či posielaš zmeny z `tvojej-vetvy` do `main`
4. **Dôležité:** V pravom stĺpci v časti **Reviewers** klikni na ozubené koliesko (settings) a vyber si niekoho, ktorý ti má kód skontrolovať
5. Klikni na **Create pull request**

### 5. Čakanie na schválenie (Merge)

Tvoj kód sa do `main` nedostane, kým ho reviewer neschváli
- Ak ti reviewer napíše komentáre, oprav ich u seba v PC, znova sprav `commit` a `push` (PR sa na GitHube aktualizuje sám)
- Keď reviewer kód schváli, Pull Request sa môže "mergnúť" (zlúčiť) do `main` a stane sa súčasťou projektu