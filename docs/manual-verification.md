# Manual verification checklist

Everything in skript-tebex that can be verified without a server has been: the suite is 111 tests
covering the HTTP clients, the delivery queue's exactly-once semantics, the rate-limit budget, the
config, the diagnostic report and the GUI helpers. The Skript syntax layer, the Minestom adapters
and the live Tebex endpoints cannot be exercised without a running server and a real secret key,
so this checklist is what remains. Nothing below has been run yet.

## Steps

1. Build the addon: `./gradlew shadowJar`. Confirm `build/libs/skript-tebex-1.0.0-all.jar` exists.
2. Copy that jar into the test server's `Skript/addons/` folder, alongside skript-minestom.
3. Start the server. Confirm the console logs skript-tebex loading, and that it warns about the
   missing secret key — a fresh `skript-tebex/config.yml` has an empty `secret-key`.
4. Run `/tebex secret <your game-server secret key>`. Confirm the connection succeeds. If
   `verification.sk` is already loaded, its `on tebex connect` handler prints the store name and
   currency.
5. Put `verification.sk` in `scripts/`, reload it, and run `/tebextest`. Confirm every line prints
   with no console error. The goals and sales lines use list variables (`{_goals::*}`) and should
   report a count.
6. **The single most important check.** Make a real purchase, or use Tebex's test mode. Confirm:
   - `on tebex purchase` and `on tebex command` both fire;
   - the purchased command actually runs in game;
   - the same purchase is **not** delivered again on the next queue check.

   This is also the moment that settles the add-package path. Tebex's own OpenAPI renders the
   Headless add-package endpoint inconsistently across pages, so the addon uses
   `POST /baskets/{ident}/packages`. If that path is wrong, the basket is still created
   successfully and the checkout link still arrives — but with no packages in it. The failure is
   silent, which is why this is the top-priority item.
7. Run `/buy`, click through to a package, and confirm a checkout link arrives in chat and opens
   in a browser to the right package.
8. Run each `/tebex` subcommand: `info`, `lookup <you>`, `forcecheck`, `refresh`, `sendlink <you>
   <packageId>`, `debug`, and `report test`. Confirm each responds sensibly, and open the file
   written under `skript-tebex/reports/` to confirm the secret key appears as
   `<redacted, N chars>` and never in the clear.
9. Confirm the queue's poll interval is being respected: with `verbose: true` in the config, the
   console logs each queue check. The gap between checks should match the `next_check` Tebex
   returns (90 seconds by default), and running `/tebex forcecheck` repeatedly inside one interval
   should not produce extra polls.

## Known limitations to keep in mind while testing

These are documented in the README and are deliberate, not bugs to rediscover:

- Delivery suppression is held in memory. If a command's delete call to Tebex fails and the server
  restarts before a retry succeeds, that command can be delivered a second time.
- If the scheduler drops a delayed task without running it (which happens at shutdown), that
  command's id stays marked in-flight and is skipped on later checks until the next restart.
- Basket return URLs are derived from the store domain the Plugin API reports, falling back to
  `https://tebex.io/`.

## If something fails

Record it as a bug before treating the addon as done. `/tebex report <message>` writes a redacted
diagnostic dump under `skript-tebex/reports/` that is safe to attach to an issue.
