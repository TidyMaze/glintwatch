package com.codingame.game.engine;

/** Output parsing, DESIGN.md section 5.3: upper-case keyword, integer tokens, then an optional message. */
public final class CommandParser {
    public static final int MESSAGE_MAX = 30;

    public static final class InvalidCommand extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidCommand(String reason) {
            super(reason);
        }
    }

    private CommandParser() {}

    public static Command parse(String line) throws InvalidCommand {
        if (line == null) throw new InvalidCommand("no output");
        String[] t = line.trim().split(" +");
        switch (t[0]) {
            case "PLACE":
                need(t, 4, line);
                return new Command(Command.Kind.PLACE, num(t[1]), num(t[2]), num(t[3]), message(t, 4));
            case "TURN":
                need(t, 3, line);
                return new Command(Command.Kind.TURN, num(t[1]), num(t[2]), -1, message(t, 3));
            case "WAIT":
                return new Command(Command.Kind.WAIT, -1, -1, -1, message(t, 1));
            default:
                throw new InvalidCommand("unknown command: " + cut(line));
        }
    }

    private static void need(String[] t, int count, String line) throws InvalidCommand {
        if (t.length < count) throw new InvalidCommand("missing arguments: " + cut(line));
    }

    private static int num(String s) throws InvalidCommand {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new InvalidCommand("not an integer: " + cut(s));
        }
    }

    private static String message(String[] t, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < t.length; i++) sb.append(i > from ? " " : "").append(t[i]);
        return cut(sb.toString());
    }

    private static String cut(String s) {
        return s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
    }
}
