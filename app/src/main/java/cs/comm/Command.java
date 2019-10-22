package cs.comm;

public enum Command {
    // pc -> box
    server_ip, //广播pc ip
    server_close,  //pc 关闭
    get_screen,   //获取box 屏幕截图
    set_time,  //设置时间
    send_ad,  //发送ad 文件
    set_id,   //设置 box id , 1 -255 
    set_name,
    prev_scene,
    next_scene,
    set_volume,
    get_volume,
    reboot,
    install_apk,
    get_time,

    // box -> pc
    send_ad_ok,
    heartbeat,
    connect,
    screen_image,
    curr_volume,
    curr_time,
    clear,  // delete all scene
    set_wan_ip,
}