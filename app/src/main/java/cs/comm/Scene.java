package cs.comm;

import java.util.ArrayList;
import java.util.List;

import cs.ipc.AdItemType;
import cs.ipc.Picture;
import cs.ipc.Subtitle;
import cs.ipc.Video;


public class Scene {
    public boolean drawing = false;
    public String dir;   //场景所在的文件夹名称

    public String timing; // 03:44
    public String name;
    public List<AdItemType> layers = new ArrayList<AdItemType>();    //表示 Video, Subtitle, Picture 的层次

    public List<Video> video = new ArrayList<Video>();
    public List<Subtitle> subtitle = new ArrayList<Subtitle>();
    public List<Picture> picture = new ArrayList<Picture>();

    //public List<String> bg = new ArrayList<String>(); 	//场景背景图, 默认全屏, 最底层
    //public int bgIntval; // 背景图 轮播间隔
}
 