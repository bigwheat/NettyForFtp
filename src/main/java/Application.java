/**
 * fshows.com
 * Copyright (C) 2020-2021 All Rights Reserved.
 */

import constant.FtpConstant;

/**
 * @author yangj
 * @version Application.java, v 0.1 2021-01-05 2:48 下午 yangj
 */
public class Application {

    public static void main(String[] args) throws InterruptedException {
        FileServer fileServer = new FileServer(FtpConstant.SERVER_PORT);
        fileServer.start();
    }
}
