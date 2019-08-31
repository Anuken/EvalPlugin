package eval;

import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.plugin.*;

public class EvalPlugin extends Plugin{
    private static final int maxLength = 1024;

    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("eval", "<code...>", "Evaluate server-side code. Admins only.", (args, player) -> {
            if(!player.isAdmin){
                player.sendMessage("[scarlet]You must be an admin to run this command.");
                return;
            }

            Eval.eval(args[0], result -> {
                player.sendMessage(">[accent] '" + result.substring(0, Math.min(result.length(), maxLength)) + "'");
            }, error -> {
                player.sendMessage("[scarlet]Error:[orange] " + error.getClass().getSimpleName() + (error.getMessage() == null ? "" :
                "(" + error.getMessage().substring(0, Math.min(error.getMessage().length(), maxLength)) + ")"));
            });

        });
    }
}
