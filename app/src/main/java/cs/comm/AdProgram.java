package cs.comm;

import java.util.ArrayList;
import java.util.List;


//生成的 json 与图片,视频文件打包成一个zip
public class AdProgram {
    public int ver; //版本,兼容用
    public int sceneIntval; // scene轮播间隔
    public int backImageIntval;  //底图轮播间隔
    public List<String> backImage = new ArrayList<String>(); //背景图
    public List<Scene> scene = new ArrayList<Scene>();

}
 