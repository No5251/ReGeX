package com.phikal.regex.Games.Match;

import com.phikal.regex.Games.Game;
import com.phikal.regex.Utils.Task;
import com.phikal.regex.Utils.Word;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomGenerator implements Game {

    final private static String chars =
            "aeuioxyzbcdfghjklmnpqrstvw0123456789_AEUIOXYYBCDFGHJKLMNPQRSTVW@%&$#~";
    final Random r = new SecureRandom();

    private char[] getRange(int lvl) {
        double a = chars.length() - 5, b = chars.length();
        int i = (int) Math.round(a - b * Math.pow((lvl + Math.pow(b / a, 2)), -0.5)) + 4;
        return chars.substring(0, i).toCharArray();
    }

    protected String tryWord(int diff) {
        String s = "";
        char[] chars = getRange(diff);
        for (int i = ((int) Math.round(1.5 * Math.log(r.nextInt(diff + 1) + 1) + r.nextInt(1)));
             i >= 0; i--)
            s += chars[r.nextInt(chars.length)];
        return s;
    }

    @Override
    public boolean pass(Task t, String sol) {
        for (Word w : t.getRight())
            if (w.matches(sol) == 0) return false;
        for (Word w : t.getWrong())
            if (w.matches(sol) == 0) return false;
        return true;
    }

    private Word genWord(int diff, boolean _) {
        String w;
        do w = tryWord(diff); while (w.isEmpty());
        return new Word(w);
    }

    private List<Word> genWords(int lvl, List compare) {
        List<Word> list = new ArrayList<>();
        if (compare == null) compare = new ArrayList();
        Random r = new Random();
        for (int i = ((int) Math.ceil(Math.log10(1 + r.nextInt(lvl * lvl + 1)))) + r.nextInt(1); i >= 0; i--) {
            Word word;
            do word = genWord(lvl, false);
            while (list.contains(word) || compare.contains(word));
            list.add(word);
        }
        return list;
    }

    @Override
    public Task genTask(int lvl) {
        List<Word> right = genWords(lvl, null);
        return new Task(right, genWords(lvl, right), null);
    }

    @Override
    public int calcMax(Task t, int lvl) {
        int clen_right = 0;
        for (Word s : t.getRight()) clen_right += s.length();
        return (int) Math.floor(clen_right + t.getRight().size() + lvl / 5);
    }
}
