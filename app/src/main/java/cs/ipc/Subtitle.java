package cs.ipc;


public class Subtitle extends ItemBase {
    public String fontname;
    public int fontsize;
    public boolean bold;
    public boolean italic;
    public boolean underline;
    public boolean transparent;    //背景完全透明 优先级比 backcolor 高
    public int opacity; // 背景不透明度, 0 - 100,  0 完全透明
    public Direction direction;    // 012 三个值, 取值2时,文字居中显示
    public int speed;
    public String text;
    public String fontcolor;
    public String backcolor;

    public Subtitle() {
        type = "subtitle";
        fontname = "宋体";
        fontsize = 30;
        bold = false;
        italic = false;
        underline = false;
        transparent = false;    //优先级比 backcolor 高
        opacity = 100;
        direction = Direction.ToLeft;    // r2l  l2r  static; 012 三个值; 取值2时;文字居中显示
        speed = 7;
        text = "请在 字幕内容 区输入字幕( 双击放大)";
        fontcolor = "#FFFFFF";
        backcolor = "#3CA7C7";
    }

    public String getFontname() {
        return fontname;
    }

    public void setFontname(String fontname) {
        this.fontname = fontname;
    }

    public int getFontsize() {
        return fontsize;
    }

    public void setFontsize(int fontsize) {
        this.fontsize = fontsize;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFontcolor() {
        return fontcolor;
    }

    public void setFontcolor(String fontcolor) {
        this.fontcolor = fontcolor;
    }

    public String getBackcolor() {
        return backcolor;
    }

    public void setBackcolor(String backcolor) {
        this.backcolor = backcolor;
    }


}

