package cs.comm;

public class Cmd {
    public static int broadcast_port = 41000;
    public static int cmd_port = 41001;
    public static int file_port = 41002;

    public static String create(Command cmd, String msg) {
        return "/" + cmd.name() + "/:" + msg;
    }
}
