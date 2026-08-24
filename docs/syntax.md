# skript-tebex syntax reference

Every event, expression, condition, effect and command skript-tebex registers.
See the [README](../README.md) for setup.

## Events

| Event | Event values |
| --- | --- |
| `on tebex connect` | `store` (the connected store; see the `tebex store` expression below for the practical way to read it) |
| `on tebex disconnect` | none |
| `on tebex queue check` | `due players` (number of players with commands due) |
| `on tebex queue error` | `reason` (string) |
| `on tebex purchase` | `event-player` (nullable), `tebex player`, `package`, `payment id`, `tebex commands` |
| `on tebex command` (cancellable) | `event-player`, `command`, `tebex command`, `package`, `payment id` |

`on tebex connect` fires once skript-tebex has authenticated and cached store information.
`on tebex queue check` fires after every successful poll of the command queue, whether or not
anything was due. `on tebex purchase` fires once per payment, the moment the first of that
payment's commands is actually delivered - so immediately after that command's `on tebex command`
event, and before any remaining commands of the same payment - and never a second time for that
payment. A command that is waiting on a slot condition, that a script left redeliverable with
`mark the tebex command as unexecuted`, that failed to dispatch, or that Tebex re-offers because
the delete call failed, does not fire the purchase event again when it eventually lands.
`on tebex command` fires once per command, and cancelling it stops the command from running but
still reports it to Tebex as executed **unless** the script also runs `mark the tebex
command as unexecuted`, in which case Tebex redelivers it on a later check.

## Expressions

| Expression | Returns |
| --- | --- |
| `[the] tebex store (name\|id\|domain\|currency)` | text/number, the cached store's account info |
| `[the] tebex categories` | list of categories |
| `[the] tebex packages` | list of packages |
| `packages of %tebexcategories%` | list of packages in a category |
| `[the] seconds until [the] next tebex check` | number |
| `[the] tebex due players` | list of players with commands due |
| `[the] pending tebex commands of %player%` | list of that player's deferred commands |
| `[tebex] name of %object%` | package / category / tebex player / sale / goal |
| `[tebex] price of %tebexpackages%` | package's total price |
| `[tebex] id of %object%` | package / category / coupon / gift card / ban / goal |
| `[tebex] code of %object%` | coupon's code, or a gift card's card number |
| `[tebex] description of %object%` | package / category / goal |

## Conditions

| Condition |
| --- |
| `tebex is [not] connected` |
| `%player% has [any] pending tebex commands` / `%player% does(n't\|not) have [any] pending tebex commands` |

## Effects

| Effect | Async? |
| --- | --- |
| `set [the] tebex secret key to %string%` | no |
| `create [a] tebex checkout link for %tebexpackage/number% for %player/string% and store it in %~object%` | yes |
| `create [a] tebex basket link for %tebexpackages% for %player% and store it in %~object%` | yes |
| `create [a] tebex coupon [named %string%] with %number% [percent] discount [expiring in %timespan%] and store it in %~object%` | yes |
| `delete [the] tebex coupon %tebexcoupon%` | yes |
| `create [a] tebex giftcard worth %number% [with note %string%] and store it in %~object%` | yes |
| `void [the] tebex giftcard %tebexgiftcard%` | yes |
| `fetch [the] tebex lookup of %player/string% and store it in %~object%` | yes |
| `fetch [the] tebex purchases of %player/string% and store them in %~objects%` | yes |
| `fetch [the] [last %number%] tebex payments and store them in %~objects%` | yes |
| `fetch [the] tebex community goals and store them in %~objects%` | yes |
| `fetch [the] tebex sales and store them in %~objects%` | yes |
| `force [a] tebex queue check` (a no-op inside the current `next_check` interval) | yes |
| `refresh [the] tebex listing` | yes |
| `tebex ban %player/string% [for %string%] [with ip %string%]` | yes |
| `mark [the] [event-]tebex command as unexecuted` (only inside `on tebex command`) | no |
| `open [the] tebex store to %players%` / `open [the] tebex category %tebexcategory% to %players%` | no |

> The four `fetch ... and store them in %~objects%` effects above (payments, purchases, community
> goals, sales) require a **list variable** as the target, e.g. `{_payments::*}`, `{_goals::*}`.
> A plain scalar variable (`{_goals}`) is rejected at parse time with an error pointing you at the
> list form — this is enforced, not just documented, so a script using the wrong form will fail to
> load rather than silently keep only one result.

## Commands

`/tebex <subcommand>`, checked against LuckPerms permission `tebex.<subcommand>` for every sender
except console (which always passes):

| Subcommand | Permission | Does |
| --- | --- | --- |
| `help` | `tebex.help` | Lists the subcommands. |
| `secret <key>` | `tebex.secret` | Saves the secret key and reconnects. |
| `info` | `tebex.info` | Shows store name, domain, server name, currency, next check. |
| `forcecheck` | `tebex.forcecheck` | Forces an immediate queue check, unless one has already run inside the current `next_check` interval. |
| `refresh` | `tebex.refresh` | Refreshes the cached category/package listing from the Headless API. |
| `lookup <player>` | `tebex.lookup` | Looks up a player's Tebex purchase history. |
| `ban <player> [reason]` | `tebex.ban` | Creates a Tebex ban. |
| `sendlink <player> <packageId>` | `tebex.sendlink` | Sends an online player a clickable checkout link. |
| `report <message>` | `tebex.report` | Writes a diagnostic report to `skript-tebex/reports/`, with the secret key redacted. |
| `debug` | `tebex.debug` | Toggles verbose logging. |

`/buy` opens the store GUI to the player who ran it. It has no permission node of its own; it is
gated only by `buy-gui.enabled` in `config.yml` (registered at startup only when that is true).
Clicking a package in the GUI creates a basket and returns a checkout link; because that costs two
Headless API requests, each player has a short cooldown between checkout clicks.

