package cs.ipc;

import java.util.ArrayList;
import java.util.List;


public class Picture extends ItemBase {
    public int intval = -1;
    public List<String> filelist = new ArrayList<String>();

    public Picture() {
        type = "picture";
    }


}