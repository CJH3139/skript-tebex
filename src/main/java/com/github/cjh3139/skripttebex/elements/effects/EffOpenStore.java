package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.gui.CategoryGui;
import com.github.cjh3139.skripttebex.gui.StoreGui;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Open Tebex Store")
@Description("Opens the Tebex store GUI, or a single category of it, to players.")
@Examples("on command /store:\n\topen the tebex store to player")
@Since("1.0.0")
public class EffOpenStore extends Effect {
    static {
        Skript.registerEffect(EffOpenStore.class,
            "open [the] tebex store to %players%",
            "open [the] tebex category %tebexcategory% to %players%");
    }

    private boolean wholeStore;
    @Nullable
    private Expression<Category> categoryExpression;
    private Expression<Player> playersExpression;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        wholeStore = matchedPattern == 0;
        if (wholeStore) {
            playersExpression = (Expression<Player>) expressions[0];
        } else {
            categoryExpression = (Expression<Category>) expressions[0];
            playersExpression = (Expression<Player>) expressions[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player[] players = playersExpression.getArray(event);
        if (players.length == 0) return;

        if (wholeStore) {
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                for (Player player : players) {
                    StoreGui.open(player);
                }
            });
            return;
        }

        Category category = categoryExpression.getSingle(event);
        if (category == null) return;

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            for (Player player : players) {
                CategoryGui.open(player, category);
            }
        });
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (wholeStore) {
            return "open the tebex store to " + playersExpression.toString(event, debug);
        }

        return "open the tebex category " + categoryExpression.toString(event, debug)
            + " to " + playersExpression.toString(event, debug);
    }
}
