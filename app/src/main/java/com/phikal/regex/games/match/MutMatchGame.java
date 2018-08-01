package com.phikal.regex.games.match;

import android.content.Context;

import com.phikal.regex.R;
import com.phikal.regex.games.Games;
import com.phikal.regex.models.Task;

public class MutMatchGame extends SimpleMatchGame {

    private static final double GAMMA = (1 - Math.sqrt(5)) / 2 + 1;

    private String mutateOn = null;

    public MutMatchGame(Context ctx, MatchProgress p) {
        super(ctx, p);
    }

    @Override
    protected String getName() {
        return ctx.getString(R.string.mutate_match);
    }

    @Override
    public Task nextTask() {
        mutateOn = super.randString();
        return super.nextTask();
    }

    @Override
    String randString() {
        assert mutateOn != null;
        char[] c = mutateOn.toCharArray();

        for (int i = 0; i < c.length; i++) {
            double chance = (progress.getDifficutly() * progress.getDifficutly()) * (1 - GAMMA) + GAMMA;
            if (rnd.nextDouble() > chance)
                c[i] = CHARS[rnd.nextInt(CHARS.length)];
        }

        return String.valueOf(c);
    }

    @Override
    public Games getGame() {
        return Games.MUTATE_MATCH;
    }
}
