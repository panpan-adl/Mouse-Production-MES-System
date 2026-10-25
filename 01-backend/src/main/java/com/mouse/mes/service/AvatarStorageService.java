package com.mouse.mes.service;

import com.mouse.mes.mapper.UserMapper;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {
    private final UserMapper users;
    private final Path directory;
    public AvatarStorageService(UserMapper users,@Value("${upload.path:uploads/avatars}") String directory) {
        this.users=users;this.directory=Paths.get(directory).toAbsolutePath().normalize();
    }
    public String upload(long userId, MultipartFile file) throws IOException {
        if(file.isEmpty()||file.getSize()>2L*1024*1024)throw new IllegalArgumentException("请选择不超过2MB的JPG或PNG图片");
        BufferedImage image;
        try(InputStream source=file.getInputStream(); ImageInputStream stream=ImageIO.createImageInputStream(source)) {
            if(stream==null)throw new IllegalArgumentException("无法读取图片");
            Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new IllegalArgumentException("文件不是有效的JPG或PNG图片");
            ImageReader reader=readers.next();
            try {
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("jpeg","jpg","png").contains(format))throw new IllegalArgumentException("只支持JPG或PNG图片");
                reader.setInput(stream);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||width>4096||height>4096)throw new IllegalArgumentException("头像尺寸不能超过4096×4096像素");
                image=reader.read(0);
            } finally {reader.dispose();}
        }
        if(image==null)throw new IllegalArgumentException("图片内容损坏，请重新选择");
        BufferedImage avatar=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=avatar.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int size=Math.min(image.getWidth(),image.getHeight()),x=(image.getWidth()-size)/2,y=(image.getHeight()-size)/2;
            graphics.drawImage(image,0,0,256,256,x,y,x+size,y+size,null);
        } finally {graphics.dispose();}
        Files.createDirectories(directory);
        Path target=directory.resolve(UUID.randomUUID()+".png");
        try {
            if(!ImageIO.write(avatar,"png",target.toFile()))throw new IOException("PNG writer unavailable");
            String url="/api/users/avatar/"+target.getFileName();
            if(users.updateAvatar(userId,url)!=1)throw new IllegalArgumentException("账号不存在，无法保存头像");
            return url;
        } catch(IOException|RuntimeException error) {Files.deleteIfExists(target);throw error;}
    }
}
