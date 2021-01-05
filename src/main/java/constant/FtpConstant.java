/**
 * fshows.com
 * Copyright (C) 2020-2021 All Rights Reserved.
 */
package constant;

/**
 * @author yangj
 * @version FtpConstant.java, v 0.1 2021-01-05 2:57 下午 yangj
 */
public class FtpConstant {

    /**
     * 服务默认端口
     */
    public static final int SERVER_PORT = 9999;

    /**
     * 文件上传url
     */
    public static final String UPLOAD_URL = "/up";

    /**
     * 文件下载url
     */
    public static final String DOWNLOAD_URL = "/down";

    /**
     * 文件列表URL
     */
    public static final String FILE_LIST_URL = "/list";

    /**
     * index
     */
    public static final String INDEX_URL = "/index.html";

    /**
     * 文件默认存储目录
     */
    public static final String FILE_PATH = System.getProperty("user.dir") + "/file";
}
