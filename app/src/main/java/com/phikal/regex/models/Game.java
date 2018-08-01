package com.phikal.regex.models;

import com.phikal.regex.games.Games;
import com.phikal.regex.games.TaskGenerationException;

public interface Game {
    void onProgress(ProgressCallback pc);

    Task nextTask() throws TaskGenerationException;

    Games getGame();

    enum Mode {CHAR_LIMIT, NO_ERASE, TIME_LIMIT}

    interface ProgressCallback {
        void progress(Progress p);
    }
}
