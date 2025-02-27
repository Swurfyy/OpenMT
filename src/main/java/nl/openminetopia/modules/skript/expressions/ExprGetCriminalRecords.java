package nl.openminetopia.modules.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.prefix.objects.Prefix;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExprGetCriminalRecords extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprGetCriminalRecords.class, String.class, ExpressionType.COMBINED, "[the] (omt|openminetopia) criminalrecords of %player%");
    }

    private Expression<Player> exprPlayer;

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        player = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "Criminalrecords expression with player: " + player.toString(event, debug);
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        Player player = exprPlayer.getSingle(event);
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return new String[0];

        List<Prefix> prefixes = minetopiaPlayer.getPrefixes();
        return prefixes.stream()
                .map(Prefix::getPrefix)
                .toArray(String[]::new);
    }
}
