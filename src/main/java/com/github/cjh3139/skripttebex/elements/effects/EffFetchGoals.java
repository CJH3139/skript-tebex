package com.github.cjh3139.skripttebex.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import com.github.cjh3139.skripttebex.SkriptTebex;
import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.CommunityGoal;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletionException;

@Name("Fetch Tebex Community Goals")
@Description(
    "Fetches the community goals of your Tebex store into a list variable. This effect waits for the Tebex API and " +
    "therefore delays the script."
)
@Examples("fetch the tebex community goals and store them in {_goals::*}\nsend \"%name of {_goals::1}%\" to player")
@Since("1.0.0")
public class EffFetchGoals extends AsyncEffect {
    static {
        Skript.registerEffect(EffFetchGoals.class,
            "fetch [the] tebex community goals and store (them|the result) in %~objects%");
    }

    private Variable<?> target;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
        getParser().setHasDelayBefore(Kleenean.TRUE);
        if (!(expressions[0] instanceof Variable<?> variable)) {
            Skript.error("The result of tebex community goals must be stored in a variable.");
            return false;
        }
        if (variable.isSingle()) {
            Skript.error("The result of tebex community goals must be stored in a list variable, e.g. {_goals::*}.");
            return false;
        }
        target = variable;
        return true;
    }

    @Override
    protected void execute(Event event) {
        TebexService service = TebexService.get();
        if (service == null || !service.isConnected() || service.pluginApi() == null) {
            SkriptTebex.getInstance().logger().warn("Cannot fetch tebex community goals: Tebex is not connected.");
            return;
        }

        try {
            List<CommunityGoal> goals = service.pluginApi().communityGoals().join();
            target.change(event, goals.toArray(new CommunityGoal[0]), ChangeMode.SET);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            SkriptTebex.getInstance().logger().warn("Failed to fetch Tebex community goals: {}", cause.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "fetch tebex community goals";
    }
}
