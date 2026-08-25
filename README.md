# skript-tebex

A [skript-minestom](https://github.com/skript-minestom/skript-minestom) addon that connects a
Minestom server to a [Tebex](https://tebex.io) store. It polls Tebex's command queue and delivers
paid purchases exactly once, exposes the store listing for a `/buy` GUI, and gives Skript full
access to both APIs through events, expressions, effects and conditions.

## Requirements

- skript-minestom `1.0.0-alpha.38` or newer
- Java 25
- A Tebex game-server store, its secret key, and optionally its Headless public token

## Installation

1. `./gradlew shadowJar` — produces `build/libs/skript-tebex-1.0.0-all.jar`.
2. Drop that jar into the server's `Skript/addons/` folder.
3. Start the server. The addon creates `<server root>/skript-tebex/config.yml` and stays
   disconnected until a secret key is set.
4. Run `/tebex secret <key>`, then put your `.sk` scripts in `scripts/` as usual.

## Credentials

Two separate Tebex credentials, each unlocking a different half of the addon:

- **Secret key** — from [creator.tebex.io/game-servers](https://creator.tebex.io/game-servers).
  Authenticates the Plugin API: purchase delivery, player lookups, bans, coupons, gift cards and
  checkout links. Without it the addon does nothing but wait.
- **Public token** — from your store's Headless API settings. Enables the store listing, `/buy`
  and basket links. Without it, delivery still works fine; only the storefront half is disabled.

Neither is ever logged or shown in full. `/tebex report` redacts the secret key.

## config.yml

Created at `<server root>/skript-tebex/config.yml`. Writes are atomic, so a crash mid-save cannot
corrupt it.

| Key | Default | Meaning |
| --- | --- | --- |
| `secret-key` | `""` | Plugin API secret key. Empty means disconnected. |
| `public-token` | `""` | Headless API public token. Empty means no listing or `/buy`. |
| `buy-gui.enabled` | `true` | Whether `/buy` is registered. |
| `buy-gui.title` | `<gold>Store` | GUI title, as MiniMessage. |
| `buy-gui.rows` | `6` | GUI rows; the last row holds paging controls. |
| `verbose` | `false` | Extra logging, toggled live with `/tebex debug`. |

## Example

```
on tebex connect:
    broadcast "<green>Connected to %tebex store name%!"

on tebex purchase:
    broadcast "<gold>%event-player% just bought %name of package%!"

on tebex command:
    if command contains "op":
        cancel event
        mark the tebex command as unexecuted

command /store:
    trigger:
        open the tebex store to player
```

## Documentation

- **[Syntax reference](docs/syntax.md)** — every event, expression, condition, effect and command.
- **[Manual verification checklist](docs/manual-verification.md)** — what to check on a live server.
- **[SkriptHub JSON](docs/skript-tebex.json)**: generated documentation, ready for the JSON import
  tool of a docs site such as skripthub.net or smdocs.hapily.me.

## Known limitations

- **Delivery suppression is in memory.** If a command's delete call to Tebex fails and the server
  restarts before the retry succeeds, that command can be delivered twice.
- **Scheduler-dropped tasks.** A delayed command's id stays in the in-flight set until its task
  runs. If the scheduler drops it (this happens at shutdown), later checks skip that command until
  the server restarts.
- **Unconfirmed Headless path.** Add-package-to-basket is called as `POST /baskets/{ident}/packages`.
  Tebex's own OpenAPI renders this path inconsistently, so confirm it against a live store — see
  the [verification checklist](docs/manual-verification.md). A wrong path makes basket checkout
  links silently empty.
- **Basket return URLs** are derived from the store domain the Plugin API reports, falling back to
  `https://tebex.io/`.

## Development

```
./gradlew build      # compile + run the test suite (111 tests)
./gradlew shadowJar  # build the addon jar
```

The syntax documentation lives in the Skript doc annotations on each element. After changing an
annotation or a pattern, regenerate the JSON:

```
python tools/generate_skripthub_docs.py
```

Tests cover the API clients, the delivery queue's exactly-once semantics, the rate-limit budget,
config handling and the GUI helpers. The Skript and Minestom layers need a running server, which
is what the verification checklist is for.
