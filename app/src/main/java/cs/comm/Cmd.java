package cs.comm;

public class Cmd {
    // socket port
    public static int broadcast_port = 41000;
    public static int cmd_port = 41001;
    public static int file_port = 41002;

    /// <summary>
    /// create buffer send to network, 4 bytes head + message body, head is message length
    /// </summary>
    //    public static byte[] createBuffer(String cmd, String msg)
    //    {
    //        byte[] b = null;
    //		try {
    //			b = ("/" + cmd + "/:" + msg + "\n").getBytes("UTF-8");
    //		} catch (UnsupportedEncodingException e) {
    //			e.printStackTrace();
    //		}
    //        print.e("cmd", "###createBuffer => " + cmd + " " + msg);
    //        return b;
    //    }

    public static String create(Command cmd, String msg) {
        // Send Length
        //byte[] front = BitConverter.GetBytes(back.Length);  // 4 bytes
        //byte[] combined = front.Concat(back).ToArray();

        //print.v("cmd", "createBuffer => " + cmd.name() + " " + msg);
        return "/" + cmd.name() + "/:" + msg;
    }

	/*
	    /// <summary>
	    /// create buffer send to network, 4 bytes head + message body, head is message length
	    /// </summary>
	    public static byte[] createBuffer(string cmd, string msg)
	    {
	        byte[] b = Encoding.ASCII.GetBytes("/" + cmd + "/:" + msg + "\n");
	        // Send Length 
	        //byte[] front = BitConverter.GetBytes(back.Length);  // 4 bytes
	        //byte[] combined = front.Concat(back).ToArray();

	        Console.WriteLine("###createBuffer => " + cmd + " " + msg);
	        return b;
	    }

	    public static byte[] createBuffer2(string cmd, string msg)
	    {
	        byte[] back = Encoding.ASCII.GetBytes("/" + cmd + "/:" + msg + "\n");
	        // Send Length 
	        byte[] front = BitConverter.GetBytes(back.Length);  // 4 bytes
	        byte[] combined = front.Concat(back).ToArray();

	        Console.WriteLine("###createBuffer => " + cmd + " " + msg);
	        return combined;
	    }	*/
}
