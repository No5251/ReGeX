package com.phikal.regex.Games.Match;

import android.app.Activity;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.phikal.regex.Activities.GameActivity;
import com.phikal.regex.Games.TaskGenerationException;
import com.phikal.regex.R;
import com.phikal.regex.Utils.Task;
import com.phikal.regex.Utils.Word;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Get tasks and contribute to REDB

public class REDBGenerator extends RandomGenerator {

    public static final String stdAddr = "192.168.2.100";

    private final REDB conn;
    private final String errorMsg;
    private final String ipaddr;

    public REDBGenerator(Activity activity) {
        errorMsg = activity.getString(R.string.redb_error);
        ipaddr = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(GameActivity.REDB_SERVER, stdAddr);
        conn = new REDB();
    }

    @Override
    public int calcMax(Task t, int lvl) {
        return -1;
    }

    @Override
    public Task genTask(int lvl) throws TaskGenerationException {
        try {
            new Socket(ipaddr, 25921);
            return conn.requestTask(lvl);
        } catch (IOException ioe) {
            throw new TaskGenerationException(errorMsg);
        }
    }

    private class REDB extends AsyncTask<Void, Void, Void> {
        private final char
                INFO = '@', ERROR = '!', INPUT = ':',
                MATCH = '+', DMATCH = '-', ANSWR = '>';

        private final BlockingQueue<String> input = new LinkedBlockingQueue<>(1);
        private final BlockingQueue<Character> notifier = new LinkedBlockingQueue<>(1);
        private final BlockingQueue<Word> toMatch = new LinkedBlockingQueue<>(),
                notMatch = new LinkedBlockingQueue<>();
        private boolean running = false;

        @Override
        protected Void doInBackground(Void[] params) {
            running = true;
            input.poll();
            notifier.poll();
            try {
                // seting up network IO...
                Socket conn = new Socket(ipaddr, 25921);
                PrintWriter writer = new PrintWriter(conn.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder line = new StringBuilder(); // obviously too much
                boolean newline = true; // has experienced newline and is to expect new command?
                char state = 0;
                for (int c; (c = reader.read()) != -1;) { // loop until stream ends, probably should buffer
                    if (newline) { // parsing new command
                        char check = (char) reader.read();
                        if (check != ' ')  // assert correct protocol form
                            break; // ... fail otherwise
                        state = (char) c;
                        if (c == INPUT || c == ANSWR) { // ... except if expecting input
                            notifier.add(state); // inform what kind of input is expected
                            writer.println(input.take()); // recive input when available
                        } else newline = false; // otherwise start parsing
                    } else { // either process since newline or add to buffer
                        if (c == '\n') {
                            switch (state) { // following considered self-explanatory
                                case MATCH:
                                    toMatch.add(new Word(line.toString()));
                                    break;
                                case DMATCH:
                                    notMatch.add(new Word(line.toString()));
                                    break;
                            } // NOTE: `toMatch` and `notMatch` are used to create new task later on
                            line = new StringBuilder();
                            newline = true; // re-expect new command
                        } else line.append((char) c);
                    }
                }
                conn.close();
            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
            }
            running = false;
            return null;
        }

        public Task requestTask(int lvl) throws TaskGenerationException {
            if (!running) // start thread if isn't running yet (will also restart thread is quit)
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            try {
                if (notifier.peek() != null && notifier.peek() == ANSWR)
                    input.add(""); // force any answer, to get new task (will delay)
                if (notifier.take() != INPUT)
                    throw new TaskGenerationException(errorMsg);
                input.add(String.valueOf(lvl));

                if (notifier.take() != ANSWR)
                    throw new TaskGenerationException(errorMsg);
                notifier.add(ANSWR); // pseudo peek: it is known that no new element will be added

                if (toMatch.size() == 0 && toMatch.size() == 0)
                    return requestTask(lvl / 2);

                List<Word> tma = new ArrayList<>(toMatch.size()),
                        nma = new ArrayList<>(notMatch.size());
                toMatch.drainTo(tma);
                notMatch.drainTo(nma);
                return new Task(tma, nma, (t_, s) -> submitSolution(s));
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return null;
        }

        public synchronized void submitSolution(String s) {
            try {
                if (notifier.take() == ANSWR)
                    input.put(s);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
