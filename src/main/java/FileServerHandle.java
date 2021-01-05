/**
 * fshows.com
 * Copyright (C) 2020-2021 All Rights Reserved.
 */

import com.alibaba.fastjson.JSONObject;
import constant.FtpConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

/**
 * @author yangj
 * @version FileServerHandle.java, v 0.1 2021-01-05 2:37 下午 yangj
 */
@ChannelHandler.Sharable
public class FileServerHandle extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    /**
     * 请求路由
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request)
            throws Exception {
        System.out.println(request.uri());

        // index页面
        if (FtpConstant.INDEX_URL.equals(request.uri())) {
            indexHtml(ctx,request);
        }

        //下载
        if (request.uri().indexOf(FtpConstant.DOWNLOAD_URL)>-1) {
            downLoadFile(ctx,request);
        }

        //上传
        if (FtpConstant.UPLOAD_URL.equals(request.uri())) {
            upLoadFile(request);
            writeResponseString(ctx, "文件上传成功");
        }

        //文件列表
        if (FtpConstant.FILE_LIST_URL.equals(request.uri())) {
            getFileList(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(copiedBuffer(
                "NettyForFtp", CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


    public  void indexHtml(ChannelHandlerContext ctx,FullHttpRequest request) throws Exception {
        File html = new File(System.getProperty("user.dir") + "/index.html");
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(html));
            String s = null;
            while((s = br.readLine())!=null){
                result.append(System.lineSeparator()+s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        // 创建http响应
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    /**
     * 下载
     *
     * @param ctx
     * @param request
     */
    public void downLoadFile(ChannelHandlerContext ctx,FullHttpRequest request) {

        try {
            String name = request.uri().split("=")[1];
            String path = FtpConstant.FILE_PATH+"/"+java.net.URLDecoder.decode(name,"utf-8");
            File file = new File(path);
            if(!file.exists()) {
                writeResponseString(ctx, "文件" + name + "不存在");
                return;
            }
            //读取文件
            final RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            //定义response对象
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            //设置请求头部
            response.headers().set(CONTENT_LENGTH, fileLength);
            response.headers().set(CONTENT_TYPE, "application/octet-stream; charset=UTF-8");
            response.headers().add(HttpHeaderNames.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(file.getName(), "UTF-8") + "\";");
            ctx.write(response);
            //设置事件通知对象
            ChannelFuture sendFileFuture = ctx
                    .write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                //文件传输完成执行监听器
                @Override
                public void operationComplete(ChannelProgressiveFuture future)
                        throws Exception {
                    // 完成
                }
                //文件传输进度监听器
                @Override
                public void operationProgressed(ChannelProgressiveFuture future,
                                                long progress, long total) throws Exception {
                    // 进度
                }
            });
            //刷新缓冲区数据，文件结束标志符
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传
     *
     * @param request
     * @return
     */
    private void upLoadFile(FullHttpRequest request) {
        try {
            //创建HTTP对象工厂
            HttpDataFactory factory = new DefaultHttpDataFactory(true);
            //使用HTTP POST解码器
            HttpPostRequestDecoder httpDecoder = new HttpPostRequestDecoder(factory, request);
            httpDecoder.setDiscardThreshold(0);
            if (httpDecoder != null) {
                //获取HTTP请求对象
                final HttpContent chunk = (HttpContent) request;
                httpDecoder.offer(chunk);
                if (chunk instanceof LastHttpContent) {
                    //通过迭代器获取HTTP的内容
                    java.util.List<InterfaceHttpData> InterfaceHttpDataList = httpDecoder.getBodyHttpDatas();
                    for (InterfaceHttpData data : InterfaceHttpDataList) {
                        //如果数据类型为文件类型，则保存到fileUploads对象中
                        if (data != null && InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType())) {
                            FileUpload fileUpload = (FileUpload) data;
                            String path = FtpConstant.FILE_PATH + "/" + fileUpload.getFilename();
                            final File file = new File(path);
                            try (FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel(); FileChannel outputChannel = new FileOutputStream(file).getChannel()) {
                                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文件列表
     *
     * @param ctx
     */
    private void getFileList(ChannelHandlerContext ctx) {
        File file = new File(FtpConstant.FILE_PATH);
        File[] fs = file.listFiles();
        StringBuilder msg = new StringBuilder();
        for (File f : fs) {
            if (!f.isDirectory()) {
                msg.append(f.getName() + " \r\n<br />");
            }
        }
        writeResponseString(ctx, msg.toString());
    }


    private final StringBuilder responseContent = new StringBuilder();

    /**
     * 应答
     *
     * @param ctx
     * @param message
     */
    private void writeResponseString(ChannelHandlerContext ctx, String message) {
        responseContent.setLength(0);
        responseContent.append(message);
        ByteBuf buf = copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, buf.readableBytes());
        // 解决跨域问题
        response.headers().set("Access-Control-Allow-Origin", "*");
        ctx.channel().writeAndFlush(response);
    }
}