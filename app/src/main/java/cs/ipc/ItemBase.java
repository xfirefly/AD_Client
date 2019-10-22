package cs.ipc;


public class ItemBase {
    //保存按 1920*1080 计算的屏幕坐标

    public int x;
    public int y;
    public int w;
    public int h;

    public float fx;
    public float fy;
    public float fx1;
    public float fy1;

    public String type;

    public String ToString() {
        return "type: " + type + " " + x + " " + y + " " + w + " " + h;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public float getFx() {
        return fx;
    }

    public void setFx(float fx) {
        this.fx = fx;
    }

    public float getFy() {
        return fy;
    }

    public void setFy(float fy) {
        this.fy = fy;
    }

    public float getFx1() {
        return fx1;
    }

    public void setFx1(float fx1) {
        this.fx1 = fx1;
    }

    public float getFy1() {
        return fy1;
    }

    public void setFy1(float fy1) {
        this.fy1 = fy1;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


}
