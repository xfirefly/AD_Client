package cs.comm;

import java.util.ArrayList;
import java.util.List;

import cs.ipc.AdItemType;
import cs.ipc.Picture;
import cs.ipc.Subtitle;
import cs.ipc.Video;

public class Scene {
    public boolean drawing = false;
    public String dir;
    public String timing;
    public String name;
    public List<AdItemType> layers = new ArrayList<>();
    public List<Video> video = new ArrayList<>();
    public List<Subtitle> subtitle = new ArrayList<>();
    public List<Picture> picture = new ArrayList<>();
}
